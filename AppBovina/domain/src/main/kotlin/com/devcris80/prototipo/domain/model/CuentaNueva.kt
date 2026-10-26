package com.devcris80.prototipo.domain.model

/** Lo que falta para poder crear una cuenta (CU-01, flujos 2a y 3a). */
enum class FaltaParaCrear { DATOS, CONTRASENA_CORTA, CONFIRMACION_DISTINTA, AUTORIZACION }

/** Los datos del formulario Crear cuenta, tal como los escribió la persona. */
data class CuentaNueva(
    val nombre: String,
    val nombreFinca: String,
    val correo: String,
    val contrasena: String,
    val confirmacion: String,
    val aceptaAutorizacion: Boolean,
) {
    /** Lo primero que falta para crear la cuenta, en el orden del formulario; nulo si ya se puede crear. */
    val faltaParaCrear: FaltaParaCrear?
        get() = when {
            nombre.isBlank() || nombreFinca.isBlank() || correo.isBlank() -> FaltaParaCrear.DATOS
            contrasena.length < CONTRASENA_MINIMA -> FaltaParaCrear.CONTRASENA_CORTA
            contrasena != confirmacion -> FaltaParaCrear.CONFIRMACION_DISTINTA
            !aceptaAutorizacion -> FaltaParaCrear.AUTORIZACION
            else -> null
        }

    companion object {
        const val CONTRASENA_MINIMA = 6
    }
}
