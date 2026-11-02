package com.devcris80.prototipo.data.local

import android.content.Context
import com.devcris80.prototipo.data.archivos.FotosLocales
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

/**
 * Las cuentas cuyos datos guarda este celular, cada una en su propia base Room (ADR-0006), y cuál tiene la sesión
 * abierta. Es el único que abre y borra esas bases; el registro de cuáles hay vive en preferencias, fuera de ellas.
 *
 * Una cuenta guardada es una [Entrada]. [activa] es el archivo de la que tiene la sesión abierta, o nulo si
 * ninguna: entonces [baseActiva] es una base vacía en memoria, para que el resto de la app no tenga que distinguir.
 * La cuenta deja de ser la abierta apenas la sesión desaparece, la haya cerrado una persona o la haya perdido la app
 * (el servidor rechazó renovarla): [sesionGuardada] lo dice.
 *
 * El celular que ya tenía una finca antes de poder guardar varias conserva su base `app-bovina.db`: se adopta como la
 * primera entrada, sin migrar datos, la primera vez que arranca esta versión. De ella no se conoce el Usuario hasta
 * abrirla ([resolverIdentidades]).
 */
class CuentasEnElCelular(
    private val context: Context,
    private val fotos: FotosLocales,
    sesionGuardada: StateFlow<Boolean>,
    private val alcance: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
    private val crearBase: (String) -> AppDatabase = { AppDatabase.crear(context, it) },
) {
    /** Una cuenta guardada. [idUsuario] es nulo solo en la base adoptada, hasta que se abre. */
    data class Entrada(val archivo: String, val idUsuario: String?, val correo: String?)

    private val preferencias = context.getSharedPreferences(PREFERENCIAS, Context.MODE_PRIVATE)
    private val candado = Any()
    private val bases = mutableMapOf<String, AppDatabase>()

    private val _entradas: MutableStateFlow<List<Entrada>>
    private val _activa: MutableStateFlow<String?>

    val entradas: StateFlow<List<Entrada>> get() = _entradas.asStateFlow()
    val activa: StateFlow<String?> get() = _activa.asStateFlow()

    val vacia: AppDatabase by lazy { AppDatabase.enMemoria(context) }

    init {
        var entradas = leerEntradas()
        var activa = preferencias.getString(ACTIVA, null)?.takeIf { archivo -> entradas.any { it.archivo == archivo } }
        val anteriorRevisada = preferencias.getBoolean(ANTERIOR_REVISADA, false)
        if (!anteriorRevisada && entradas.isEmpty() && context.getDatabasePath(ARCHIVO_ANTERIOR).exists()) {
            entradas = listOf(Entrada(ARCHIVO_ANTERIOR, idUsuario = null, correo = null))
            if (sesionGuardada.value) activa = ARCHIVO_ANTERIOR
        }
        _entradas = MutableStateFlow(entradas)
        _activa = MutableStateFlow(activa)
        escribir()
        borrarBasesHuerfanas(entradas, anteriorRevisada)
        alcance.launch {
            sesionGuardada.collect { guardada -> if (!guardada) desactivar() }
        }
    }

    fun base(archivo: String): AppDatabase = synchronized(bases) { bases.getOrPut(archivo) { crearBase(archivo) } }

    /** La base de la cuenta con la sesión abierta, o una vacía si no hay ninguna. */
    fun baseActiva(): AppDatabase = activa.value?.let(::base) ?: vacia

    /** Si [archivo] es la base de la cuenta con la sesión abierta. Nulo (la base vacía) nunca lo es. */
    fun esActiva(archivo: String?): Boolean = archivo != null && activa.value == archivo

    /** Al abrirse la base adoptada se conoce su Usuario. Las demás entradas ya lo traen. */
    suspend fun resolverIdentidades() {
        for (entrada in _entradas.value.filter { it.idUsuario == null }) {
            val idUsuario = base(entrada.archivo).usuarioDao().getOnce()?.idUsuario ?: continue
            actualizar { lista -> lista.map { if (it.archivo == entrada.archivo) it.copy(idUsuario = idUsuario) else it } }
        }
    }

    suspend fun entradaDe(idUsuario: String): Entrada? {
        resolverIdentidades()
        return _entradas.value.firstOrNull { it.idUsuario == idUsuario }
    }

    /**
     * La entrada de la cuenta, creada con una base nueva si el celular no la guardaba. No la abre como activa: quien
     * la llama guarda primero lo que descargó y entonces la [activar].
     */
    suspend fun abrirCuenta(idUsuario: String, correo: String?): Entrada {
        val existente = entradaDe(idUsuario)
        if (existente != null) {
            if (correo == null || correo == existente.correo) return existente
            val actualizada = existente.copy(correo = correo)
            actualizar { lista -> lista.map { if (it.archivo == existente.archivo) actualizada else it } }
            return actualizada
        }
        // El nombre no se repite: la base de una cuenta que se quitó puede seguir cerrándose cuando la misma
        // persona vuelve a entrar.
        val nueva = Entrada("$PREFIJO_CUENTA${UUID.randomUUID()}.db", idUsuario, correo)
        actualizar { it + nueva }
        return nueva
    }

    fun activar(archivo: String) {
        synchronized(candado) {
            _activa.value = archivo
            escribir()
        }
    }

    fun desactivar() {
        synchronized(candado) {
            if (_activa.value == null) return
            _activa.value = null
            escribir()
        }
    }

    /**
     * Borra del celular los datos de la cuenta. La entrada sale del registro ya; sus fotos y su base se borran un
     * poco después, cuando la pantalla de la cuenta que acaba de salir dejó de leerlas. Si la app muere antes, el
     * archivo queda sin entrada y el próximo arranque lo borra ([borrarBasesHuerfanas]); las fotos las limpia la
     * sincronización de fotos de cualquier cuenta, como a las demás que nadie usa.
     */
    suspend fun eliminar(archivo: String) {
        val entrada = _entradas.value.firstOrNull { it.archivo == archivo } ?: return
        val fotosDeLaCuenta = runCatching { base(archivo).animalDao().findFotoUris() }.getOrDefault(emptyList())
        synchronized(candado) {
            _entradas.value = _entradas.value - entrada
            if (_activa.value == archivo) _activa.value = null
            escribir()
        }
        val base = synchronized(bases) { bases.remove(archivo) }
        alcance.launch {
            delay(GRACIA_DE_CIERRE)
            fotosDeLaCuenta.forEach { fotos.borrar(it) }
            base?.close()
            context.deleteDatabase(archivo)
        }
    }

    /** Las fotos de las cuentas guardadas distintas de [archivo]: no son basura de la cuenta que limpia las suyas. */
    suspend fun fotosDeLasDemasCuentas(archivo: String?): Set<String> = _entradas.value
        .filter { it.archivo != archivo }
        .flatMap { base(it.archivo).animalDao().findFotoUris() }
        .toSet()

    /** Anota que el celular borró la cuenta abierta porque el servidor ya no le daba finca, para avisarlo al volver a Iniciar sesión. */
    fun marcarCuentaQuitada() {
        preferencias.edit().putBoolean(CUENTA_QUITADA, true).apply()
    }

    /** Dice una sola vez si hay una cuenta quitada por avisar. */
    fun consumirCuentaQuitada(): Boolean {
        val quitada = preferencias.getBoolean(CUENTA_QUITADA, false)
        if (quitada) preferencias.edit().remove(CUENTA_QUITADA).apply()
        return quitada
    }

    private fun actualizar(cambio: (List<Entrada>) -> List<Entrada>) {
        synchronized(candado) {
            _entradas.value = cambio(_entradas.value)
            escribir()
        }
    }

    /** Los archivos de una cuenta que se quitó cuando la app murió antes de borrarlos: ninguna entrada los reclama. */
    private fun borrarBasesHuerfanas(entradas: List<Entrada>, anteriorRevisada: Boolean) {
        val guardadas = entradas.map { it.archivo }.toSet()
        context.databaseList()
            .filter { it.endsWith(".db") && it !in guardadas }
            .filter { it.startsWith(PREFIJO_CUENTA) || (it == ARCHIVO_ANTERIOR && anteriorRevisada) }
            .forEach { context.deleteDatabase(it) }
    }

    private fun leerEntradas(): List<Entrada> {
        val texto = preferencias.getString(ENTRADAS, null) ?: return emptyList()
        val lista = JSONArray(texto)
        return (0 until lista.length()).map { i ->
            val entrada = lista.getJSONObject(i)
            Entrada(
                archivo = entrada.getString("archivo"),
                idUsuario = entrada.optString("idUsuario").takeIf { it.isNotEmpty() },
                correo = entrada.optString("correo").takeIf { it.isNotEmpty() },
            )
        }
    }

    private fun escribir() {
        val lista = JSONArray()
        _entradas.value.forEach {
            lista.put(
                JSONObject()
                    .put("archivo", it.archivo)
                    .put("idUsuario", it.idUsuario.orEmpty())
                    .put("correo", it.correo.orEmpty()),
            )
        }
        val activa = _activa.value
        preferencias.edit().apply {
            putString(ENTRADAS, lista.toString())
            putBoolean(ANTERIOR_REVISADA, true)
            if (activa != null) putString(ACTIVA, activa) else remove(ACTIVA)
        }.apply()
    }

    private companion object {
        const val PREFERENCIAS = "cuentas_guardadas"
        const val ENTRADAS = "entradas"
        const val ACTIVA = "activa"
        const val ANTERIOR_REVISADA = "anterior_revisada"
        const val CUENTA_QUITADA = "cuenta_quitada"
        const val PREFIJO_CUENTA = "cuenta-"

        /** La base que usaba la app cuando un celular solo podía tener una cuenta. */
        const val ARCHIVO_ANTERIOR = "app-bovina.db"

        val GRACIA_DE_CIERRE = 5.seconds
    }
}
