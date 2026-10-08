package com.devcris80.prototipo.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pesaje_pendiente",
    foreignKeys = [
        // Sin onDelete: un Usuario con Pesajes pendientes a su nombre no se puede borrar.
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["idUsuario"],
            childColumns = ["idUsuario"],
        ),
    ],
    indices = [Index("idUsuario"), Index("idLectura", unique = true)],
)
data class PesajePendienteEntity(
    @PrimaryKey val idPesajePendiente: String,
    val idLectura: String,
    val codigo: String,
    val peso: Float,
    val timestamp: Long,
    val idUsuario: String,
    val motivo: String,
    val resuelto: Boolean = false,
    val sincronizado: Boolean = false,
    val fechaModificacion: Long,
)
