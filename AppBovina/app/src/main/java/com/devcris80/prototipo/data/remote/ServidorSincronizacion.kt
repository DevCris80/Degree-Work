package com.devcris80.prototipo.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Las funciones de sincronización del servidor (ADR-0005). El contrato está en supabase/README.md. */
interface ServidorSincronizacion {
    /** Llama `subir_cambios` con un lote y devuelve un resultado por cambio, en el mismo orden. */
    suspend fun subirCambios(lote: JsonArray): JsonArray

    /** Llama `descargar_cambios`: una página de los cambios de la finca con cursor mayor que [desde]. */
    suspend fun descargarCambios(desde: Long, limite: Int): JsonObject

    /** Llama `sigo_en_la_finca`: si la cuenta de la sesión sigue en una finca, o un Ganadero la quitó. */
    suspend fun sigoEnLaFinca(): Boolean
}

/** Llama las funciones por PostgREST con el JWT de la sesión guardada. */
class SupabaseServidorSincronizacion(private val supabase: SupabaseClient) : ServidorSincronizacion {
    override suspend fun subirCambios(lote: JsonArray): JsonArray =
        supabase.postgrest.rpc("subir_cambios", buildJsonObject { put("lote", lote) }).decodeAs<JsonArray>()

    override suspend fun descargarCambios(desde: Long, limite: Int): JsonObject =
        supabase.postgrest.rpc(
            "descargar_cambios",
            buildJsonObject {
                put("desde", desde)
                put("limite", limite)
            },
        ).decodeAs<JsonObject>()

    override suspend fun sigoEnLaFinca(): Boolean = supabase.postgrest.rpc("sigo_en_la_finca").decodeAs<Boolean>()
}
