package com.devcris80.prototipo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.devcris80.prototipo.data.local.entity.SiblingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SiblingDao {
    // Un Sibling no cambia: si la descarga lo trae otra vez (margen de solapamiento), se ignora.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(sibling: SiblingEntity)

    // Un sibling resuelto, en este celular o en otro, sale de la tabla; el servidor lo conserva.
    @Query("DELETE FROM sibling WHERE idVersion = :idVersion")
    suspend fun delete(idVersion: String)

    @Query("SELECT EXISTS(SELECT 1 FROM sibling WHERE entidad = :entidad AND idFila = :idFila)")
    fun observeEnConflicto(entidad: String, idFila: String): Flow<Boolean>

    @Query("SELECT DISTINCT idFila FROM sibling WHERE entidad = :entidad")
    fun observeFilasEnConflicto(entidad: String): Flow<List<String>>

    // Resolverlos los saca de esta tabla; la resolución queda en `resolucion` hasta que sube.
    @Query("DELETE FROM sibling WHERE idVersion IN (:idsVersion)")
    suspend fun deleteAll(idsVersion: List<String>)

    @Query("SELECT * FROM sibling WHERE entidad = :entidad AND idFila = :idFila ORDER BY fechaRecepcion ASC")
    suspend fun findByFila(entidad: String, idFila: String): List<SiblingEntity>

    @Query("SELECT * FROM sibling WHERE entidad = :entidad AND idFila = :idFila ORDER BY fechaRecepcion ASC")
    fun observeByFila(entidad: String, idFila: String): Flow<List<SiblingEntity>>
}
