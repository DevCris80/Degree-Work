package com.devcris80.prototipo.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.devcris80.prototipo.data.local.entity.EstadoSyncEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EstadoSyncDao {
    @Upsert
    suspend fun guardar(estado: EstadoSyncEntity)

    /** Nulo hasta la primera descarga. */
    @Query("SELECT * FROM estado_sync WHERE id = 0")
    fun observe(): Flow<EstadoSyncEntity?>
}
