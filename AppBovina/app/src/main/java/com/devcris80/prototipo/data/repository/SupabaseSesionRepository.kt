package com.devcris80.prototipo.data.repository

import com.devcris80.prototipo.data.local.dao.PerfilFincaDao
import com.devcris80.prototipo.data.local.dao.UsuarioDao
import com.devcris80.prototipo.data.remote.SesionCifrada
import com.devcris80.prototipo.data.remote.esFaltaDeConexion
import com.devcris80.prototipo.domain.repository.ResultadoInicioSesion
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

/**
 * Inicia sesión contra Supabase Auth y deja en Room la finca y el Usuario de la cuenta, tal como
 * están en el servidor. Un dispositivo es de un solo Usuario: si ya tiene los datos de otra finca u
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
            usuarioDao.guardar(usuario.aUsuarioEntity())
        }
        return ResultadoInicioSesion.Exito
    }

    private suspend fun filaUnica(tabla: String, columna: String, valor: String): JsonObject? =
        supabase.from(tabla).select { filter { eq(columna, valor) } }.decodeList<JsonObject>().singleOrNull()
}
