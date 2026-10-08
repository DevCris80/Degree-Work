package com.devcris80.prototipo.domain.model

/** La medición que reporta el ESP32 cada vez que un bovino pasa por la báscula. */
data class Pesaje(
    /** Identificador que genera el ESP32 para cada medición; lo repite si reintenta el envío. */
    val idLectura: String,
    val codigo: String,
    val peso: Float,
)
