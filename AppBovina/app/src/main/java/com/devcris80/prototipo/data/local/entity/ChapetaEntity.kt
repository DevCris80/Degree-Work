package com.devcris80.prototipo.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chapeta",
    foreignKeys = [
        ForeignKey(
            entity = AnimalEntity::class,
            parentColumns = ["idAnimal"],
            childColumns = ["idAnimal"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("idAnimal"), Index("codigo")],
)
data class ChapetaEntity(
    @PrimaryKey val idChapeta: String,
    val codigo: String,
    val idAnimal: String,
    val fechaAsociacion: Long,
    val fechaDesasociacion: Long? = null,
    val sincronizado: Boolean = false,
)
