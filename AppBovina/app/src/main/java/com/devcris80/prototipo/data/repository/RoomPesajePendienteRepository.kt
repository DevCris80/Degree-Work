package com.devcris80.prototipo.data.repository

import com.devcris80.prototipo.data.local.dao.PesajePendienteDao
import com.devcris80.prototipo.domain.model.Conciliacion
import com.devcris80.prototipo.domain.model.PesajePendiente
import com.devcris80.prototipo.domain.repository.PesajePendienteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomPesajePendienteRepository(
    private val pesajePendienteDao: PesajePendienteDao,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : PesajePendienteRepository {
    override suspend fun insert(pesajePendiente: PesajePendiente) =
        pesajePendienteDao.insert(pesajePendiente.toEntity(fechaModificacion = clock()))

    override suspend fun findById(idPesajePendiente: String): PesajePendiente? =
        pesajePendienteDao.findById(idPesajePendiente)?.toDomain()

    override suspend fun findByIdLectura(idLectura: String): PesajePendiente? =
        pesajePendienteDao.findByIdLectura(idLectura)?.toDomain()

    override fun observeSinResolver(): Flow<List<PesajePendiente>> =
        pesajePendienteDao.observeSinResolver().map { pendientes -> pendientes.map { it.toDomain() } }

    override suspend fun findSinResolverByCodigo(codigo: String): List<PesajePendiente> =
        pesajePendienteDao.findSinResolverByCodigo(codigo).map { it.toDomain() }

    override suspend fun conciliar(idPesajePendiente: String, conciliacion: Conciliacion) =
        pesajePendienteDao.conciliar(
            idPesajePendiente = idPesajePendiente,
            resolucion = conciliacion.resolucion.textoGuardado(),
            idUsuario = conciliacion.idUsuario,
            fecha = conciliacion.fecha,
            fechaModificacion = clock(),
        )
}
