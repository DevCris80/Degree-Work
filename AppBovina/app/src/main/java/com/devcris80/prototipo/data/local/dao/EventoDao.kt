package com.devcris80.prototipo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.devcris80.prototipo.data.local.entity.EventoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventoDao {
    @Insert
    suspend fun insert(evento: EventoEntity)

    @Query("SELECT * FROM evento WHERE idAnimal = :idAnimal AND fechaBaja IS NULL ORDER BY fecha DESC")
    fun observeByAnimal(idAnimal: String): Flow<List<EventoEntity>>
}
