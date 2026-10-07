package com.devcris80.prototipo.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RegistroDao {
    @Insert
    suspend fun insert(registro: Registro)

    @Query("SELECT * FROM registro WHERE idAnimal = :idAnimal AND fechaBaja IS NULL ORDER BY timestamp DESC")
    fun observeByAnimal(idAnimal: String): Flow<List<Registro>>

    @Query("SELECT * FROM registro WHERE idAnimal = :idAnimal AND fechaBaja IS NULL ORDER BY timestamp DESC LIMIT 1")
    fun observeUltimoByAnimal(idAnimal: String): Flow<Registro?>
}
