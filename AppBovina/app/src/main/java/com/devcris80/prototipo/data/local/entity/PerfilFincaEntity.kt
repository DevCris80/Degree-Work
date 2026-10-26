package com.devcris80.prototipo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Un solo PerfilFinca por dispositivo: SupabaseSesionRepository no crea cuenta ni inicia sesión con los datos de otra.
@Entity(tableName = "perfil_finca")
data class PerfilFincaEntity(
    @PrimaryKey val idPerfilFinca: String,
    val nombreFinca: String,
    val sincronizado: Boolean = false,
    val fechaModificacion: Long,
)
