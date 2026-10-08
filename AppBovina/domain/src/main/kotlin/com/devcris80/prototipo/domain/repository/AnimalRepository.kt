package com.devcris80.prototipo.domain.repository

import com.devcris80.prototipo.domain.model.Animal
import kotlinx.coroutines.flow.Flow

interface AnimalRepository {
    suspend fun insert(animal: Animal)

    /** Animales que no están dados de baja, ordenados por nombre. */
    fun observeActivos(): Flow<List<Animal>>

    fun observeById(idAnimal: String): Flow<Animal?>

    suspend fun findById(idAnimal: String): Animal?

    suspend fun darDeBaja(idAnimal: String, fecha: Long)
}
