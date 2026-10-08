package com.devcris80.prototipo.domain.repository

import com.devcris80.prototipo.domain.model.Conciliacion
import com.devcris80.prototipo.domain.model.PesajePendiente
import kotlinx.coroutines.flow.Flow

interface PesajePendienteRepository {
    suspend fun insert(pesajePendiente: PesajePendiente)

    suspend fun findById(idPesajePendiente: String): PesajePendiente?

    suspend fun findByIdLectura(idLectura: String): PesajePendiente?

    /** Pesajes pendientes sin resolver, del más reciente al más antiguo. */
    fun observeSinResolver(): Flow<List<PesajePendiente>>

    /** Pesajes pendientes sin resolver de ese codigo, del más reciente al más antiguo. */
    suspend fun findSinResolverByCodigo(codigo: String): List<PesajePendiente>

    suspend fun conciliar(idPesajePendiente: String, conciliacion: Conciliacion)
}
