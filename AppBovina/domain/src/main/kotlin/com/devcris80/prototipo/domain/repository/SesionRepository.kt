package com.devcris80.prototipo.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * La sesión de Supabase Auth de este dispositivo (ADR-0002). Iniciar sesión necesita señal; una vez
 * guardada, la sesión sirve sin señal y se renueva sola cuando vuelve.
 */
interface SesionRepository {
    /** Si este dispositivo tiene una sesión guardada. No consulta la red. */
    fun observeSesionGuardada(): Flow<Boolean>

    /** Valida las credenciales y deja en este dispositivo la finca y el Usuario de la cuenta. */
    suspend fun iniciarSesion(correo: String, contrasena: String): ResultadoInicioSesion

    /**
     * Pide a Supabase Auth que envíe el correo de recuperación. Un correo sin cuenta responde igual que uno
     * con cuenta, para no revelar quién está registrado.
     */
    suspend fun recuperarContrasena(correo: String): ResultadoRecuperacion

    /** Olvida la sesión; los datos de la finca quedan en el dispositivo. */
    suspend fun cerrarSesion()
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
     * El dispositivo ya tiene los datos de otra finca o de otro Usuario: hay que desinstalar la app
     * para empezar limpio (un dispositivo, un Usuario).
     */
    data object OtraCuentaEnElDispositivo : ResultadoInicioSesion

    /** El servidor respondió con un error inesperado. */
    data object NoSePudo : ResultadoInicioSesion
}
