package com.devcris80.prototipo.domain.repository

import com.devcris80.prototipo.domain.model.Chapeta
import kotlinx.coroutines.flow.Flow

interface ChapetaRepository {
    suspend fun insert(chapeta: Chapeta)

    suspend fun findActivaByCodigo(codigo: String): Chapeta?

    /** La Chapeta más reciente de ese codigo, esté activa o no. */
    suspend fun findUltimaByCodigo(codigo: String): Chapeta?

    fun observeActivas(): Flow<List<Chapeta>>

    fun observeActivaByAnimal(idAnimal: String): Flow<Chapeta?>

    suspend fun liberar(idChapeta: String, fecha: Long)
}
