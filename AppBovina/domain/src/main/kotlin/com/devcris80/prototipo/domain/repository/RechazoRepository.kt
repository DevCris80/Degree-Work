package com.devcris80.prototipo.domain.repository

import com.devcris80.prototipo.domain.model.Rechazo
import kotlinx.coroutines.flow.Flow

interface RechazoRepository {
    /** Los Rechazos sin corregir, del más reciente al más antiguo. */
    fun observeAbiertos(): Flow<List<Rechazo>>

    /**
     * Cierra el Rechazo: sale de la bandeja y se conserva (RN-24). Su cambio no se reenvía; una corrección es una
     * escritura nueva.
     */
    suspend fun cerrar(idRechazo: String)
}
