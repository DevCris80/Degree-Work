package com.devcris80.prototipo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.devcris80.prototipo.data.local.entity.AnimalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimalDao {
    @Insert
    suspend fun insert(animal: AnimalEntity)

    @Query("SELECT * FROM animal WHERE fechaBaja IS NULL ORDER BY nombre ASC")
    fun observeActivos(): Flow<List<AnimalEntity>>

    @Query("SELECT * FROM animal ORDER BY nombre ASC")
    suspend fun getAllOnce(): List<AnimalEntity>

    @Query("SELECT * FROM animal WHERE idAnimal = :idAnimal")
    fun observeById(idAnimal: String): Flow<AnimalEntity?>

    @Query("SELECT * FROM animal WHERE idAnimal = :idAnimal")
    suspend fun findById(idAnimal: String): AnimalEntity?

    @Query("UPDATE animal SET fechaBaja = :fecha WHERE idAnimal = :idAnimal")
    suspend fun darDeBaja(idAnimal: String, fecha: Long)
}
