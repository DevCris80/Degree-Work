package com.devcris80.prototipo.domain.model

/** Por qué un Pesaje no pudo asignarse a un Animal. */
enum class MotivoPesajePendiente { CODIGO_SIN_CHAPETA_ACTIVA, ANIMAL_DADO_DE_BAJA }

/** En qué terminó un Pesaje pendiente al conciliarlo. */
enum class ResolucionPesajePendiente { ASIGNADO, DESCARTADO }

/** Cómo se concilió un Pesaje pendiente, quién lo hizo y cuándo. */
data class Conciliacion(
    val resolucion: ResolucionPesajePendiente,
    val idUsuario: String,
    val fecha: Long,
)

/**
 * Un Pesaje válido que no pudo asignarse a un Animal. Está sin resolver hasta que alguien lo
 * concilia; resuelto, se conserva igual (ver docs/adr/0003).
 */
data class PesajePendiente(
    val idPesajePendiente: String,
    val idLectura: String,
    val codigo: String,
    val peso: Float,
    val timestamp: Long,
    val idUsuario: String,
    /** El motivo con el que llegó; no cambia aunque después cambie la Chapeta de su codigo. */
    val motivo: MotivoPesajePendiente,
    /** Nula mientras está sin resolver. */
    val conciliacion: Conciliacion? = null,
)
