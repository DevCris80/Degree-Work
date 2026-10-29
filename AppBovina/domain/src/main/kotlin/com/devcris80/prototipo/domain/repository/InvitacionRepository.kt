package com.devcris80.prototipo.domain.repository

/** Un código de invitación a la finca (CU-04): de un solo uso, vence a los 7 días. */
data class Invitacion(val codigo: String, val venceEn: Long)

/**
 * Las invitaciones a la finca viven solo en el servidor (RN-18): generar una necesita señal y el rol Ganadero.
 * El Operario las canjea al crear su cuenta, con [SesionRepository.unirseAFinca].
 */
interface InvitacionRepository {
    suspend fun generar(): ResultadoInvitacion
}

sealed interface ResultadoInvitacion {
    data class Generada(val invitacion: Invitacion) : ResultadoInvitacion
    data object SinConexion : ResultadoInvitacion

    /** El servidor no deja generar códigos a este Usuario: no es Ganadero o la sesión ya no vale. */
    data object NoPermitido : ResultadoInvitacion

    /** El servidor respondió con un error inesperado. */
    data object NoSePudo : ResultadoInvitacion
}
