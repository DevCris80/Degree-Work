package com.devcris80.prototipo.data.repository

import com.devcris80.prototipo.data.local.dao.PerfilFincaDao
import com.devcris80.prototipo.data.local.dao.UsuarioDao
import com.devcris80.prototipo.data.remote.SesionCifrada
import com.devcris80.prototipo.data.remote.esFaltaDeConexion
import com.devcris80.prototipo.domain.repository.ResultadoCreacionCuenta
import com.devcris80.prototipo.domain.repository.ResultadoInicioSesion
import com.devcris80.prototipo.domain.repository.ResultadoRecuperacion
import com.devcris80.prototipo.domain.repository.SesionRepository
import com.devcris80.prototipo.domain.repository.Transaccion
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Inicia sesión o crea la cuenta contra Supabase Auth y deja en Room la finca y el Usuario de la cuenta,
 * tal como están en el servidor. Un dispositivo es de un solo Usuario: si ya tiene los datos de otra finca u
 * otro Usuario, no inicia sesión (hay que desinstalar la app para empezar limpio) y nunca borra
 * datos por su cuenta.
 */
class SupabaseSesionRepository(
    private val supabase: SupabaseClient,
    private val sesion: SesionCifrada,
    private val perfilFincaDao: PerfilFincaDao,
    private val usuarioDao: UsuarioDao,
    private val transaccion: Transaccion,
) : SesionRepository {
    override fun observeSesionGuardada(): Flow<Boolean> = sesion.guardada

    override suspend fun iniciarSesion(correo: String, contrasena: String): ResultadoInicioSesion = try {
        supabase.auth.signInWith(Email) {
            email = correo
            password = contrasena
        }
        val resultado = guardarCuenta(checkNotNull(supabase.auth.currentUserOrNull()).id)
        if (resultado != ResultadoInicioSesion.Exito) cerrarSesion()
        resultado
    } catch (e: CancellationException) {
        throw e
    } catch (e: AuthRestException) {
        if (e.errorCode == AuthErrorCode.InvalidCredentials) {
            ResultadoInicioSesion.CredencialesInvalidas
        } else {
            ResultadoInicioSesion.NoSePudo
        }
    } catch (e: Exception) {
        cerrarSesion()
        if (e.esFaltaDeConexion()) ResultadoInicioSesion.SinConexion else ResultadoInicioSesion.NoSePudo
    }

    override suspend fun crearCuenta(
        nombre: String,
        nombreFinca: String,
        correo: String,
        contrasena: String,
        versionAutorizacion: String,
    ): ResultadoCreacionCuenta {
        // Con datos de otra cuenta en el dispositivo no se crea nada, ni siquiera en el servidor.
        if (perfilFincaDao.getOnce() != null || usuarioDao.getOnce() != null) {
            return ResultadoCreacionCuenta.OtraCuentaEnElDispositivo
        }
        val resultado = try {
            // El trigger crear_finca (supabase/migrations) crea la finca en el mismo alta, con estos metadatos.
            supabase.auth.signUpWith(Email) {
                email = correo
                password = contrasena
                data = buildJsonObject {
                    put("nombre", nombre)
                    put("nombre_finca", nombreFinca)
                    put("version_autorizacion", versionAutorizacion)
                }
            }
            // La confirmación de correo está desactivada en el proyecto: el registro deja la sesión abierta.
            val idUsuario = checkNotNull(supabase.auth.currentUserOrNull()) { "El registro no dejó sesión" }.id
            if (guardarCuenta(idUsuario) == ResultadoInicioSesion.Exito) {
                ResultadoCreacionCuenta.Exito
            } else {
                ResultadoCreacionCuenta.NoSePudo
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: AuthRestException) {
            when (e.errorCode) {
                AuthErrorCode.UserAlreadyExists, AuthErrorCode.EmailExists -> ResultadoCreacionCuenta.CorreoYaRegistrado
                AuthErrorCode.WeakPassword -> ResultadoCreacionCuenta.ContrasenaRechazada
                else -> ResultadoCreacionCuenta.NoSePudo
            }
        } catch (e: Exception) {
            if (e.esFaltaDeConexion()) ResultadoCreacionCuenta.SinConexion else ResultadoCreacionCuenta.NoSePudo
        }
        // Sin finca guardada la sesión no sirve: el dispositivo queda como antes de intentarlo.
        if (resultado != ResultadoCreacionCuenta.Exito) cerrarSesion()
        return resultado
    }

    override suspend fun recuperarContrasena(correo: String): ResultadoRecuperacion = try {
        supabase.auth.resetPasswordForEmail(correo)
        ResultadoRecuperacion.Enviado
    } catch (e: CancellationException) {
        throw e
    } catch (e: AuthRestException) {
        // Un correo con cuenta, pedido otra vez de seguida, topa con este límite. Mostrarlo como fallo delataría
        // qué correos están registrados: el primer correo ya va en camino, así que se confirma igual.
        if (e.errorCode == AuthErrorCode.OverEmailSendRateLimit) {
            ResultadoRecuperacion.Enviado
        } else {
            ResultadoRecuperacion.NoSePudo
        }
    } catch (e: Exception) {
        if (e.esFaltaDeConexion()) ResultadoRecuperacion.SinConexion else ResultadoRecuperacion.NoSePudo
    }

    override suspend fun cerrarSesion() {
        try {
            supabase.auth.signOut(SignOutScope.LOCAL)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Sin señal el servidor no se entera; la sesión se olvida igual en el dispositivo.
        } finally {
            // signOut no limpia si falla: clearSession olvida el token en memoria y el guardado.
            withContext(NonCancellable) { supabase.auth.clearSession() }
        }
    }

    private suspend fun guardarCuenta(idUsuario: String): ResultadoInicioSesion {
        val usuario = filaUnica("usuario", "id_usuario", idUsuario) ?: return ResultadoInicioSesion.SinFinca
        val finca = filaUnica("perfil_finca", "id_perfil_finca", usuario.texto("id_perfil_finca"))
            ?: return ResultadoInicioSesion.SinFinca
        val fincaLocal = perfilFincaDao.getOnce()
        val usuarioLocal = usuarioDao.getOnce()
        if (fincaLocal != null && fincaLocal.idPerfilFinca != usuario.texto("id_perfil_finca") ||
            usuarioLocal != null && usuarioLocal.idUsuario != idUsuario
        ) {
            return ResultadoInicioSesion.OtraCuentaEnElDispositivo
        }
        // La finca antes que el Usuario, que la referencia.
        transaccion.ejecutar {
            perfilFincaDao.guardar(finca.aPerfilFincaEntity())
            usuarioDao.guardar(usuario.aUsuarioEntity(delDispositivo = true))
        }
        return ResultadoInicioSesion.Exito
    }

    private suspend fun filaUnica(tabla: String, columna: String, valor: String): JsonObject? =
        supabase.from(tabla).select { filter { eq(columna, valor) } }.decodeList<JsonObject>().singleOrNull()
}
