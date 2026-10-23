package com.devcris80.prototipo.data.repository

import com.devcris80.prototipo.data.local.dao.SiblingDao
import com.devcris80.prototipo.domain.model.EntidadConRevisiones
import com.devcris80.prototipo.domain.model.Sibling
import com.devcris80.prototipo.domain.repository.ConflictoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomConflictoRepository(private val siblingDao: SiblingDao) : ConflictoRepository {
    override fun observeEnConflicto(entidad: EntidadConRevisiones, idFila: String): Flow<Boolean> =
        siblingDao.observeEnConflicto(entidad.textoGuardado(), idFila)

    override fun observeSiblings(entidad: EntidadConRevisiones, idFila: String): Flow<List<Sibling>> =
        siblingDao.observeByFila(entidad.textoGuardado(), idFila).map { siblings -> siblings.map { it.toDomain() } }
}
