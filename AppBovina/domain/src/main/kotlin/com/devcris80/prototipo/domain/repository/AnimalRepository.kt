package com.devcris80.prototipo.domain.repository

import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Sexo
import kotlinx.coroutines.flow.Flow

interface AnimalRepository {
    suspend fun insert(animal: Animal)

    /** Animales que no están dados de baja, ordenados por nombre. */
    fun observeActivos(): Flow<List<Animal>>

    /**
     * Animales de un sexo, también los dados de baja, porque un padre o una madre pueden haber
     * salido de la finca antes de registrar la cría. Primero los activos, cada grupo por nombre.
     */
    fun observePorSexo(sexo: Sexo): Flow<List<Animal>>

    fun observeById(idAnimal: String): Flow<Animal?>

    suspend fun findById(idAnimal: String): Animal?

    suspend fun darDeBaja(idAnimal: String, fecha: Long)
}
