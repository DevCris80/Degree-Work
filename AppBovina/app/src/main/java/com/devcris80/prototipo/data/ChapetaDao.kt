package com.devcris80.prototipo.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapetaDao {
    @Insert
    suspend fun insert(chapeta: Chapeta)

    @Query("SELECT * FROM chapeta WHERE idChip = :idChip LIMIT 1")
    suspend fun findByIdChip(idChip: String): Chapeta?

    @Query("SELECT * FROM chapeta WHERE fechaDesasociacion IS NULL")
    fun observeActivas(): Flow<List<Chapeta>>

    @Query("SELECT * FROM chapeta WHERE idAnimal = :idAnimal AND fechaDesasociacion IS NULL LIMIT 1")
    suspend fun findActivaByAnimal(idAnimal: String): Chapeta?

    @Query("UPDATE chapeta SET fechaDesasociacion = :fecha WHERE idChip = :idChip")
    suspend fun desasociar(idChip: String, fecha: Long)
}
