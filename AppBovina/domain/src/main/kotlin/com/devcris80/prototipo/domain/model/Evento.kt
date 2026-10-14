package com.devcris80.prototipo.domain.model

data class Evento(
    val idEvento: String,
    val idAnimal: String,
    /** El Usuario que registró el Evento. */
    val idUsuario: String,
    val tipoEvento: String,
    val fecha: Long,
    val detalle: String,
    val fechaBaja: Long? = null,
)
