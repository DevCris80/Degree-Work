package com.devcris80.prototipo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import com.devcris80.prototipo.data.local.entity.UsuarioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioDao {
    @Insert
    suspend fun insert(usuario: UsuarioEntity)

    /** Para el Usuario que llega del servidor al iniciar sesión, que puede estar ya en el dispositivo. */
    @Upsert
    suspend fun guardar(usuario: UsuarioEntity)

    /** El Usuario del dispositivo. */
    @Query("SELECT * FROM usuario WHERE delDispositivo = 1 LIMIT 1")
    fun observe(): Flow<UsuarioEntity?>

    /** El Usuario del dispositivo. */
    @Query("SELECT * FROM usuario WHERE delDispositivo = 1 LIMIT 1")
    suspend fun getOnce(): UsuarioEntity?

    @Query("SELECT * FROM usuario WHERE idUsuario = :idUsuario")
    suspend fun findById(idUsuario: String): UsuarioEntity?
}
