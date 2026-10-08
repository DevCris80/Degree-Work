package com.devcris80.prototipo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.devcris80.prototipo.data.local.entity.RegistroEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RegistroDao {
    @Insert
    suspend fun insert(registro: RegistroEntity)

    @Query("SELECT * FROM registro WHERE idAnimal = :idAnimal AND fechaBaja IS NULL ORDER BY timestamp DESC")
    fun observeByAnimal(idAnimal: String): Flow<List<RegistroEntity>>

    @Query("SELECT * FROM registro WHERE idAnimal = :idAnimal AND fechaBaja IS NULL ORDER BY timestamp DESC LIMIT 1")
    fun observeUltimoByAnimal(idAnimal: String): Flow<RegistroEntity?>
}
