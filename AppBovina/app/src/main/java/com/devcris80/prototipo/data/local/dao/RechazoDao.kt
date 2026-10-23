package com.devcris80.prototipo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.devcris80.prototipo.data.local.entity.RechazoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RechazoDao {
    @Insert
    suspend fun insert(rechazo: RechazoEntity)

    @Query("SELECT * FROM rechazo WHERE fechaCorreccion IS NULL ORDER BY fechaRechazo DESC")
    fun observeAbiertos(): Flow<List<RechazoEntity>>
}
