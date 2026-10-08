package com.devcris80.prototipo.data.repository

import com.devcris80.prototipo.data.local.dao.AnimalDao
import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.repository.AnimalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomAnimalRepository(private val animalDao: AnimalDao) : AnimalRepository {
    override suspend fun insert(animal: Animal) = animalDao.insert(animal.toEntity())

    override fun observeActivos(): Flow<List<Animal>> =
        animalDao.observeActivos().map { animales -> animales.map { it.toDomain() } }

    override fun observeById(idAnimal: String): Flow<Animal?> =
        animalDao.observeById(idAnimal).map { it?.toDomain() }

    override suspend fun findById(idAnimal: String): Animal? = animalDao.findById(idAnimal)?.toDomain()

    override suspend fun darDeBaja(idAnimal: String, fecha: Long) = animalDao.darDeBaja(idAnimal, fecha)
}
