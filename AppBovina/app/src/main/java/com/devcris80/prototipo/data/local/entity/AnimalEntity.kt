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
            parentColumns = ["idPerfilFinca"],
            childColumns = ["idPerfilFinca"],
            onDelete = ForeignKey.CASCADE,
        ),
        // Parentesco: si el padre o la madre desaparecen, la cría queda sin ese dato en vez de borrarse.
        ForeignKey(
            entity = AnimalEntity::class,
            parentColumns = ["idAnimal"],
            childColumns = ["idPadre"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = AnimalEntity::class,
            parentColumns = ["idAnimal"],
            childColumns = ["idMadre"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("idPerfilFinca"), Index("idPadre"), Index("idMadre")],
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
    val idPadre: String? = null,
    val idMadre: String? = null,
    val sincronizado: Boolean = false,
    val fechaModificacion: Long,
)
