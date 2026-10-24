package com.devcris80.prototipo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.devcris80.prototipo.data.local.entity.SiblingEntity
import kotlinx.coroutines.flow.Flow

/**
 * Un Sibling que esta persona ya resolvió (está en `resolucion`) deja de contar como sin resolver, pero sigue en la
 * tabla hasta que el servidor acepta la edición que lo cierra: si la contesta como sibling o la rechaza, el Conflicto
 * sigue abierto allá y aquí vuelve a verse.
 */
private const val SIN_RESOLVER = "idVersion NOT IN (SELECT idVersionSibling FROM resolucion)"

@Dao
interface SiblingDao {
    // Un Sibling no cambia: si la descarga lo trae otra vez (margen de solapamiento), se ignora.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(sibling: SiblingEntity)

    // Un sibling resuelto, en este celular o en otro, sale de la tabla; el servidor lo conserva.
    @Query("DELETE FROM sibling WHERE idVersion = :idVersion")
    suspend fun delete(idVersion: String)

    @Query("DELETE FROM sibling WHERE idVersion IN (:idsVersion)")
    suspend fun deleteAll(idsVersion: List<String>)

    @Query("SELECT EXISTS(SELECT 1 FROM sibling WHERE entidad = :entidad AND idFila = :idFila AND " + SIN_RESOLVER + ")")
    fun observeEnConflicto(entidad: String, idFila: String): Flow<Boolean>

    @Query("SELECT DISTINCT idFila FROM sibling WHERE entidad = :entidad AND " + SIN_RESOLVER)
    fun observeFilasEnConflicto(entidad: String): Flow<List<String>>

    @Query(
        "SELECT * FROM sibling WHERE entidad = :entidad AND idFila = :idFila AND " + SIN_RESOLVER +
            " ORDER BY fechaRecepcion ASC",
    )
    suspend fun findByFila(entidad: String, idFila: String): List<SiblingEntity>

    @Query(
        "SELECT * FROM sibling WHERE entidad = :entidad AND idFila = :idFila AND " + SIN_RESOLVER +
            " ORDER BY fechaRecepcion ASC",
    )
    fun observeByFila(entidad: String, idFila: String): Flow<List<SiblingEntity>>
}
