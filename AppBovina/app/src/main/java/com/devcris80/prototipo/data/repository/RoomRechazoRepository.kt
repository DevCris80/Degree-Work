package com.devcris80.prototipo.data.repository

import com.devcris80.prototipo.data.local.dao.RechazoDao
import com.devcris80.prototipo.domain.model.Rechazo
import com.devcris80.prototipo.domain.repository.RechazoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomRechazoRepository(
    private val rechazoDao: RechazoDao,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : RechazoRepository {
    override fun observeAbiertos(): Flow<List<Rechazo>> =
        rechazoDao.observeAbiertos().map { rechazos -> rechazos.map { it.toDomain() } }

    override suspend fun cerrar(idRechazo: String) = rechazoDao.cerrar(idRechazo, clock())
}
