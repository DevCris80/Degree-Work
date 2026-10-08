package com.devcris80.prototipo.domain.model

/** Por qué un Pesaje no pudo asignarse a un Animal. */
enum class MotivoPesajePendiente { CODIGO_SIN_CHAPETA_ACTIVA, ANIMAL_DADO_DE_BAJA }

/**
 * Un Pesaje válido que no pudo asignarse a un Animal. Se conserva con su motivo hasta que
 * alguien lo concilie (ver docs/adr/0003).
 */
data class PesajePendiente(
    val idPesajePendiente: String,
    val idLectura: String,
    val codigo: String,
    val peso: Float,
    val timestamp: Long,
    val idUsuario: String,
    val motivo: MotivoPesajePendiente,
    val resuelto: Boolean = false,
)
