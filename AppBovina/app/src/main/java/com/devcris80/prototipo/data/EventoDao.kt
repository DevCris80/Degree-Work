package com.devcris80.prototipo.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EventoDao {
    @Insert
    suspend fun insert(evento: Evento)

    @Query("SELECT * FROM evento WHERE idAnimal = :idAnimal ORDER BY fecha DESC")
    fun observeByAnimal(idAnimal: String): Flow<List<Evento>>
}
