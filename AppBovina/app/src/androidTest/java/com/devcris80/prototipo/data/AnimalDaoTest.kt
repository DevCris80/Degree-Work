package com.devcris80.prototipo.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
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

    @Test
    fun insertYObserveAll_devuelveElAnimalInsertado() = runTest {
        val animal = Animal(
            idAnimal = UUID.randomUUID().toString(),
            nombre = "Manchas",
            raza = "Holstein",
            sexo = "Hembra",
            etapa = "Vaca",
            edadAnios = 3,
            edadMeses = 0,
            proposito = "Leche",
        )

        dao.insert(animal)

        val animales = dao.observeAll().first()
        assertEquals(listOf(animal), animales)
    }
}
