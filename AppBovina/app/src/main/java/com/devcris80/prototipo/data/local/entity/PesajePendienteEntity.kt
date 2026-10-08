package com.devcris80.prototipo.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pesaje_pendiente",
    foreignKeys = [
        // Sin onDelete: un Usuario con Pesajes pendientes a su nombre, o conciliados por él, no se puede borrar.
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["idUsuario"],
            childColumns = ["idUsuario"],
        ),
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["idUsuario"],
            childColumns = ["idUsuarioResolucion"],
        ),
    ],
    indices = [Index("idUsuario"), Index("idUsuarioResolucion"), Index("idLectura", unique = true)],
)
data class PesajePendienteEntity(
    @PrimaryKey val idPesajePendiente: String,
    val idLectura: String,
    val codigo: String,
    val peso: Float,
    val timestamp: Long,
    val idUsuario: String,
    val motivo: String,
    // Las tres son nulas mientras está sin resolver y se llenan juntas al conciliar.
    val resolucion: String? = null,
    val idUsuarioResolucion: String? = null,
    val fechaResolucion: Long? = null,
    val sincronizado: Boolean = false,
    val fechaModificacion: Long,
)
