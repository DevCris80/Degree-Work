package com.devcris80.prototipo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Una sola fila: hasta dónde llegó la descarga y cuándo terminó la última sincronización completa. */
@Entity(tableName = "estado_sync")
data class EstadoSyncEntity(
    @PrimaryKey val id: Int = 0,
    val cursorDescarga: Long,
    val fechaUltimaSincronizacion: Long? = null,
)
