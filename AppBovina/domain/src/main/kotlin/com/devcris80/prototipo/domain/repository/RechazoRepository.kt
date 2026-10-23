package com.devcris80.prototipo.domain.repository

import com.devcris80.prototipo.domain.model.Rechazo
import kotlinx.coroutines.flow.Flow

interface RechazoRepository {
    /** Los Rechazos sin corregir, del más reciente al más antiguo. */
    fun observeAbiertos(): Flow<List<Rechazo>>
}
