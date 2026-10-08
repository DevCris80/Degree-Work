package com.devcris80.prototipo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.devcris80.prototipo.data.local.entity.UsuarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {
    @Insert
    suspend fun insert(usuario: UsuarioEntity)

    @Query("SELECT * FROM usuario LIMIT 1")
    fun observe(): Flow<UsuarioEntity?>

    @Query("SELECT * FROM usuario LIMIT 1")
    suspend fun getOnce(): UsuarioEntity?
}
