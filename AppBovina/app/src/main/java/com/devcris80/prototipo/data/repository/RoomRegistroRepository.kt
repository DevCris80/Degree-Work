package com.devcris80.prototipo.data.repository

import com.devcris80.prototipo.data.local.dao.RegistroDao
import com.devcris80.prototipo.domain.model.Registro
import com.devcris80.prototipo.domain.repository.RegistroRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomRegistroRepository(private val registroDao: RegistroDao) : RegistroRepository {
    override suspend fun insert(registro: Registro) = registroDao.insert(registro.toEntity())

    override suspend fun findByIdLectura(idLectura: String): Registro? =
        registroDao.findByIdLectura(idLectura)?.toDomain()

    override fun observeByAnimal(idAnimal: String): Flow<List<Registro>> =
        registroDao.observeByAnimal(idAnimal).map { registros -> registros.map { it.toDomain() } }

    override fun observeUltimoByAnimal(idAnimal: String): Flow<Registro?> =
        registroDao.observeUltimoByAnimal(idAnimal).map { it?.toDomain() }
}
