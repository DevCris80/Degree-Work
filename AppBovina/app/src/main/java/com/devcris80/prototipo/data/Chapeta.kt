package com.devcris80.prototipo.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chapeta",
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
data class Chapeta(
    @PrimaryKey val idChip: String,
    val idAnimal: String,
    val fechaAsociacion: Long,
    val fechaDesasociacion: Long? = null,
    val sincronizado: Boolean = false,
)
