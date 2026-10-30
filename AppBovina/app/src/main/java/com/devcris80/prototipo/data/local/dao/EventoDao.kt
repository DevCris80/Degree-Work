package com.devcris80.prototipo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.devcris80.prototipo.data.local.entity.EventoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventoDao {
    @Insert
    suspend fun insert(evento: EventoEntity)

    @Query("SELECT * FROM evento WHERE idAnimal = :idAnimal AND fechaBaja IS NULL ORDER BY fecha DESC")
    fun observeByAnimal(idAnimal: String): Flow<List<EventoEntity>>

    @Query("SELECT * FROM evento WHERE fechaBaja IS NULL ORDER BY fecha DESC")
    fun observeActivos(): Flow<List<EventoEntity>>

    // Lo que usa la subida (MotorSincronizacion). Sus escrituras no llevan EDICION_LOCAL.
    @Query("SELECT * FROM evento WHERE idEvento = :idEvento")
    suspend fun findById(idEvento: String): EventoEntity?

    /** Los Eventos con cambios por subir, del cambio más antiguo al más reciente. */
    @Query("SELECT * FROM evento WHERE sincronizado = 0 ORDER BY fechaModificacion")
    suspend fun findPendientes(): List<EventoEntity>

    /** Todos los Eventos del Animal, también los dados de baja. */
    @Query("SELECT * FROM evento WHERE idAnimal = :idAnimal")
    suspend fun findAllByAnimal(idAnimal: String): List<EventoEntity>

    @Update
    suspend fun update(evento: EventoEntity)

    // Editar y dar de baja son modificaciones: la fila vuelve a quedar pendiente de sincronizar (ADR-0004).
    // El Animal, el autor, el tipo y la cría no se tocan.
    @Query(
        "UPDATE evento SET detalle = :detalle, fecha = :fecha, biologico = :biologico, dosis = :dosis, via = :via, " +
            EDICION_LOCAL + " WHERE idEvento = :idEvento",
    )
    suspend fun editar(
        idEvento: String,
        detalle: String,
        fecha: Long,
        biologico: String?,
        dosis: String?,
        via: String?,
        idVersion: String,
        fechaModificacion: Long,
    )

    @Query("UPDATE evento SET fechaBaja = :fecha, " + EDICION_LOCAL + " WHERE idEvento = :idEvento")
    suspend fun darDeBaja(idEvento: String, fecha: Long, idVersion: String, fechaModificacion: Long)

    // Ver AnimalDao.rebasar.
    @Query("UPDATE evento SET rev = :rev, revBase = :rev WHERE idEvento = :idEvento")
    suspend fun rebasar(idEvento: String, rev: Int)

    // Solo para un Evento que el servidor rechazó y no tiene (ADR-0004, regla 7).
    @Query("DELETE FROM evento WHERE idEvento = :idEvento")
    suspend fun delete(idEvento: String)
}
