package com.devcris80.prototipo.domain.repository

import com.devcris80.prototipo.domain.model.EntidadConRevisiones
import com.devcris80.prototipo.domain.model.Sibling
import kotlinx.coroutines.flow.Flow

interface ConflictoRepository {
    /** Si la fila tiene algún Sibling sin resolver. */
    fun observeEnConflicto(entidad: EntidadConRevisiones, idFila: String): Flow<Boolean>

    /** Los Siblings sin resolver de la fila, del más antiguo al más reciente. */
    fun observeSiblings(entidad: EntidadConRevisiones, idFila: String): Flow<List<Sibling>>
}
