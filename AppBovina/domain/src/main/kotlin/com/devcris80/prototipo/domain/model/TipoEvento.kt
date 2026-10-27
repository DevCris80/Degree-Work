package com.devcris80.prototipo.domain.model

/**
 * Los tipos de Evento que la app ofrece. [nombre] es lo que se guarda en `Evento.tipoEvento`, que sigue
 * siendo texto: los Eventos anteriores y los que bajan de otro celular pueden traer otro tipo.
 */
enum class TipoEvento(val nombre: String) {
    VACUNACION("Vacunación"),
    PARTO("Parto"),
}
