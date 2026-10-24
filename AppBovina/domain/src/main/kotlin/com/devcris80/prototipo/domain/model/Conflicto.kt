package com.devcris80.prototipo.domain.model

/** Una versión completa de una fila que puede estar en Conflicto. */
sealed interface FilaEnConflicto {
    data class DeAnimal(val animal: Animal) : FilaEnConflicto
    data class DeEvento(val evento: Evento) : FilaEnConflicto
    data class DeRegistro(val registro: Registro) : FilaEnConflicto
}

/** Un [Sibling] sin resolver, junto con la fila que subió su autor. */
data class SiblingEnConflicto(val sibling: Sibling, val fila: FilaEnConflicto)

/**
 * Una fila con Siblings sin resolver: su Revisión vigente y cada Sibling, para compararlos campo por campo
 * (RN-17). Los [siblings] van del más antiguo al más reciente.
 */
data class Conflicto(
    val entidad: EntidadConRevisiones,
    val idFila: String,
    val vigente: FilaEnConflicto,
    val siblings: List<SiblingEnConflicto>,
)

/** Lo que una persona elige que quede al resolver un Conflicto. */
sealed interface Eleccion {
    /** Conservar la Revisión vigente y cerrar los Siblings. */
    data object Vigente : Eleccion

    /** Quedarse con la fila de este Sibling. */
    data class DeSibling(val idVersion: String) : Eleccion
}
