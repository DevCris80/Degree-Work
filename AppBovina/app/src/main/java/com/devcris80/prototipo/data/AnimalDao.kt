package com.devcris80.prototipo.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimalDao {
    @Insert
    suspend fun insert(animal: Animal)

    @Query("SELECT * FROM animal WHERE fechaBaja IS NULL ORDER BY nombre ASC")
    fun observeActivos(): Flow<List<Animal>>

    @Query("SELECT * FROM animal ORDER BY nombre ASC")
    suspend fun getAllOnce(): List<Animal>

    @Query("SELECT * FROM animal WHERE idAnimal = :idAnimal")
    fun observeById(idAnimal: String): Flow<Animal?>

    @Query("UPDATE animal SET fechaBaja = :fecha WHERE idAnimal = :idAnimal")
    suspend fun darDeBaja(idAnimal: String, fecha: Long)
}
