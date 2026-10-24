package com.devcris80.prototipo.data.repository

import com.devcris80.prototipo.data.local.dao.EventoDao
import com.devcris80.prototipo.domain.model.Evento
import com.devcris80.prototipo.domain.repository.EventoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class RoomEventoRepository(
    private val eventoDao: EventoDao,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val nuevoIdVersion: () -> String = { UUID.randomUUID().toString() },
) : EventoRepository {
    override suspend fun insert(evento: Evento) =
        eventoDao.insert(evento.toEntity(fechaModificacion = clock(), idVersion = nuevoIdVersion()))

    override fun observeByAnimal(idAnimal: String): Flow<List<Evento>> =
        eventoDao.observeByAnimal(idAnimal).map { eventos -> eventos.map { it.toDomain() } }

    override fun observeActivos(): Flow<List<Evento>> =
        eventoDao.observeActivos().map { eventos -> eventos.map { it.toDomain() } }
}
