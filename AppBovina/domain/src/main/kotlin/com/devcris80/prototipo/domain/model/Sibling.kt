package com.devcris80.prototipo.domain.model

/** Las entidades que llevan Revisiones y pueden quedar en Conflicto (ADR-0004). */
enum class EntidadConRevisiones { ANIMAL, EVENTO, REGISTRO }

/** Una edición concurrente sin resolver de una fila: llegó partiendo de una Revisión que ya no era la vigente. */
data class Sibling(
    val idVersion: String,
    val entidad: EntidadConRevisiones,
    val idFila: String,
    val revBase: Int,
    /** La fila completa que subió su autor, en JSON. */
    val contenido: String,
    val idUsuario: String,
    val fechaRecepcion: Long,
)
