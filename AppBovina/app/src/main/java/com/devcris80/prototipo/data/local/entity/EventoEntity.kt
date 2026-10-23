package com.devcris80.prototipo.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "evento",
    foreignKeys = [
        ForeignKey(
            entity = AnimalEntity::class,
            parentColumns = ["idAnimal"],
            childColumns = ["idAnimal"],
            onDelete = ForeignKey.CASCADE,
        ),
        // Sin onDelete: un Usuario con Eventos a su nombre no se puede borrar.
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["idUsuario"],
            childColumns = ["idUsuario"],
        ),
    ],
    indices = [Index("idAnimal"), Index("idUsuario")],
)
data class EventoEntity(
    @PrimaryKey val idEvento: String,
    val idAnimal: String,
    val idUsuario: String,
    val tipoEvento: String,
    val fecha: Long,
    val detalle: String,
    val fechaBaja: Long? = null,
    val sincronizado: Boolean = false,
    val fechaModificacion: Long,
    // Revisiones: ver AnimalEntity.
    val rev: Int? = null,
    val revBase: Int? = null,
    @ColumnInfo(defaultValue = "") val idVersion: String,
)
