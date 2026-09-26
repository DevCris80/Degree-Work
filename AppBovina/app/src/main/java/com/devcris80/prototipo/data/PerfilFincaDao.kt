package com.devcris80.prototipo.data

import androidx.room.Dao
import androidx.room.OnConflictStrategy
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PerfilFincaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardar(perfil: PerfilFinca)

    @Query("SELECT * FROM perfil_finca WHERE id = 'perfil_local' LIMIT 1")
    fun observe(): Flow<PerfilFinca?>

    @Query("SELECT * FROM perfil_finca WHERE id = 'perfil_local' LIMIT 1")
    suspend fun getOnce(): PerfilFinca?
}
