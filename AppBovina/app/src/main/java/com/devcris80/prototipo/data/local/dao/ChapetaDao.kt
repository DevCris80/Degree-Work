package com.devcris80.prototipo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
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
        "UPDATE chapeta SET fechaDesasociacion = :fecha, sincronizado = 0, " +
            "fechaModificacion = :fechaModificacion WHERE idChapeta = :idChapeta",
    )
    suspend fun desasociar(idChapeta: String, fecha: Long, fechaModificacion: Long)

    // Lo que usa la subida (MotorSincronizacion). Sus escrituras no llevan EDICION_LOCAL.
    @Query("SELECT * FROM chapeta WHERE idChapeta = :idChapeta")
    suspend fun findById(idChapeta: String): ChapetaEntity?

    /** Las Chapetas con cambios por subir, del cambio más antiguo al más reciente. */
    @Query("SELECT * FROM chapeta WHERE sincronizado = 0 ORDER BY fechaModificacion")
    suspend fun findPendientes(): List<ChapetaEntity>

    /** Todas las Chapetas del Animal, también las liberadas. */
    @Query("SELECT * FROM chapeta WHERE idAnimal = :idAnimal")
    suspend fun findAllByAnimal(idAnimal: String): List<ChapetaEntity>

    @Update
    suspend fun update(chapeta: ChapetaEntity)

    // Solo para una Chapeta que el servidor rechazó y no tiene (ADR-0004, regla 7).
    @Query("DELETE FROM chapeta WHERE idChapeta = :idChapeta")
    suspend fun delete(idChapeta: String)
}
