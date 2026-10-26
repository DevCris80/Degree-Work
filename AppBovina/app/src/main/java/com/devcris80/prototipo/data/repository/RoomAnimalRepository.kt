package com.devcris80.prototipo.data.repository

import com.devcris80.prototipo.data.local.dao.AnimalDao
import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Sexo
import com.devcris80.prototipo.domain.repository.AnimalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class RoomAnimalRepository(
    private val animalDao: AnimalDao,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val nuevoIdVersion: () -> String = { UUID.randomUUID().toString() },
) : AnimalRepository {
    override suspend fun insert(animal: Animal) =
        animalDao.insert(animal.toEntity(fechaModificacion = clock(), idVersion = nuevoIdVersion()))

    override fun observeActivos(): Flow<List<Animal>> =
        animalDao.observeActivos().map { animales -> animales.map { it.toDomain() } }

    override fun observePorSexo(sexo: Sexo): Flow<List<Animal>> =
        animalDao.observePorSexo(sexo.textoGuardado()).map { animales -> animales.map { it.toDomain() } }

    override fun observeById(idAnimal: String): Flow<Animal?> =
        animalDao.observeById(idAnimal).map { it?.toDomain() }

    override suspend fun findById(idAnimal: String): Animal? = animalDao.findById(idAnimal)?.toDomain()

    override suspend fun tieneCrias(idAnimal: String): Boolean = animalDao.tieneCrias(idAnimal)

    override suspend fun update(animal: Animal) {
        val fila = animal.toEntity(fechaModificacion = clock(), idVersion = nuevoIdVersion())
        animalDao.editar(
            idAnimal = fila.idAnimal,
            nombre = fila.nombre,
            raza = fila.raza,
            sexo = fila.sexo,
            etapa = fila.etapa,
            fechaNacimiento = fila.fechaNacimiento,
            fechaNacimientoEsEstimada = fila.fechaNacimientoEsEstimada,
            proposito = fila.proposito,
            fotoUri = fila.fotoUri,
            idPadre = fila.idPadre,
            idMadre = fila.idMadre,
            idVersion = fila.idVersion,
            fechaModificacion = fila.fechaModificacion,
        )
    }

    override suspend fun darDeBaja(idAnimal: String, fecha: Long) =
        animalDao.darDeBaja(idAnimal, fecha, idVersion = nuevoIdVersion(), fechaModificacion = clock())
}
