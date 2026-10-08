package com.devcris80.prototipo.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "registro",
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
data class RegistroEntity(
    @PrimaryKey val idRegistro: String,
    val idAnimal: String,
    val peso: Float,
    val timestamp: Long,
    val fechaBaja: Long? = null,
    val sincronizado: Boolean = false,
)
