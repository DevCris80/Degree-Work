package com.devcris80.prototipo.data.repository

import com.devcris80.prototipo.data.local.dao.ChapetaDao
import com.devcris80.prototipo.domain.model.Chapeta
import com.devcris80.prototipo.domain.repository.ChapetaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomChapetaRepository(private val chapetaDao: ChapetaDao) : ChapetaRepository {
    override suspend fun insert(chapeta: Chapeta) = chapetaDao.insert(chapeta.toEntity())

    override suspend fun findActivaByCodigo(codigo: String): Chapeta? =
        chapetaDao.findActivaByCodigo(codigo)?.toDomain()

    override suspend fun findUltimaByCodigo(codigo: String): Chapeta? =
        chapetaDao.findUltimaByCodigo(codigo)?.toDomain()

    override fun observeActivas(): Flow<List<Chapeta>> =
        chapetaDao.observeActivas().map { chapetas -> chapetas.map { it.toDomain() } }

    override fun observeActivaByAnimal(idAnimal: String): Flow<Chapeta?> =
        chapetaDao.observeActivaByAnimal(idAnimal).map { it?.toDomain() }

    override suspend fun liberar(idChapeta: String, fecha: Long) = chapetaDao.desasociar(idChapeta, fecha)
}
