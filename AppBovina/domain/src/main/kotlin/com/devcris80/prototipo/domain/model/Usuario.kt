package com.devcris80.prototipo.domain.model

data class Usuario(
    val idUsuario: String,
    val idPerfilFinca: String,
    val nombre: String,
    val rol: Rol,
)

/** Sin Usuario en el dispositivo no hay permiso: ante la duda, no se ofrece ni se hace. */
val Usuario?.puedeAdministrar: Boolean get() = this?.rol?.puedeAdministrar == true
