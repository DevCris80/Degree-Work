package com.devcris80.prototipo.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.devcris80.prototipo.data.local.entity.PerfilFincaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PerfilFincaDao {
    // Upsert y no REPLACE: REPLACE borra la fila y el CASCADE se llevaría sus Animales y Usuarios.
    @Upsert
    suspend fun guardar(perfil: PerfilFincaEntity)

    @Query("SELECT * FROM perfil_finca LIMIT 1")
    fun observe(): Flow<PerfilFincaEntity?>

    @Query("SELECT * FROM perfil_finca LIMIT 1")
    suspend fun getOnce(): PerfilFincaEntity?

    // Lo que usa la subida (MotorSincronizacion). Sus escrituras no llevan EDICION_LOCAL.
    @Query("SELECT * FROM perfil_finca WHERE sincronizado = 0")
    suspend fun findPendientes(): List<PerfilFincaEntity>
}
