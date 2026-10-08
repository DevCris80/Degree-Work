package com.devcris80.prototipo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.devcris80.prototipo.data.local.entity.ChapetaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapetaDao {
    @Insert
    suspend fun insert(chapeta: ChapetaEntity)

    @Query("SELECT * FROM chapeta WHERE codigo = :codigo AND fechaDesasociacion IS NULL LIMIT 1")
    suspend fun findActivaByCodigo(codigo: String): ChapetaEntity?

    @Query("SELECT * FROM chapeta WHERE codigo = :codigo ORDER BY fechaAsociacion DESC LIMIT 1")
    suspend fun findUltimaByCodigo(codigo: String): ChapetaEntity?

    @Query("SELECT * FROM chapeta WHERE fechaDesasociacion IS NULL")
    fun observeActivas(): Flow<List<ChapetaEntity>>

    @Query("SELECT * FROM chapeta WHERE idAnimal = :idAnimal AND fechaDesasociacion IS NULL LIMIT 1")
    suspend fun findActivaByAnimal(idAnimal: String): ChapetaEntity?

    @Query("SELECT * FROM chapeta WHERE idAnimal = :idAnimal AND fechaDesasociacion IS NULL LIMIT 1")
    fun observeActivaByAnimal(idAnimal: String): Flow<ChapetaEntity?>

    // Liberar es una modificación: la fila vuelve a quedar pendiente de sincronizar.
    @Query(
        "UPDATE chapeta SET fechaDesasociacion = :fecha, sincronizado = 0, fechaModificacion = :fecha " +
            "WHERE idChapeta = :idChapeta",
    )
    suspend fun desasociar(idChapeta: String, fecha: Long)
}
