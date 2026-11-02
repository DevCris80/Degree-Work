package com.devcris80.prototipo.data.repository

import androidx.room.withTransaction
import com.devcris80.prototipo.data.local.CuentasEnElCelular
import com.devcris80.prototipo.data.remote.SesionCifrada
import com.devcris80.prototipo.data.remote.esFaltaDeConexion
import com.devcris80.prototipo.domain.repository.ResultadoCreacionCuenta
import com.devcris80.prototipo.domain.repository.ResultadoInicioSesion
import com.devcris80.prototipo.domain.repository.ResultadoRecuperacion
import com.devcris80.prototipo.domain.repository.SesionRepository
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
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Inicia sesión o crea la cuenta contra Supabase Auth y deja en la base de esa cuenta la finca y el Usuario, tal
 * como están en el servidor. Un celular guarda varias cuentas, cada una con su base (ADR-0006): entrar con otra
 * no toca los datos de las demás. Si el servidor ya no le da finca a una cuenta que el celular guardaba, sus
 * datos se borran (RN-28).
 */
class SupabaseSesionRepository(
    private val supabase: SupabaseClient,
    private val sesion: SesionCifrada,
    private val cuentas: CuentasEnElCelular,
) : SesionRepository {
    override fun observeSesionGuardada(): Flow<Boolean> = sesion.guardada

    override suspend fun iniciarSesion(correo: String, contrasena: String): ResultadoInicioSesion = try {
        supabase.auth.signInWith(Email) {
            email = correo
            password = contrasena
        }
        val resultado = guardarCuenta(checkNotNull(supabase.auth.currentUserOrNull()).id, correo)
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
    ): ResultadoCreacionCuenta = registrar(correo, contrasena, conCodigo = false) {
        // El trigger crear_finca (supabase/migrations) crea la finca en el mismo alta, con estos metadatos.
        put("nombre", nombre)
        put("nombre_finca", nombreFinca)
        put("version_autorizacion", versionAutorizacion)
    }

    override suspend fun unirseAFinca(
        nombre: String,
        codigoInvitacion: String,
        correo: String,
        contrasena: String,
        versionAutorizacion: String,
    ): ResultadoCreacionCuenta = registrar(correo, contrasena, conCodigo = true) {
        // El mismo trigger canjea el código en el alta: sin un código vigente, el servidor no crea la identidad.
        put("nombre", nombre)
        put("codigo_invitacion", codigoInvitacion)
        put("version_autorizacion", versionAutorizacion)
    }

    private suspend fun registrar(
        correo: String,
        contrasena: String,
        conCodigo: Boolean,
        metadatos: JsonObjectBuilder.() -> Unit,
    ): ResultadoCreacionCuenta {
        val resultado = try {
            supabase.auth.signUpWith(Email) {
                email = correo
                password = contrasena
                data = buildJsonObject(metadatos)
            }
            // La confirmación de correo está desactivada en el proyecto: el registro deja la sesión abierta.
            val idUsuario = checkNotNull(supabase.auth.currentUserOrNull()) { "El registro no dejó sesión" }.id
            if (guardarCuenta(idUsuario, correo) == ResultadoInicioSesion.Exito) {
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
                // Si el trigger rechaza el código, Auth solo dice que falló al guardar al usuario: con un código en
                // el alta, esa es la señal de que no lo aceptó.
                AuthErrorCode.UnexpectedFailure ->
                    if (conCodigo) ResultadoCreacionCuenta.CodigoRechazado else ResultadoCreacionCuenta.NoSePudo
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
            withContext(NonCancellable) {
                supabase.auth.clearSession()
                // La cuenta queda guardada, pero ya no es la abierta.
                cuentas.desactivar()
            }
        }
    }

    override suspend fun cerrarSesionYBorrarCuenta() {
        val abierta = cuentas.activa.value
        // El aviso se anota antes: al caer la sesión la app navega a Iniciar sesión, que lo pide apenas aparece.
        if (abierta != null) cuentas.marcarCuentaQuitada()
        cerrarSesion()
        if (abierta != null) withContext(NonCancellable) { cuentas.eliminar(abierta) }
    }

    override suspend fun consumirAvisoDeCuentaQuitada(): Boolean = cuentas.consumirCuentaQuitada()

    private suspend fun guardarCuenta(idUsuario: String, correo: String): ResultadoInicioSesion {
        val usuario = filaUnica("usuario", "id_usuario", idUsuario)
        val finca = usuario?.let { filaUnica("perfil_finca", "id_perfil_finca", it.texto("id_perfil_finca")) }
        if (usuario == null || finca == null) return sinFinca(idUsuario)
        val existia = cuentas.entradaDe(idUsuario) != null
        val entrada = cuentas.abrirCuenta(idUsuario, correo)
        try {
            val base = cuentas.base(entrada.archivo)
            // La finca antes que el Usuario, que la referencia.
            base.withTransaction {
                base.perfilFincaDao().guardar(finca.aPerfilFincaEntity())
                base.usuarioDao().guardar(usuario.aUsuarioEntity(delDispositivo = true))
            }
        } catch (e: Exception) {
            // Una cuenta nueva que no pudo guardarse no deja una base vacía registrada.
            if (!existia) withContext(NonCancellable) { cuentas.eliminar(entrada.archivo) }
            throw e
        }
        cuentas.activar(entrada.archivo)
        return ResultadoInicioSesion.Exito
    }

    /** Una cuenta que el celular guardaba y el servidor ya no tiene en ninguna finca pierde sus datos aquí (RN-28). */
    private suspend fun sinFinca(idUsuario: String): ResultadoInicioSesion {
        val guardada = cuentas.entradaDe(idUsuario) ?: return ResultadoInicioSesion.SinFinca
        cuentas.eliminar(guardada.archivo)
        return ResultadoInicioSesion.CuentaQuitada
    }

    private suspend fun filaUnica(tabla: String, columna: String, valor: String): JsonObject? =
        supabase.from(tabla).select { filter { eq(columna, valor) } }.decodeList<JsonObject>().singleOrNull()
}
