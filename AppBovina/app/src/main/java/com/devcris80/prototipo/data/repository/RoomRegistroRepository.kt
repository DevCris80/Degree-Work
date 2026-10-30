package com.devcris80.prototipo.data.repository

import com.devcris80.prototipo.data.local.dao.RegistroDao
import com.devcris80.prototipo.domain.model.Registro
import com.devcris80.prototipo.domain.repository.RegistroRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class RoomRegistroRepository(
    private val registroDao: RegistroDao,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val nuevoIdVersion: () -> String = { UUID.randomUUID().toString() },
) : RegistroRepository {
    override suspend fun insert(registro: Registro) =
        registroDao.insert(registro.toEntity(fechaModificacion = clock(), idVersion = nuevoIdVersion()))

    override suspend fun findByIdLectura(idLectura: String): Registro? =
        registroDao.findByIdLectura(idLectura)?.toDomain()

    override fun observeByAnimal(idAnimal: String): Flow<List<Registro>> =
        registroDao.observeByAnimal(idAnimal).map { registros -> registros.map { it.toDomain() } }

    override fun observeUltimoByAnimal(idAnimal: String): Flow<Registro?> =
        registroDao.observeUltimoByAnimal(idAnimal).map { it?.toDomain() }

    override suspend fun findById(idRegistro: String): Registro? = registroDao.findById(idRegistro)?.toDomain()

    override suspend fun update(registro: Registro) = registroDao.editar(
        idRegistro = registro.idRegistro,
        peso = registro.peso,
        timestamp = registro.timestamp,
        idVersion = nuevoIdVersion(),
        fechaModificacion = clock(),
    )

    override suspend fun darDeBaja(idRegistro: String, fecha: Long) =
        registroDao.darDeBaja(idRegistro, fecha, idVersion = nuevoIdVersion(), fechaModificacion = clock())
}
