package com.devcris80.prototipo.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapetaDao {
    @Insert
    suspend fun insert(chapeta: Chapeta)

    @Query("SELECT * FROM chapeta WHERE codigo = :codigo AND fechaDesasociacion IS NULL LIMIT 1")
    suspend fun findActivaByCodigo(codigo: String): Chapeta?

    @Query("SELECT * FROM chapeta WHERE codigo = :codigo ORDER BY fechaAsociacion DESC LIMIT 1")
    suspend fun findUltimaByCodigo(codigo: String): Chapeta?

    @Query("SELECT * FROM chapeta WHERE fechaDesasociacion IS NULL")
    fun observeActivas(): Flow<List<Chapeta>>

    @Query("SELECT * FROM chapeta WHERE idAnimal = :idAnimal AND fechaDesasociacion IS NULL LIMIT 1")
    suspend fun findActivaByAnimal(idAnimal: String): Chapeta?

    @Query("SELECT * FROM chapeta WHERE idAnimal = :idAnimal AND fechaDesasociacion IS NULL LIMIT 1")
    fun observeActivaByAnimal(idAnimal: String): Flow<Chapeta?>

    @Query("UPDATE chapeta SET fechaDesasociacion = :fecha WHERE idChapeta = :idChapeta")
    suspend fun desasociar(idChapeta: String, fecha: Long)
}
