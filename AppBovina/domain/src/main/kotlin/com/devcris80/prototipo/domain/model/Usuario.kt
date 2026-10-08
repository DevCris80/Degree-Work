package com.devcris80.prototipo.domain.model

data class Usuario(
    val idUsuario: String,
    val idPerfilFinca: String,
    val nombre: String,
    val rol: Rol,
)
