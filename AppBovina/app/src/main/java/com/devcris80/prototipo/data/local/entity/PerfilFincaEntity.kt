package com.devcris80.prototipo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

const val PERFIL_FINCA_ID = "perfil_local"

@Entity(tableName = "perfil_finca")
data class PerfilFincaEntity(
    @PrimaryKey val id: String = PERFIL_FINCA_ID,
    val nombreFinca: String,
    val sincronizado: Boolean = false,
    // Con valor por defecto solo mientras haya código que construya la entidad sin pasar por un
    // repositorio (las pantallas viejas y SeedData); lo quita #41.
    val fechaModificacion: Long = System.currentTimeMillis(),
)
