package com.devcris80.prototipo.data.repository

import com.devcris80.prototipo.data.local.dao.PerfilFincaDao
import com.devcris80.prototipo.data.local.dao.UsuarioDao
import com.devcris80.prototipo.data.local.entity.PerfilFincaEntity
import com.devcris80.prototipo.data.local.entity.UsuarioEntity
import com.devcris80.prototipo.data.remote.SesionCifrada
import com.devcris80.prototipo.domain.repository.ResultadoInicioSesion
import com.devcris80.prototipo.domain.repository.SesionRepository
import com.devcris80.prototipo.domain.repository.Transaccion
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.postgrest.from
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Inicia sesión contra Supabase Auth y deja en Room la finca y el Usuario de la cuenta, tal como
 * están en el servidor. Un dispositivo guarda una sola finca: si ya tiene otra, no inicia sesión
 * (hay que desinstalar la app para empezar limpio) y nunca borra datos por su cuenta.
 */
class SupabaseSesionRepository(
    private val supabase: SupabaseClient,
    private val sesion: SesionCifrada,
    private val perfilFincaDao: PerfilFincaDao,
    private val usuarioDao: UsuarioDao,
    private val transaccion: Transaccion,
) : SesionRepository {
    override fun observeSesionGuardada(): Flow<Boolean> = sesion.guardada

    override suspend fun iniciarSesion(correo: String, contrasena: String): ResultadoInicioSesion {
        try {
            supabase.auth.signInWith(Email) {
                email = correo
                password = contrasena
            }
            val idUsuario = supabase.auth.currentUserOrNull()?.id ?: return ResultadoInicioSesion.CredencialesInvalidas
            val usuario = supabase.from("usuario")
                .select { filter { eq("id_usuario", idUsuario) } }
                .decodeList<JsonObject>()
                .singleOrNull()
                ?: return descartarSesion(ResultadoInicioSesion.SinFinca)
            val finca = supabase.from("perfil_finca")
                .select { filter { eq("id_perfil_finca", usuario.texto("id_perfil_finca")) } }
                .decodeList<JsonObject>()
                .singleOrNull()
                ?: return descartarSesion(ResultadoInicioSesion.SinFinca)

            val fincaLocal = perfilFincaDao.getOnce()
            if (fincaLocal != null && fincaLocal.idPerfilFinca != finca.texto("id_perfil_finca")) {
                return descartarSesion(ResultadoInicioSesion.OtraFincaEnElDispositivo)
            }
            // Llegan del servidor: ya están sincronizados.
            transaccion.ejecutar {
                perfilFincaDao.guardar(
                    PerfilFincaEntity(
                        idPerfilFinca = finca.texto("id_perfil_finca"),
                        nombreFinca = finca.texto("nombre_finca"),
                        sincronizado = true,
                        fechaModificacion = finca.fecha("fecha_modificacion"),
                    ),
                )
                usuarioDao.guardar(
                    UsuarioEntity(
                        idUsuario = idUsuario,
                        idPerfilFinca = usuario.texto("id_perfil_finca"),
                        nombre = usuario.texto("nombre"),
                        rol = usuario.texto("rol"),
                        sincronizado = true,
                        fechaModificacion = usuario.fecha("fecha_modificacion"),
                    ),
                )
            }
            return ResultadoInicioSesion.Exito
        } catch (e: AuthRestException) {
            return ResultadoInicioSesion.CredencialesInvalidas
        } catch (e: HttpRequestException) {
            return descartarSesion(ResultadoInicioSesion.SinConexion)
        } catch (e: HttpRequestTimeoutException) {
            return descartarSesion(ResultadoInicioSesion.SinConexion)
        } catch (e: IOException) {
            return descartarSesion(ResultadoInicioSesion.SinConexion)
        }
    }

    override suspend fun cerrarSesion() {
        // Sin señal el servidor no se entera, pero la sesión se olvida igual en el dispositivo.
        runCatching { supabase.auth.signOut(SignOutScope.LOCAL) }
        sesion.deleteSession()
    }

    private suspend fun descartarSesion(resultado: ResultadoInicioSesion): ResultadoInicioSesion {
        cerrarSesion()
        return resultado
    }
}

private fun JsonObject.texto(campo: String): String = getValue(campo).jsonPrimitive.content

@OptIn(ExperimentalTime::class)
private fun JsonObject.fecha(campo: String): Long = Instant.parse(texto(campo)).toEpochMilliseconds()
