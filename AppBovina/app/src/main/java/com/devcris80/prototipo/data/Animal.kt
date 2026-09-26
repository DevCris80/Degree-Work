package com.devcris80.prototipo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "animal")
data class Animal(
    @PrimaryKey val idAnimal: String,
    val nombre: String,
    val raza: String,
    val sexo: String,
    val etapa: String,
    val edadAnios: Int,
    val edadMeses: Int,
    val proposito: String,
    val fotoUri: String? = null,
    val sincronizado: Boolean = false,
)
