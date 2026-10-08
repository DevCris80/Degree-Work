package com.devcris80.prototipo.domain.model

/** La medición que reporta el ESP32 cada vez que un bovino pasa por la báscula. */
data class Pesaje(
    /** Identificador que genera el ESP32 para cada medición; lo repite si reintenta el envío. */
    val idLectura: String,
    val codigo: String,
    val peso: Float,
    /** Milisegundos entre la medición y su envío; 0 si el ESP32 lo envió al momento. */
    val antiguedadMs: Long = 0,
) {
    companion object {
        /** Ningún bovino pesa más que esto: un valor mayor es un error de la báscula. */
        const val PESO_MAXIMO_KG = 1500f
    }
}
