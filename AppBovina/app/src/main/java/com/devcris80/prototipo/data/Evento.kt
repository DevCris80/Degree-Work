package com.devcris80.prototipo.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "evento",
    foreignKeys = [
        ForeignKey(
            entity = Animal::class,
            parentColumns = ["idAnimal"],
            childColumns = ["idAnimal"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("idAnimal")],
)
data class Evento(
    @PrimaryKey val idEvento: String,
    val idAnimal: String,
    val tipoEvento: String,
    val fecha: Long,
    val detalle: String,
    val fechaBaja: Long? = null,
    val sincronizado: Boolean = false,
)
