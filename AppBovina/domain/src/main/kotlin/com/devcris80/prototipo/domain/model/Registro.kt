package com.devcris80.prototipo.domain.model

data class Registro(
    val idRegistro: String,
    val idAnimal: String,
    val idUsuario: String,
    val peso: Float,
    val timestamp: Long,
    val fechaBaja: Long? = null,
)
