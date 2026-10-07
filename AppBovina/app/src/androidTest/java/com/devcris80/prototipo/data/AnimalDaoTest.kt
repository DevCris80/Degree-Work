package com.devcris80.prototipo.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AnimalDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: AnimalDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.animalDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun crearAnimal(nombre: String): Animal {
        db.perfilFincaDao().guardar(PerfilFinca(nombreFinca = "Finca test"))
        return Animal(
            idAnimal = UUID.randomUUID().toString(),
            idPerfilFinca = PERFIL_FINCA_ID,
            nombre = nombre,
            raza = "Holstein",
            sexo = "Hembra",
            etapa = "Vaca",
            fechaNacimiento = 1_700_000_000_000L,
            proposito = "Leche",
        )
    }

    @Test
    fun insertYObserveActivos_devuelveElAnimalInsertado() = runTest {
        val animal = crearAnimal("Manchas")

        dao.insert(animal)

        val animales = dao.observeActivos().first()
        assertEquals(listOf(animal), animales)
    }

    @Test
    fun darDeBaja_ocultaElAnimalDeObserveActivos_sinBorrarlo() = runTest {
        val animal = crearAnimal("Manchas")
        dao.insert(animal)

        dao.darDeBaja(animal.idAnimal, 1_800_000_000_000L)

        assertTrue(dao.observeActivos().first().isEmpty())
        assertEquals(1_800_000_000_000L, dao.observeById(animal.idAnimal).first()?.fechaBaja)
    }
}
