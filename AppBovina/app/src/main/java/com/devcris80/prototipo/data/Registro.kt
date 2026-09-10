package com.devcris80.prototipo.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "registro",
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
data class Registro(
    @PrimaryKey val idRegistro: String,
    val idAnimal: String,
    val peso: Float,
    val timestamp: Long,
    val sincronizado: Boolean = false,
)
