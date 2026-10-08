package com.devcris80.prototipo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.devcris80.prototipo.data.local.entity.PesajePendienteEntity

@Dao
interface PesajePendienteDao {
    @Insert
    suspend fun insert(pesajePendiente: PesajePendienteEntity)

    @Query("SELECT * FROM pesaje_pendiente WHERE idLectura = :idLectura LIMIT 1")
    suspend fun findByIdLectura(idLectura: String): PesajePendienteEntity?
}
