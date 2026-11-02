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
 *
 * El celular que ya tenía una finca antes de poder guardar varias conserva su base `app-bovina.db`: se adopta como la
 * primera entrada, sin migrar datos. De ella no se conoce el Usuario hasta abrirla ([resolverIdentidades]).
 */
class CuentasEnElCelular(
    private val context: Context,
    private val fotos: FotosLocales,
    haySesionGuardada: Boolean,
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
        if (entradas.isEmpty() && context.getDatabasePath(ARCHIVO_ANTERIOR).exists()) {
            entradas = listOf(Entrada(ARCHIVO_ANTERIOR, idUsuario = null, correo = null))
            if (haySesionGuardada) activa = ARCHIVO_ANTERIOR
        }
        _entradas = MutableStateFlow(entradas)
        _activa = MutableStateFlow(activa)
        escribir()
    }

    fun base(archivo: String): AppDatabase = synchronized(bases) { bases.getOrPut(archivo) { crearBase(archivo) } }

    /** La base de la cuenta con la sesión abierta, o una vacía si no hay ninguna. */
    fun baseActiva(): AppDatabase = activa.value?.let(::base) ?: vacia

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
        val nueva = Entrada("cuenta-${UUID.randomUUID()}.db", idUsuario, correo)
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
            _activa.value = null
            escribir()
        }
    }

    /**
     * Borra del celular los datos de la cuenta: su entrada, sus fotos y su base. Quien tenga la base abierta (la
     * pantalla de la cuenta que acaba de salir) sigue leyéndola hasta que se cierra, un poco después.
     */
    suspend fun eliminar(archivo: String) {
        val entrada = _entradas.value.firstOrNull { it.archivo == archivo } ?: return
        val fotosDeLaCuenta = runCatching { base(archivo).animalDao().findFotoUris() }.getOrDefault(emptyList())
        synchronized(candado) {
            _entradas.value = _entradas.value - entrada
            if (_activa.value == archivo) _activa.value = null
            escribir()
        }
        fotosDeLaCuenta.forEach { fotos.borrar(it) }
        val base = synchronized(bases) { bases.remove(archivo) }
        alcance.launch {
            delay(GRACIA_DE_CIERRE)
            base?.close()
            context.deleteDatabase(archivo)
        }
    }

    /** Las fotos de las cuentas guardadas distintas de [archivo]: no son basura de la cuenta que limpia las suyas. */
    suspend fun fotosDeLasDemasCuentas(archivo: String?): Set<String> = _entradas.value
        .filter { it.archivo != archivo }
        .flatMap { base(it.archivo).animalDao().findFotoUris() }
        .toSet()

    private fun actualizar(cambio: (List<Entrada>) -> List<Entrada>) {
        synchronized(candado) {
            _entradas.value = cambio(_entradas.value)
            escribir()
        }
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
        preferencias.edit()
            .putString(ENTRADAS, lista.toString())
            .apply { _activa.value?.let { putString(ACTIVA, it) } ?: remove(ACTIVA) }
            .apply()
    }

    private companion object {
        const val PREFERENCIAS = "cuentas_guardadas"
        const val ENTRADAS = "entradas"
        const val ACTIVA = "activa"

        /** La base que usaba la app cuando un celular solo podía tener una cuenta. */
        const val ARCHIVO_ANTERIOR = "app-bovina.db"

        val GRACIA_DE_CIERRE = 5.seconds
    }
}
