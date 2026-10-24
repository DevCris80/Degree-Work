package com.devcris80.prototipo.data.repository

import androidx.room.withTransaction
import com.devcris80.prototipo.data.local.AppDatabase
import com.devcris80.prototipo.data.local.entity.RechazoEntity
import com.devcris80.prototipo.data.local.entity.SiblingEntity
import com.devcris80.prototipo.data.remote.ServidorSincronizacion
import com.devcris80.prototipo.data.remote.esFaltaDeConexion
import com.devcris80.prototipo.domain.repository.ResultadoSubida
import com.devcris80.prototipo.domain.repository.SincronizacionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

/**
 * El motor de sincronización (DAT-05, CU-20): sube los cambios locales por lotes pequeños, de padres a hijos, y
 * aplica la respuesta de `subir_cambios` fila por fila (ADR-0004). Cada lote se confirma por separado: si se
 * pierde la señal, lo ya aplicado queda y el resto sigue pendiente.
 *
 * Sus escrituras no llevan EDICION_LOCAL, y solo tocan una fila si no cambió mientras el lote viajaba. Si
 * cambió, la escritura nueva queda pendiente y sube en la próxima sincronización.
 */
class MotorSincronizacion(
    private val servidor: ServidorSincronizacion,
    private val database: AppDatabase,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val nuevoIdRechazo: () -> String = { UUID.randomUUID().toString() },
    private val tamanoLote: Int = TAMANO_LOTE,
) : SincronizacionRepository {
    private val tablas = tablasQueSuben(database)

    override suspend fun subirCambios(): ResultadoSubida = try {
        tablas.fold(ResultadoSubida.Completa(0, 0, 0)) { conteo, tabla -> conteo + subir(tabla) }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        if (e.esFaltaDeConexion()) ResultadoSubida.SinConexion else ResultadoSubida.NoSePudo
    }

    private suspend fun <E : Any> subir(tabla: TablaQueSube<E>): ResultadoSubida.Completa {
        var conteo = ResultadoSubida.Completa(0, 0, 0)
        for (ids in tabla.pendientes().map(tabla::id).chunked(tamanoLote)) {
            // Se releen: un lote anterior pudo sacar filas de su tabla junto con un Animal rechazado.
            val lote = ids.mapNotNull { tabla.buscar(it) }.filterNot(tabla::sincronizada)
            if (lote.isEmpty()) continue
            val cambios = lote.map(tabla::aCambio)
            val resultados = servidor.subirCambios(JsonArray(cambios))
            check(resultados.size == cambios.size) { "El servidor respondió ${resultados.size} de ${cambios.size} cambios" }
            database.withTransaction {
                lote.indices.forEach { i -> conteo += aplicar(tabla, lote[i], cambios[i], resultados[i].jsonObject) }
            }
        }
        return conteo
    }

    private suspend fun <E : Any> aplicar(
        tabla: TablaQueSube<E>,
        enviada: E,
        cambio: JsonObject,
        resultado: JsonObject,
    ): ResultadoSubida.Completa {
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
                ResultadoSubida.Completa(1, 0, 0)
            }
            // La fila toma la revisión vigente y la edición local queda como sibling (ADR-0004, regla 3).
            "sibling" -> {
                guardarSibling(tabla.entidad, tabla.id(enviada), cambio)
                if (intacta) tabla.guardar(tabla.conLaDelServidor(local!!, checkNotNull(delServidor)))
                ResultadoSubida.Completa(0, 1, 0)
            }
            "rechazada" -> {
                if (intacta) rechazar(tabla, local!!, cambio, resultado, delServidor)
                ResultadoSubida.Completa(0, 0, 1)
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
        tabla: TablaQueSube<E>,
        local: E,
        cambio: JsonObject,
        resultado: JsonObject,
        delServidor: JsonObject?,
    ) {
        val codigo = resultado.texto("codigo")
        val motivo = resultado["motivo"]?.jsonPrimitive?.content.orEmpty()
        guardarRechazo(tabla.entidad, tabla.id(local), cambio.getValue("fila").jsonObject, "$codigo: $motivo")
        if (delServidor != null) {
            tabla.guardar(tabla.conLaDelServidor(local, delServidor))
            return
        }
        for (dependiente in tabla.sacar(local)) {
            guardarRechazo(
                dependiente.entidad,
                dependiente.idFila,
                dependiente.fila,
                "$codigo: Se rechazó el ${tabla.entidad} ${tabla.id(local)}. $motivo",
            )
        }
    }

    private suspend fun guardarRechazo(entidad: String, idFila: String, fila: JsonObject, motivo: String) {
        database.rechazoDao().insert(
            RechazoEntity(
                idRechazo = nuevoIdRechazo(),
                entidad = entidad,
                idFila = idFila,
                contenido = fila.toString(),
                motivo = motivo,
                fechaRechazo = clock(),
            ),
        )
    }

    private companion object {
        /** Pequeño para que, con señal débil, cada lote llegue rápido y lo confirmado no se pierda. */
        const val TAMANO_LOTE = 25
    }
}

private operator fun ResultadoSubida.Completa.plus(otro: ResultadoSubida.Completa) = ResultadoSubida.Completa(
    aceptados = aceptados + otro.aceptados,
    enConflicto = enConflicto + otro.enConflicto,
    rechazados = rechazados + otro.rechazados,
)
