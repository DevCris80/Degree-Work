package com.devcris80.prototipo.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.devcris80.prototipo.data.local.entity.EstadoSyncEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EstadoSyncDao {
    @Upsert
    suspend fun guardar(estado: EstadoSyncEntity)

    /** Nulo hasta la primera descarga. */
    @Query("SELECT * FROM estado_sync WHERE id = 0")
    fun observe(): Flow<EstadoSyncEntity?>

    @Query("SELECT * FROM estado_sync WHERE id = 0")
    suspend fun getOnce(): EstadoSyncEntity?

    /** Las filas con cambios por subir, de todas las tablas que suben: las de `tablasSincronizadas`. */
    @Query(
        "SELECT (SELECT COUNT(*) FROM perfil_finca WHERE sincronizado = 0)" +
            " + (SELECT COUNT(*) FROM animal WHERE sincronizado = 0)" +
            " + (SELECT COUNT(*) FROM chapeta WHERE sincronizado = 0)" +
            " + (SELECT COUNT(*) FROM evento WHERE sincronizado = 0)" +
            " + (SELECT COUNT(*) FROM registro WHERE sincronizado = 0)" +
            " + (SELECT COUNT(*) FROM pesaje_pendiente WHERE sincronizado = 0)",
    )
    fun observeCambiosSinSubir(): Flow<Int>

    /** Las fotos de los Animales que no han subido. No cuentan en [observeCambiosSinSubir]: suben aparte. */
    @Query("SELECT COUNT(*) FROM animal WHERE fotoPorSubir = 1 AND fotoUri IS NOT NULL")
    fun observeFotosSinSubir(): Flow<Int>
}
