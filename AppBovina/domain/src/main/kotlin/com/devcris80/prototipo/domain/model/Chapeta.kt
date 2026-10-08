package com.devcris80.prototipo.domain.model

data class Chapeta(
    val idChapeta: String,
    val codigo: String,
    val idAnimal: String,
    val fechaAsociacion: Long,
    val fechaDesasociacion: Long? = null,
)
