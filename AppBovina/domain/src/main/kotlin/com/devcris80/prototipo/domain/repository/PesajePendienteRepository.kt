package com.devcris80.prototipo.domain.repository

import com.devcris80.prototipo.domain.model.PesajePendiente

interface PesajePendienteRepository {
    suspend fun insert(pesajePendiente: PesajePendiente)

    suspend fun findByIdLectura(idLectura: String): PesajePendiente?
}
