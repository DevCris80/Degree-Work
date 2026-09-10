package com.devcris80.prototipo.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimalDao {
    @Insert
    suspend fun insert(animal: Animal)

    @Query("SELECT * FROM animal ORDER BY nombre ASC")
    fun observeAll(): Flow<List<Animal>>

    @Query("SELECT * FROM animal ORDER BY nombre ASC")
    suspend fun getAllOnce(): List<Animal>

    @Query("SELECT * FROM animal WHERE idAnimal = :idAnimal")
    fun observeById(idAnimal: String): Flow<Animal?>
}
