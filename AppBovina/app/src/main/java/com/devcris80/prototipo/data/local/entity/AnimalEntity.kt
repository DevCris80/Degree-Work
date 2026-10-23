package com.devcris80.prototipo.data.local.entity

import androidx.room.ColumnInfo
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
    val fechaModificacion: Long,
    // Revisiones (ADR-0004). `rev` es la revisión vigente que conoce el celular, nula hasta la primera
    // subida; `revBase`, la revisión de la que partió la edición pendiente; `idVersion`, la última
    // escritura local. Las llenan los repositorios (ver EDICION_LOCAL). El valor por defecto solo
    // existe para que la Migration pueda agregar la columna; la Migration la llena enseguida.
    val rev: Int? = null,
    val revBase: Int? = null,
    @ColumnInfo(defaultValue = "") val idVersion: String,
)
