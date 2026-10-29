package com.devcris80.prototipo.domain.model

/** Lo que falta para poder crear una cuenta (CU-01, flujos 2a y 3a; CU-04). */
enum class FaltaParaCrear { DATOS, CONTRASENA_CORTA, CONFIRMACION_DISTINTA, AUTORIZACION }

/**
 * Los datos del formulario Crear cuenta, tal como los escribió la persona. Con [conCodigo] la persona se une a una
 * finca con un código de invitación (CU-04) y no escribe el nombre de una finca; sin él, crea la suya (CU-01).
 */
data class CuentaNueva(
    val nombre: String,
    val nombreFinca: String,
    val correo: String,
    val contrasena: String,
    val confirmacion: String,
    val aceptaAutorizacion: Boolean,
    val conCodigo: Boolean = false,
    val codigoInvitacion: String = "",
) {
    /** Lo primero que falta para crear la cuenta, en el orden del formulario; nulo si ya se puede crear. */
    val faltaParaCrear: FaltaParaCrear?
        get() = when {
            nombre.isBlank() || correo.isBlank() || (if (conCodigo) codigoInvitacion else nombreFinca).isBlank() ->
                FaltaParaCrear.DATOS
            contrasena.length < CONTRASENA_MINIMA -> FaltaParaCrear.CONTRASENA_CORTA
            contrasena != confirmacion -> FaltaParaCrear.CONFIRMACION_DISTINTA
            !aceptaAutorizacion -> FaltaParaCrear.AUTORIZACION
            else -> null
        }

    companion object {
        const val CONTRASENA_MINIMA = 6
    }
}
