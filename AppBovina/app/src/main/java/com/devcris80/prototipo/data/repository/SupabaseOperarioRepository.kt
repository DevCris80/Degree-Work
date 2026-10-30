package com.devcris80.prototipo.data.repository

import com.devcris80.prototipo.data.remote.esFaltaDeConexion
import com.devcris80.prototipo.domain.repository.OperarioDeLaFinca
import com.devcris80.prototipo.domain.repository.OperarioRepository
import com.devcris80.prototipo.domain.repository.ResultadoListaOperarios
import com.devcris80.prototipo.domain.repository.ResultadoQuitarOperario
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Lista los Operarios de la finca y los quita con `quitar_operario`, con el JWT de la sesión (supabase/migrations).
 * Es lo único que lee `usuario` directo del servidor: la lista del celular no distingue a los quitados.
 */
class SupabaseOperarioRepository(private val supabase: SupabaseClient) : OperarioRepository {
    override suspend fun listar(): ResultadoListaOperarios = try {
        val filas = supabase.from("usuario").select().decodeList<JsonObject>()
        ResultadoListaOperarios.Lista(
            filas
                .filter { it.getValue("rol").jsonPrimitive.content == "Operario" }
                .filter { it["fecha_quitado"]?.jsonPrimitive?.contentOrNull == null }
                .map { OperarioDeLaFinca(it.getValue("id_usuario").jsonPrimitive.content, it.getValue("nombre").jsonPrimitive.content) }
                .sortedBy { it.nombre.lowercase() },
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        if (e.esFaltaDeConexion()) ResultadoListaOperarios.SinConexion else ResultadoListaOperarios.NoSePudo
    }

    override suspend fun quitar(idUsuario: String): ResultadoQuitarOperario = try {
        supabase.postgrest.rpc("quitar_operario", buildJsonObject { put("id_operario", idUsuario) })
        ResultadoQuitarOperario.Quitado
    } catch (e: CancellationException) {
        throw e
    } catch (e: RestException) {
        // PostgREST contesta 403 al 42501 (no es Ganadero, o la sesión ya no vale) y 400 al 22023 (no hay un Operario
        // de la finca con ese id: alguien lo quitó antes). El id lo manda la propia app, así que no hay otro 400.
        when (e.statusCode) {
            401, 403 -> ResultadoQuitarOperario.NoPermitido
            400 -> ResultadoQuitarOperario.NoDisponible
            else -> ResultadoQuitarOperario.NoSePudo
        }
    } catch (e: Exception) {
        if (e.esFaltaDeConexion()) ResultadoQuitarOperario.SinConexion else ResultadoQuitarOperario.NoSePudo
    }
}
