package com.devcris80.prototipo.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "usuario",
    foreignKeys = [
        ForeignKey(
            entity = PerfilFincaEntity::class,
            parentColumns = ["idPerfilFinca"],
            childColumns = ["idPerfilFinca"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("idPerfilFinca")],
)
data class UsuarioEntity(
    @PrimaryKey val idUsuario: String,
    val idPerfilFinca: String,
    val nombre: String,
    val rol: String = "Ganadero",
    val sincronizado: Boolean = false,
    val fechaModificacion: Long,
    /** El Usuario que inició sesión en este dispositivo. Los demás Usuarios de la finca llegan con la descarga. */
    @ColumnInfo(defaultValue = "0") val delDispositivo: Boolean,
)
