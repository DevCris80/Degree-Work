package com.devcris80.prototipo.data.local.entity

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
    ],
    indices = [Index("idAnimal")],
)
data class EventoEntity(
    @PrimaryKey val idEvento: String,
    val idAnimal: String,
    val tipoEvento: String,
    val fecha: Long,
    val detalle: String,
    val fechaBaja: Long? = null,
    val sincronizado: Boolean = false,
)
