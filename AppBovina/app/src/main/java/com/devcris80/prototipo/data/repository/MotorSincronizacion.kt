package com.devcris80.prototipo.data.repository

import androidx.room.withTransaction
import com.devcris80.prototipo.data.local.AppDatabase
import com.devcris80.prototipo.data.archivos.FotosLocales
import com.devcris80.prototipo.data.local.entity.EstadoSyncEntity
import com.devcris80.prototipo.data.local.entity.SiblingEntity
import com.devcris80.prototipo.data.remote.AlmacenFotos
import com.devcris80.prototipo.data.remote.ServidorSincronizacion
import com.devcris80.prototipo.data.remote.esFaltaDeConexion
import com.devcris80.prototipo.data.remote.salvoCancelacion
import com.devcris80.prototipo.domain.repository.EstadoSincronizacion
import com.devcris80.prototipo.domain.repository.ResultadoSincronizacion
import com.devcris80.prototipo.domain.repository.SincronizacionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.util.UUID

/**
 * El motor de sincronización (DAT-05, CU-20): sube los cambios locales por lotes pequeños, de padres a hijos, y
 * aplica la respuesta de `subir_cambios` fila por fila (ADR-0004). Después descarga por páginas, con
 * `descargar_cambios`, lo que subieron los demás celulares de la finca. Cada lote y cada página se confirman por
 * separado: si se pierde la señal, lo ya aplicado queda, el resto sigue pendiente y la descarga sigue desde el
 * cursor de la última página.
 *
 * Las fotos suben y bajan aparte, con [sincronizarFotos] ([SincronizacionFotos]), después de los datos: nunca los
 * esperan ni cambian su resultado (CU-20 1b).
 *
 * Sus escrituras no llevan EDICION_LOCAL. Una aceptada o un sibling solo tocan la fila si no cambió mientras el
 * lote viajaba: si cambió, la escritura nueva queda pendiente y sube en la próxima sincronización. La descarga
 * nunca pisa una fila con cambios pendientes.
 *
 * Es uno solo en la app: el botón y el trabajo en segundo plano lo comparten, y nunca corren dos sincronizaciones
 * a la vez. [programarTrabajo] deja programado ese trabajo.
 */
class MotorSincronizacion(
    private val servidor: ServidorSincronizacion,
    private val database: AppDatabase,
    almacenFotos: AlmacenFotos,
    fotosLocales: FotosLocales,
    private val programarTrabajo: () -> Unit,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val nuevoIdRechazo: () -> String = { UUID.randomUUID().toString() },
    private val tamanoLote: Int = TAMANO_LOTE,
    private val tamanoPagina: Int = TAMANO_PAGINA,
    private val margenSolapamiento: Long = MARGEN_SOLAPAMIENTO,
    private val alQuitarCuenta: suspend () -> Unit = {},
) : SincronizacionRepository {
    private val tablas = tablasSincronizadas(database)
    private val bandeja = BandejaDeRechazos(database.rechazoDao(), clock, nuevoIdRechazo)
    private val fotos = SincronizacionFotos(almacenFotos, fotosLocales, database, bandeja)
    private val enCurso = Mutex()
    private val fotosEnCurso = Mutex()

    override suspend fun sincronizar(): ResultadoSincronizacion = enCurso.withLock {
        salvoCancelacion(
            bloque = {
                // Un Operario quitado no sube nada: su sesión se cierra y lo capturado antes se queda en el celular (LIM-06).
                if (!servidor.sigoEnLaFinca()) {
                    alQuitarCuenta()
                    return@salvoCancelacion ResultadoSincronizacion.CuentaQuitada
                }
                val subida = tablas.fold(ResultadoSincronizacion.Completa(0, 0, 0)) { conteo, tabla -> conteo + subir(tabla) }
                descargar()
                subida
            },
            alFallar = { if (it.esFaltaDeConexion()) ResultadoSincronizacion.SinConexion else ResultadoSincronizacion.NoSePudo },
        )
    }

    override fun programar() = programarTrabajo()

    // Con su propio candado, para que una foto lenta no haga esperar a los datos. Puede correr a la vez que ellos: cada
    // escritura de una foto es una sola sentencia, y los datos leen y guardan una fila entera dentro de una transacción.
    // Lo que no pasó queda por subir o por bajar, y se intenta la próxima vez.
    override suspend fun sincronizarFotos() = fotosEnCurso.withLock {
        salvoCancelacion(bloque = { fotos.sincronizar() }, alFallar = {})
    }

    override fun observeEstado(): Flow<EstadoSincronizacion> = combine(
        database.estadoSyncDao().observeCambiosSinSubir(),
        database.estadoSyncDao().observe(),
        database.estadoSyncDao().observeFotosSinSubir(),
    ) { cambiosSinSubir, estado, fotosSinSubir ->
        EstadoSincronizacion(cambiosSinSubir, estado?.fechaUltimaSincronizacion, fotosSinSubir)
    }

    private suspend fun <E : Any> subir(tabla: TablaSincronizada<E>): ResultadoSincronizacion.Completa {
        var conteo = ResultadoSincronizacion.Completa(0, 0, 0)
        for (ids in tabla.pendientes().map(tabla::id).chunked(tamanoLote)) {
            // Se releen: un lote anterior pudo sacar filas de su tabla junto con un Animal rechazado.
            val lote = ids.mapNotNull { tabla.buscar(it) }.filterNot(tabla::sincronizada)
            if (lote.isEmpty()) continue
            val cambios = lote.map { tabla.aCambio(it) }
            val resultados = servidor.subirCambios(JsonArray(cambios))
            check(resultados.size == cambios.size) { "El servidor respondió ${resultados.size} de ${cambios.size} cambios" }
            database.withTransaction {
                lote.indices.forEach { i ->
                    val resultado = resultados[i].jsonObject
                    conteo += aplicar(tabla, lote[i], cambios[i], resultado)
                    cerrarResueltos(cambios[i], resultado)
                }
            }
        }
        return conteo
    }

    /**
     * Los Siblings que la edición nombró en `resuelve` dejan de estar por nombrar. Solo si el servidor la aceptó los
     * cerró: entonces salen también de la tabla `sibling`. Si la contestó como sibling o la rechazó (otro celular
     * avanzó la fila), siguen abiertos en el servidor y aquí vuelven a verse como Conflicto.
     */
    private suspend fun cerrarResueltos(cambio: JsonObject, resultado: JsonObject) {
        val resueltos = cambio.idsQueResuelve()
        if (resueltos.isEmpty()) return
        if (resultado.texto("resultado") == "aceptada") database.siblingDao().deleteAll(resueltos)
        database.resolucionDao().delete(resueltos)
    }

    private suspend fun <E : Any> aplicar(
        tabla: TablaSincronizada<E>,
        enviada: E,
        cambio: JsonObject,
        resultado: JsonObject,
    ): ResultadoSincronizacion.Completa {
        val local = tabla.buscar(tabla.id(enviada))
        val intacta = local?.let { tabla.version(it) == tabla.version(enviada) } == true
        val delServidor = resultado["fila"] as? JsonObject
        return when (val tipo = resultado.texto("resultado")) {
            "aceptada" -> {
                if (intacta) {
                    tabla.guardar(tabla.conLaDelServidor(local!!, checkNotNull(delServidor)))
                } else if (local != null && tabla is TablaConRevisiones) {
                    resultado["rev"]?.jsonPrimitive?.intOrNull?.let { tabla.rebasar(tabla.id(local), it) }
                }
                ResultadoSincronizacion.Completa(1, 0, 0)
            }
            // La fila toma la revisión vigente y la edición local queda como sibling (ADR-0004, regla 3).
            "sibling" -> {
                guardarSibling(tabla.entidad, tabla.id(enviada), cambio)
                if (intacta) tabla.guardar(tabla.conLaDelServidor(local!!, checkNotNull(delServidor)))
                ResultadoSincronizacion.Completa(0, 1, 0)
            }
            // Aunque la fila haya cambiado mientras subía: si no, sus dependientes subirían y se rechazarían una
            // por una mientras ella sigue pendiente. La bandeja guarda su última versión. Una fila que ya quedó
            // sincronizada en este lote salió como dependiente de otra, y no queda nada que rechazar.
            "rechazada" -> {
                if (local != null && !tabla.sincronizada(local)) {
                    val motivo = resultado["motivo"]?.jsonPrimitive?.content.orEmpty()
                    rechazar(tabla, local, resultado.texto("codigo"), motivo, delServidor)
                }
                ResultadoSincronizacion.Completa(0, 0, 1)
            }
            else -> error("Resultado desconocido: $tipo")
        }
    }

    private suspend fun guardarSibling(entidad: String, idFila: String, cambio: JsonObject) {
        database.siblingDao().insert(
            SiblingEntity(
                idVersion = cambio.texto("id_version"),
                entidad = entidad,
                idFila = idFila,
                // Sin rev_base, el servidor tomó la edición como hecha sobre la revisión 1.
                revBase = cambio["rev_base"]?.jsonPrimitive?.intOrNull ?: 1,
                contenido = cambio.getValue("fila").toString(),
                idUsuario = checkNotNull(database.usuarioDao().getOnce()) { "Sin Usuario en el dispositivo" }.idUsuario,
                fechaRecepcion = clock(),
            ),
        )
    }

    /**
     * El cambio rechazado queda en la bandeja (RN-24), con un motivo que empieza con el código de error del
     * servidor (supabase/README.md). Si el servidor tiene la fila, la fila local vuelve a esa versión; si no la
     * tiene, sale de su tabla con sus dependientes, que también quedan en la bandeja.
     */
    private suspend fun <E : Any> rechazar(
        tabla: TablaSincronizada<E>,
        local: E,
        codigo: String,
        motivo: String,
        delServidor: JsonObject?,
    ) {
        bandeja.guardar(tabla.entidad, tabla.id(local), tabla.aFila(local), "$codigo: $motivo")
        if (delServidor != null) {
            // Por ejemplo, una Chapeta cuya liberación se rechazó vuelve a quedar activa.
            val vigente = tabla.conLaDelServidor(local, delServidor)
            abrirPaso(tabla, vigente)
            tabla.guardar(vigente)
            return
        }
        for (dependiente in tabla.sacar(local)) {
            bandeja.guardar(
                dependiente.entidad,
                dependiente.idFila,
                dependiente.fila,
                motivoDeUnDependiente(codigo, tabla.entidad, tabla.id(local), motivo),
            )
        }
    }

    /**
     * Descarga desde el cursor guardado, menos el margen de solapamiento: dos transacciones del servidor pueden
     * confirmar en orden distinto al de sus cursores. Guarda el cursor con cada página, en la misma transacción
     * que la aplica, y al terminar registra la fecha de la última sincronización completa.
     */
    private suspend fun descargar() {
        val estadoSync = database.estadoSyncDao()
        val anterior = estadoSync.getOnce()
        var cursor = anterior?.cursorDescarga ?: 0
        var desde = maxOf(0, cursor - margenSolapamiento)
        do {
            val pagina = servidor.descargarCambios(desde, tamanoPagina)
            val hasta = pagina.getValue("cursor").jsonPrimitive.long
            val hayMas = pagina.getValue("hay_mas").jsonPrimitive.boolean
            check(!hayMas || hasta > desde) { "La página no avanzó desde el cursor $desde" }
            database.withTransaction {
                aplicarPagina(pagina.getValue("referencias").jsonArray + pagina.getValue("cambios").jsonArray)
                cursor = maxOf(cursor, hasta)
                estadoSync.guardar(
                    EstadoSyncEntity(cursorDescarga = cursor, fechaUltimaSincronizacion = anterior?.fechaUltimaSincronizacion),
                )
            }
            desde = hasta
        } while (hayMas)
        estadoSync.guardar(EstadoSyncEntity(cursorDescarga = cursor, fechaUltimaSincronizacion = clock()))
    }

    /** Las filas de una página, de padres a hijos para que cada una encuentre las filas a las que apunta. */
    private suspend fun aplicarPagina(filas: List<JsonElement>) {
        val porTabla = filas.map { it.jsonObject }.groupBy({ it.texto("entidad") }, { it.getValue("fila").jsonObject })
        porTabla["usuario"].orEmpty().forEach { guardarUsuario(it) }
        for (tabla in tablas) {
            val deLaTabla = porTabla[tabla.entidad].orEmpty()
            val enOrden = if (tabla.entidad == "animal") padresPrimero(deLaTabla) else deLaTabla
            enOrden.forEach { bajar(tabla, it) }
        }
        porTabla["sibling"].orEmpty().forEach { sibling ->
            if (sibling.siblingResuelto()) {
                database.siblingDao().delete(sibling.texto("id_version"))
            } else {
                database.siblingDao().insert(sibling.aSiblingEntity())
            }
        }
    }

    private suspend fun <E : Any> bajar(tabla: TablaSincronizada<E>, fila: JsonObject) {
        val local = tabla.buscar(fila.texto("id_${tabla.entidad}"))
        if (local == null) {
            val nueva = tabla.desdeElServidor(fila)
            abrirPaso(tabla, nueva)
            tabla.insertar(nueva)
        } else if (tabla.sincronizada(local) && tabla.alDia(fila, local)) {
            val vigente = tabla.conLaDelServidor(local, fila)
            abrirPaso(tabla, vigente)
            tabla.guardar(vigente)
        }
    }

    /**
     * Despeja los índices únicos locales para guardar [fila], la versión del servidor. Una fila nueva que choca con
     * ella no puede subir: pasa a la bandeja, como la rechazaría el servidor. Una ya sincronizada le cede el lugar.
     */
    private suspend fun <E : Any> abrirPaso(tabla: TablaSincronizada<E>, fila: E) {
        for (choque in tabla.choques(fila)) {
            if (tabla.sincronizada(choque)) {
                tabla.ceder(choque, fila)
            } else {
                rechazar(tabla, choque, CODIGO_UNICIDAD, tabla.motivoDelChoque, delServidor = null)
            }
        }
    }

    // Conserva cuál es el Usuario del dispositivo.
    private suspend fun guardarUsuario(fila: JsonObject) {
        val local = database.usuarioDao().findById(fila.texto("id_usuario"))
        database.usuarioDao().guardar(fila.aUsuarioEntity(delDispositivo = local?.delDispositivo ?: false))
    }

    private companion object {
        /** Pequeño para que, con señal débil, cada lote llegue rápido y lo confirmado no se pierda. */
        const val TAMANO_LOTE = 25

        /** Por la misma razón; una fila descargada pesa menos que un cambio subido, que espera su respuesta. */
        const val TAMANO_PAGINA = 200

        /**
         * Cursores que se vuelven a pedir en cada descarga. Cubre las escrituras que confirmaron después de otras
         * con un cursor mayor. La secuencia es de todas las fincas, pero cada lote toma pocos cursores.
         */
        const val MARGEN_SOLAPAMIENTO = 200L

        const val CODIGO_UNICIDAD = "23505"
    }
}

private operator fun ResultadoSincronizacion.Completa.plus(otro: ResultadoSincronizacion.Completa) =
    ResultadoSincronizacion.Completa(
        aceptados = aceptados + otro.aceptados,
        enConflicto = enConflicto + otro.enConflicto,
        rechazados = rechazados + otro.rechazados,
    )

/** Los Animales de una página con cada padre y cada madre antes que su cría. */
private fun padresPrimero(animales: List<JsonObject>): List<JsonObject> {
    val porId = animales.associateBy { it.texto("id_animal") }
    val orden = LinkedHashSet<JsonObject>()
    fun agregar(animal: JsonObject) {
        if (animal in orden) return
        listOf("id_padre", "id_madre").mapNotNull { animal[it]?.jsonPrimitive?.contentOrNull }.mapNotNull(porId::get)
            .forEach(::agregar)
        orden += animal
    }
    animales.forEach(::agregar)
    return orden.toList()
}

/** Los `id_version` de los Siblings que este cambio nombra en `resuelve`. */
private fun JsonObject.idsQueResuelve(): List<String> =
    (get("resuelve") as? JsonArray)?.map { it.jsonPrimitive.content }.orEmpty()
