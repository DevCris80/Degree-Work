package com.devcris80.prototipo.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.devcris80.prototipo.data.local.entity.PesajePendienteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PesajePendienteDao {
    @Insert
    suspend fun insert(pesajePendiente: PesajePendienteEntity)

    @Query("SELECT * FROM pesaje_pendiente WHERE idLectura = :idLectura LIMIT 1")
    suspend fun findByIdLectura(idLectura: String): PesajePendienteEntity?

    @Query("SELECT * FROM pesaje_pendiente WHERE idPesajePendiente = :idPesajePendiente")
    suspend fun findById(idPesajePendiente: String): PesajePendienteEntity?

    @Query("SELECT * FROM pesaje_pendiente WHERE resolucion IS NULL ORDER BY timestamp DESC")
    fun observeSinResolver(): Flow<List<PesajePendienteEntity>>

    @Query("SELECT * FROM pesaje_pendiente WHERE codigo = :codigo AND resolucion IS NULL ORDER BY timestamp DESC")
    suspend fun findSinResolverByCodigo(codigo: String): List<PesajePendienteEntity>

    // Conciliar es una modificación: la fila vuelve a quedar pendiente de sincronizar. Es
    // definitivo: no toca un pendiente que ya está resuelto.
    @Query(
        "UPDATE pesaje_pendiente SET resolucion = :resolucion, idUsuarioResolucion = :idUsuario, " +
            "fechaResolucion = :fecha, sincronizado = 0, fechaModificacion = :fechaModificacion " +
            "WHERE idPesajePendiente = :idPesajePendiente AND resolucion IS NULL",
    )
    suspend fun conciliar(
        idPesajePendiente: String,
        resolucion: String,
        idUsuario: String,
        fecha: Long,
        fechaModificacion: Long,
    )

    // Lo que usa la subida (MotorSincronizacion). Sus escrituras no llevan EDICION_LOCAL.
    /** Los Pesajes pendientes con cambios por subir, del cambio más antiguo al más reciente. */
    @Query("SELECT * FROM pesaje_pendiente WHERE sincronizado = 0 ORDER BY fechaModificacion")
    suspend fun findPendientes(): List<PesajePendienteEntity>

    @Update
    suspend fun update(pesajePendiente: PesajePendienteEntity)

    // Solo para un Pesaje pendiente que el servidor rechazó y no tiene (ADR-0004, regla 7).
    @Query("DELETE FROM pesaje_pendiente WHERE idPesajePendiente = :idPesajePendiente")
    suspend fun delete(idPesajePendiente: String)
}
