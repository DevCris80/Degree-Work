package com.devcris80.prototipo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

const val PERFIL_FINCA_ID = "perfil_local"

@Entity(tableName = "perfil_finca")
data class PerfilFinca(
    @PrimaryKey val id: String = PERFIL_FINCA_ID,
    val nombreFinca: String,
)
