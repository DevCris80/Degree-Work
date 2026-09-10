package com.devcris80.prototipo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "animal")
data class Animal(
    @PrimaryKey val idAnimal: String,
    val nombre: String,
    val raza: String,
    val sexo: String,
    val edad: Int,
    val proposito: String,
    val sincronizado: Boolean = false,
)
