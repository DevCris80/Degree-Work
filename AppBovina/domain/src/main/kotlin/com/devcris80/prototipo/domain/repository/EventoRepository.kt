package com.devcris80.prototipo.domain.repository

import com.devcris80.prototipo.domain.model.Evento
import kotlinx.coroutines.flow.Flow

interface EventoRepository {
    suspend fun insert(evento: Evento)

    /** Eventos del Animal que no están dados de baja, del más reciente al más antiguo. */
    fun observeByAnimal(idAnimal: String): Flow<List<Evento>>
}
