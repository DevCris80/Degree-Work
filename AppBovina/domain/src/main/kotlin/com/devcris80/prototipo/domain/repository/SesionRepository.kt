package com.devcris80.prototipo.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * La sesión de Supabase Auth de este dispositivo (ADR-0002). Iniciar sesión necesita señal; una vez
 * guardada, la sesión sirve sin señal y se renueva sola cuando vuelve.
 */
interface SesionRepository {
    /** Si este dispositivo tiene una sesión guardada. No consulta la red. */
    fun observeSesionGuardada(): Flow<Boolean>

    /**
     * Valida las credenciales y deja abierta en este dispositivo la cuenta: sus datos propios, aislados de los de
     * las demás cuentas guardadas (ADR-0006), con la finca y el Usuario como están en el servidor.
     */
    suspend fun iniciarSesion(correo: String, contrasena: String): ResultadoInicioSesion

    /**
     * Pide a Supabase Auth que envíe el correo de recuperación. Un correo sin cuenta responde igual que uno
     * con cuenta, para no revelar quién está registrado.
     */
    suspend fun recuperarContrasena(correo: String): ResultadoRecuperacion

    /**
     * Crea la identidad en Supabase Auth y, en el mismo alta, la finca, su primer Usuario Ganadero y la aceptación
     * de [versionAutorizacion]; después deja la finca y el Usuario en el dispositivo. Si algo falla, el dispositivo
     * queda sin sesión y sin filas nuevas.
     */
    suspend fun crearCuenta(
        nombre: String,
        nombreFinca: String,
        correo: String,
        contrasena: String,
        versionAutorizacion: String,
    ): ResultadoCreacionCuenta

    /**
     * Crea la identidad en Supabase Auth con un código de invitación y, en el mismo alta, el Usuario Operario de la
     * finca que generó el código y la aceptación de [versionAutorizacion]; después deja la finca y el Usuario en el
     * dispositivo. Un código usado, vencido o inexistente no crea la cuenta (RN-18).
     */
    suspend fun unirseAFinca(
        nombre: String,
        codigoInvitacion: String,
        correo: String,
        contrasena: String,
        versionAutorizacion: String,
    ): ResultadoCreacionCuenta

    /** Olvida la sesión; los datos de la cuenta quedan en el dispositivo como una Cuenta guardada. */
    suspend fun cerrarSesion()

    /**
     * Olvida la sesión y borra del dispositivo los datos de la cuenta que la tenía, sin subir lo que le quedara
     * (RN-28): el servidor ya no le da finca.
     */
    suspend fun cerrarSesionYBorrarCuenta()
}

sealed interface ResultadoCreacionCuenta {
    data object Exito : ResultadoCreacionCuenta

    /** El correo ya tiene una cuenta con finca: hay que iniciar sesión. */
    data object CorreoYaRegistrado : ResultadoCreacionCuenta

    /** El servidor no aceptó la contraseña, por ejemplo por débil. */
    data object ContrasenaRechazada : ResultadoCreacionCuenta

    data object SinConexion : ResultadoCreacionCuenta

    /** El servidor no aceptó el código de invitación: no existe, ya se usó o venció. */
    data object CodigoRechazado : ResultadoCreacionCuenta

    /** El servidor respondió con un error inesperado. */
    data object NoSePudo : ResultadoCreacionCuenta
}

sealed interface ResultadoRecuperacion {
    /** El correo salió o ya va en camino: también cuando el servidor limita los envíos de una cuenta. */
    data object Enviado : ResultadoRecuperacion
    data object SinConexion : ResultadoRecuperacion

    /** El servidor respondió con un error inesperado. */
    data object NoSePudo : ResultadoRecuperacion
}

sealed interface ResultadoInicioSesion {
    data object Exito : ResultadoInicioSesion
    data object CredencialesInvalidas : ResultadoInicioSesion
    data object SinConexion : ResultadoInicioSesion

    /** La cuenta existe, pero ninguna finca la tiene como Usuario. */
    data object SinFinca : ResultadoInicioSesion

    /**
     * La cuenta estaba guardada en el celular, pero el servidor ya no le da finca (la quitaron): sus datos
     * se borraron del celular (RN-28).
     */
    data object CuentaQuitada : ResultadoInicioSesion

    /** El servidor respondió con un error inesperado. */
    data object NoSePudo : ResultadoInicioSesion
}
