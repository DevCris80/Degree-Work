package com.devcris80.prototipo.domain.repository

import com.devcris80.prototipo.domain.model.Conflicto
import com.devcris80.prototipo.domain.model.EntidadConRevisiones
import com.devcris80.prototipo.domain.model.Sibling
import kotlinx.coroutines.flow.Flow

interface ConflictoRepository {
    /** Si la fila tiene algún Sibling sin resolver. */
    fun observeEnConflicto(entidad: EntidadConRevisiones, idFila: String): Flow<Boolean>

    /** Los ids de las filas de esa entidad que tienen algún Sibling sin resolver. */
    fun observeFilasEnConflicto(entidad: EntidadConRevisiones): Flow<Set<String>>

    /** Los Siblings sin resolver de la fila, del más antiguo al más reciente. */
    fun observeSiblings(entidad: EntidadConRevisiones, idFila: String): Flow<List<Sibling>>

    /** La Revisión vigente de la fila junto con sus Siblings sin resolver; nulo si no tiene ninguno. */
    fun observeConflicto(entidad: EntidadConRevisiones, idFila: String): Flow<Conflicto?>

    /**
     * Resuelve [idsResueltos] con una edición nueva sobre la Revisión vigente: la fila de [idVersionElegida], o la
     * vigente si es nulo. La edición queda pendiente de subir y nombra a esos Siblings. Los Siblings que no estén en
     * [idsResueltos] siguen sin resolver.
     */
    suspend fun resolver(
        entidad: EntidadConRevisiones,
        idFila: String,
        idVersionElegida: String?,
        idsResueltos: List<String>,
    )
}
