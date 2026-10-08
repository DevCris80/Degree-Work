package com.devcris80.prototipo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.devcris80.prototipo.data.local.entity.PerfilFincaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PerfilFincaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(perfil: PerfilFincaEntity)

    @Query("SELECT * FROM perfil_finca WHERE id = 'perfil_local' LIMIT 1")
    fun observe(): Flow<PerfilFincaEntity?>

    @Query("SELECT * FROM perfil_finca WHERE id = 'perfil_local' LIMIT 1")
    suspend fun getOnce(): PerfilFincaEntity?
}
