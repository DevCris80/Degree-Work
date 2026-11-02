package com.devcris80.prototipo.domain.model

/**
 * Un Usuario con la sesión cerrada cuyos datos siguen en el celular (ADR-0006). [correo] es nulo en la cuenta que
 * ya estaba en el celular antes de poder guardar varias, de la que no se sabe el correo.
 */
data class CuentaGuardada(
    val idUsuario: String,
    val correo: String?,
    val nombreUsuario: String,
    val rol: Rol,
    val nombreFinca: String,
    val cambiosSinSubir: Int,
    val fotosSinSubir: Int,
    val rechazosAbiertos: Int,
) {
    /** Lo que se perdería al quitar la cuenta: cambios, fotos y cambios rechazados (RN-27). */
    val tienePendientes: Boolean get() = cambiosSinSubir + fotosSinSubir + rechazosAbiertos > 0
}
