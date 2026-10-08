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
        // Sin onDelete: un Usuario con Registros a su nombre no se puede borrar.
        ForeignKey(
            entity = UsuarioEntity::class,
            parentColumns = ["idUsuario"],
            childColumns = ["idUsuario"],
        ),
    ],
    indices = [Index("idAnimal"), Index("idUsuario"), Index("idLectura", unique = true)],
)
data class RegistroEntity(
    @PrimaryKey val idRegistro: String,
    val idAnimal: String,
    val idUsuario: String,
    val idLectura: String? = null,
    val peso: Float,
    val timestamp: Long,
    val fechaBaja: Long? = null,
    val sincronizado: Boolean = false,
    // Con valor por defecto solo mientras haya código que construya la entidad sin pasar por un
    // repositorio (las pantallas viejas y SeedData); lo quita #41.
    val fechaModificacion: Long = System.currentTimeMillis(),
)
