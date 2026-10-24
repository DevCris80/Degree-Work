package com.devcris80.prototipo.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject

/** Las funciones de sincronización del servidor (ADR-0005). El contrato está en supabase/README.md. */
interface ServidorSincronizacion {
    /** Llama `subir_cambios` con un lote y devuelve un resultado por cambio, en el mismo orden. */
    suspend fun subirCambios(lote: JsonArray): JsonArray
}

/** Llama las funciones por PostgREST con el JWT de la sesión guardada. */
class SupabaseServidorSincronizacion(private val supabase: SupabaseClient) : ServidorSincronizacion {
    override suspend fun subirCambios(lote: JsonArray): JsonArray =
        supabase.postgrest.rpc("subir_cambios", buildJsonObject { put("lote", lote) }).decodeAs<JsonArray>()
}
