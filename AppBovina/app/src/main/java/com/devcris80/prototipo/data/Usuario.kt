package com.devcris80.prototipo.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "usuario",
    foreignKeys = [
        ForeignKey(
            entity = PerfilFinca::class,
            parentColumns = ["id"],
            childColumns = ["idPerfilFinca"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("idPerfilFinca")],
)
data class Usuario(
    @PrimaryKey val idUsuario: String,
    val idPerfilFinca: String,
    val nombre: String,
    val rol: String = "Ganadero",
    val sincronizado: Boolean = false,
)
