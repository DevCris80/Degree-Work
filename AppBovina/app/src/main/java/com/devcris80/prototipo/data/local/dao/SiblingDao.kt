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

    @Query("SELECT EXISTS(SELECT 1 FROM sibling WHERE entidad = :entidad AND idFila = :idFila)")
    fun observeEnConflicto(entidad: String, idFila: String): Flow<Boolean>

    @Query("SELECT * FROM sibling WHERE entidad = :entidad AND idFila = :idFila ORDER BY fechaRecepcion ASC")
    fun observeByFila(entidad: String, idFila: String): Flow<List<SiblingEntity>>
}
