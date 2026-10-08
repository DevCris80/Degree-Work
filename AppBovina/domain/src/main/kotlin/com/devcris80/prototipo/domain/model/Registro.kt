package com.devcris80.prototipo.domain.model

data class Registro(
    val idRegistro: String,
    val idAnimal: String,
    val idUsuario: String,
    /** El idLectura del Pesaje que lo originó; nulo si el peso no vino del ESP32. */
    val idLectura: String? = null,
    val peso: Float,
    val timestamp: Long,
    val fechaBaja: Long? = null,
)
