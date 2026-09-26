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
class EventoDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var animalDao: AnimalDao
    private lateinit var eventoDao: EventoDao
    private lateinit var idAnimal: String

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        animalDao = db.animalDao()
        eventoDao = db.eventoDao()
        idAnimal = UUID.randomUUID().toString()
        animalDao.insert(
            Animal(
                idAnimal = idAnimal,
                nombre = "Manchas",
                raza = "Holstein",
                sexo = "Hembra",
                etapa = "Vaca",
                edadAnios = 3,
                edadMeses = 0,
                proposito = "Leche",
            ),
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun observeByAnimal_devuelveEventosOrdenadosPorFechaDescendente() = runTest {
        val antiguo = Evento(
            idEvento = UUID.randomUUID().toString(),
            idAnimal = idAnimal,
            tipoEvento = "nacimiento",
            fecha = 1_000L,
            detalle = "nace en el potrero norte",
        )
        val reciente = Evento(
            idEvento = UUID.randomUUID().toString(),
            idAnimal = idAnimal,
            tipoEvento = "vacuna",
            fecha = 2_000L,
            detalle = "vacuna aftosa",
        )

        eventoDao.insert(antiguo)
        eventoDao.insert(reciente)

        val eventos = eventoDao.observeByAnimal(idAnimal).first()
        assertEquals(listOf(reciente, antiguo), eventos)
    }
}
