package com.devcris80.prototipo.data.repository

import com.devcris80.prototipo.data.remote.ServidorSincronizacion
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Habla el contrato de supabase/README.md. Guarda cada lote que recibe y responde con [responder]; por defecto,
 * acepta todo. Descarga las filas de [filas] por cursor, en páginas de a lo sumo el límite que se le pide.
 */
internal class ServidorFalso : ServidorSincronizacion {
    val lotes = mutableListOf<JsonArray>()
    var responder: suspend (JsonArray) -> JsonArray = ::aceptarTodo

    /** Los cambios de la finca en el servidor, cada uno con su cursor. */
    val filas = mutableListOf<CambioDelServidor>()

    /** Las referencias que trae cada página, cualquiera sea. */
    var referencias = JsonArray(emptyList())

    /** Cada descarga que se pidió: desde qué cursor y con qué límite. */
    val descargas = mutableListOf<Pair<Long, Int>>()

    /** Si no es nulo, se llama antes de responder cada página; puede lanzar una excepción. */
    var antesDeDescargar: (() -> Unit)? = null

    /** Lo que contesta `sigo_en_la_finca`: falso cuando un Ganadero quitó a este Usuario. */
    var estaEnLaFinca = true
    var consultasDeFinca = 0

    override suspend fun sigoEnLaFinca(): Boolean {
        consultasDeFinca++
        return estaEnLaFinca
    }

    override suspend fun subirCambios(lote: JsonArray): JsonArray {
        lotes += lote
        return responder(lote)
    }

    override suspend fun descargarCambios(desde: Long, limite: Int): JsonObject {
        descargas += desde to limite
        antesDeDescargar?.invoke()
        val siguientes = filas.filter { it.cursor > desde }.sortedBy { it.cursor }
        val pagina = siguientes.take(limite)
        return buildJsonObject {
            put("cambios", JsonArray(pagina.map { it.json() }))
            put("referencias", referencias)
            put("cursor", pagina.lastOrNull()?.cursor ?: desde)
            put("hay_mas", siguientes.size > limite)
        }
    }
}

internal data class CambioDelServidor(val entidad: String, val cursor: Long, val fila: JsonObject) {
    fun json() = buildJsonObject {
        put("entidad", entidad)
        put("cursor", cursor)
        put("fila", fila)
    }
}

internal fun aceptarTodo(lote: JsonArray) = JsonArray(lote.map { aceptada(it.jsonObject) })

/** Lo que responde el servidor a un cambio aceptado: la fila tal como quedó, con su revisión si la lleva. */
internal fun aceptada(cambio: JsonObject): JsonObject {
    val fila = cambio.getValue("fila").jsonObject
    if ("id_version" !in cambio) return resultado(cambio, "aceptada", fila = fila)
    val rev = (cambio["rev_base"]?.jsonPrimitive?.intOrNull ?: 0) + 1
    return resultado(cambio, "aceptada", rev = rev, fila = JsonObject(fila + ("rev" to JsonPrimitive(rev))))
}

internal fun resultado(
    cambio: JsonObject,
    resultado: String,
    rev: Int? = null,
    fila: JsonObject? = null,
    codigo: String? = null,
    motivo: String? = null,
) = buildJsonObject {
    put("entidad", cambio.getValue("entidad"))
    put("resultado", resultado)
    rev?.let { put("rev", it) }
    put("fila", fila ?: JsonNull)
    codigo?.let { put("codigo", it) }
    motivo?.let { put("motivo", it) }
}
