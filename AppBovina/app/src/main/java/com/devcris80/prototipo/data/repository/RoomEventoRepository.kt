package com.devcris80.prototipo.data.repository

import com.devcris80.prototipo.data.local.dao.EventoDao
import com.devcris80.prototipo.domain.model.Evento
import com.devcris80.prototipo.domain.repository.EventoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomEventoRepository(
    private val eventoDao: EventoDao,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : EventoRepository {
    override suspend fun insert(evento: Evento) = eventoDao.insert(evento.toEntity(fechaModificacion = clock()))

    override fun observeByAnimal(idAnimal: String): Flow<List<Evento>> =
        eventoDao.observeByAnimal(idAnimal).map { eventos -> eventos.map { it.toDomain() } }
}
