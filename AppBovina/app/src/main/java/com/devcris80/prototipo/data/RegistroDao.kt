package com.devcris80.prototipo.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RegistroDao {
    @Insert
    suspend fun insert(registro: Registro)

    @Query("SELECT * FROM registro WHERE idAnimal = :idAnimal ORDER BY timestamp DESC")
    fun observeByAnimal(idAnimal: String): Flow<List<Registro>>
}
