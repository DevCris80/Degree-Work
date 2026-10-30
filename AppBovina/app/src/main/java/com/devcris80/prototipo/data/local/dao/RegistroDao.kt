package com.devcris80.prototipo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.devcris80.prototipo.data.local.entity.RegistroEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RegistroDao {
    @Insert
    suspend fun insert(registro: RegistroEntity)

    @Query("SELECT * FROM registro WHERE idLectura = :idLectura LIMIT 1")
    suspend fun findByIdLectura(idLectura: String): RegistroEntity?

    @Query("SELECT * FROM registro WHERE idAnimal = :idAnimal AND fechaBaja IS NULL ORDER BY timestamp DESC")
    fun observeByAnimal(idAnimal: String): Flow<List<RegistroEntity>>

    @Query("SELECT * FROM registro WHERE idAnimal = :idAnimal AND fechaBaja IS NULL ORDER BY timestamp DESC LIMIT 1")
    fun observeUltimoByAnimal(idAnimal: String): Flow<RegistroEntity?>

    // Lo que usa la subida (MotorSincronizacion). Sus escrituras no llevan EDICION_LOCAL.
    @Query("SELECT * FROM registro WHERE idRegistro = :idRegistro")
    suspend fun findById(idRegistro: String): RegistroEntity?

    /** Los Registros con cambios por subir, del cambio más antiguo al más reciente. */
    @Query("SELECT * FROM registro WHERE sincronizado = 0 ORDER BY fechaModificacion")
    suspend fun findPendientes(): List<RegistroEntity>

    /** Todos los Registros del Animal, también los dados de baja. */
    @Query("SELECT * FROM registro WHERE idAnimal = :idAnimal")
    suspend fun findAllByAnimal(idAnimal: String): List<RegistroEntity>

    // Editar y dar de baja son modificaciones: la fila vuelve a quedar pendiente de sincronizar (ADR-0004).
    // El Animal, el autor y el idLectura no se tocan: el servidor tampoco los deja cambiar.
    @Query("UPDATE registro SET peso = :peso, timestamp = :timestamp, " + EDICION_LOCAL + " WHERE idRegistro = :idRegistro")
    suspend fun editar(idRegistro: String, peso: Float, timestamp: Long, idVersion: String, fechaModificacion: Long)

    @Query("UPDATE registro SET fechaBaja = :fecha, " + EDICION_LOCAL + " WHERE idRegistro = :idRegistro")
    suspend fun darDeBaja(idRegistro: String, fecha: Long, idVersion: String, fechaModificacion: Long)

    @Update
    suspend fun update(registro: RegistroEntity)

    // Ver AnimalDao.rebasar.
    @Query("UPDATE registro SET rev = :rev, revBase = :rev WHERE idRegistro = :idRegistro")
    suspend fun rebasar(idRegistro: String, rev: Int)

    // Solo para un Registro que el servidor rechazó y no tiene (ADR-0004, regla 7).
    @Query("DELETE FROM registro WHERE idRegistro = :idRegistro")
    suspend fun delete(idRegistro: String)
}
