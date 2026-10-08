package com.devcris80.prototipo.data.repository

import com.devcris80.prototipo.data.local.dao.PesajePendienteDao
import com.devcris80.prototipo.domain.model.PesajePendiente
import com.devcris80.prototipo.domain.repository.PesajePendienteRepository

class RoomPesajePendienteRepository(
    private val pesajePendienteDao: PesajePendienteDao,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : PesajePendienteRepository {
    override suspend fun insert(pesajePendiente: PesajePendiente) =
        pesajePendienteDao.insert(pesajePendiente.toEntity(fechaModificacion = clock()))

    override suspend fun findByIdLectura(idLectura: String): PesajePendiente? =
        pesajePendienteDao.findByIdLectura(idLectura)?.toDomain()
}
