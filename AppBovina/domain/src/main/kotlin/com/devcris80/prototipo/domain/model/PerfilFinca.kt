package com.devcris80.prototipo.domain.model

data class PerfilFinca(
    val idPerfilFinca: String,
    val nombreFinca: String,
    val latitud: Double? = null,
    val longitud: Double? = null,
)
