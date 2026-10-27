package com.devcris80.prototipo.domain.model

import java.text.Normalizer
import java.util.Locale

data class Evento(
    val idEvento: String,
    val idAnimal: String,
    /** El Usuario que registró el Evento. */
    val idUsuario: String,
    val tipoEvento: String,
    val fecha: Long,
    val detalle: String,
    val fechaBaja: Long? = null,
    /** Vacunación: el biológico aplicado. */
    val biologico: String? = null,
    /** Vacunación: la dosis aplicada, como la escribió la persona. */
    val dosis: String? = null,
    /** Vacunación: la vía de aplicación. */
    val via: String? = null,
    /** Parto: la cría que nació. */
    val idCriaResultante: String? = null,
) {
    /** Su fecha todavía no llega: es un Evento programado. */
    fun estaProgramado(ahora: Long): Boolean = fecha > ahora

    /** Biológico, dosis y vía de una vacunación, los que tenga; nulo si no tiene ninguno. */
    val resumenDeVacunacion: String?
        get() = listOfNotNull(biologico, dosis, via).joinToString(" · ").ifEmpty { null }

    /** [tipoEvento] escrito sin espacios de más, como lo digitó la persona. */
    val tipoEscrito: String get() = tipoEvento.trim().replace(ESPACIOS, " ")

    /**
     * El tipo sin lo que no lo cambia: mayúsculas, tildes y espacios. [tipoEvento] es texto libre,
     * así que "Vacunación" y " vacunacion" son el mismo tipo.
     */
    val claveDeTipo: String
        get() = Normalizer.normalize(tipoEscrito, Normalizer.Form.NFD).replace(TILDES, "").lowercase(Locale.ROOT)

    private companion object {
        val ESPACIOS = Regex("\\s+")
        val TILDES = Regex("\\p{M}")
    }
}
