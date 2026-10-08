package com.devcris80.prototipo.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "animal",
    foreignKeys = [
        ForeignKey(
            entity = PerfilFincaEntity::class,
            parentColumns = ["id"],
            childColumns = ["idPerfilFinca"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("idPerfilFinca")],
)
data class AnimalEntity(
    @PrimaryKey val idAnimal: String,
    val idPerfilFinca: String,
    val nombre: String,
    val raza: String,
    val sexo: String,
    val etapa: String,
    val fechaNacimiento: Long,
    val fechaNacimientoEsEstimada: Boolean = false,
    val proposito: String,
    val fotoUri: String? = null,
    val fechaBaja: Long? = null,
    val sincronizado: Boolean = false,
    // Con valor por defecto solo mientras el código viejo construya la entidad; lo quita #41.
    val fechaModificacion: Long = System.currentTimeMillis(),
)
