package com.devcris80.prototipo.data.repository

import com.devcris80.prototipo.data.remote.esFaltaDeConexion
import com.devcris80.prototipo.domain.repository.Invitacion
import com.devcris80.prototipo.domain.repository.InvitacionRepository
import com.devcris80.prototipo.domain.repository.ResultadoInvitacion
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** Genera los códigos de invitación llamando `generar_invitacion` con el JWT de la sesión (supabase/migrations). */
@OptIn(ExperimentalTime::class)
class SupabaseInvitacionRepository(private val supabase: SupabaseClient) : InvitacionRepository {
    override suspend fun generar(): ResultadoInvitacion = try {
        val respuesta = supabase.postgrest.rpc("generar_invitacion").decodeAs<JsonObject>()
        ResultadoInvitacion.Generada(
            Invitacion(
                codigo = respuesta.getValue("codigo").jsonPrimitive.content,
                venceEnMillis = Instant.parse(respuesta.getValue("fecha_vencimiento").jsonPrimitive.content)
                    .toEpochMilliseconds(),
            ),
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: RestException) {
        // PostgREST contesta 403 al 42501 (el rol no lo permite, RN-18) y 401 a un JWT que ya no vale.
        if (e.statusCode == 401 || e.statusCode == 403) {
            ResultadoInvitacion.NoPermitido
        } else {
            ResultadoInvitacion.NoSePudo
        }
    } catch (e: Exception) {
        if (e.esFaltaDeConexion()) ResultadoInvitacion.SinConexion else ResultadoInvitacion.NoSePudo
    }
}
