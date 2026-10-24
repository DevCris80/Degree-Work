package com.devcris80.prototipo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.devcris80.prototipo.data.local.entity.ResolucionEntity

@Dao
interface ResolucionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(resoluciones: List<ResolucionEntity>)

    /** Los Siblings que la edición pendiente de la fila resuelve, para nombrarlos en la subida. */
    @Query("SELECT idVersionSibling FROM resolucion WHERE entidad = :entidad AND idFila = :idFila ORDER BY idVersionSibling")
    suspend fun idsDeLaFila(entidad: String, idFila: String): List<String>

    @Query("DELETE FROM resolucion WHERE idVersionSibling IN (:idsVersion)")
    suspend fun delete(idsVersion: List<String>)
}
