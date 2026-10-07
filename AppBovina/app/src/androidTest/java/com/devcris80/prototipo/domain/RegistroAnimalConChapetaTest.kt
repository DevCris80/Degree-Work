package com.devcris80.prototipo.domain

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.devcris80.prototipo.data.Animal
import com.devcris80.prototipo.data.AppDatabase
import com.devcris80.prototipo.data.Chapeta
import com.devcris80.prototipo.data.PERFIL_FINCA_ID
import com.devcris80.prototipo.data.PerfilFinca
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class RegistroAnimalConChapetaTest {
    private lateinit var db: AppDatabase

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).addCallback(AppDatabase.CALLBACK_INDICES_CHAPETA).allowMainThreadQueries().build()
        db.perfilFincaDao().guardar(PerfilFinca(nombreFinca = "Finca test"))
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun animal(nombre: String) = Animal(
        idAnimal = UUID.randomUUID().toString(),
        idPerfilFinca = PERFIL_FINCA_ID,
        nombre = nombre,
        raza = "Holstein",
        sexo = "Hembra",
        etapa = "Vaca",
        fechaNacimiento = 1_700_000_000_000L,
        proposito = "Leche",
    )

    @Test
    fun conCodigoLibre_guardaAnimalYChapetaActiva() = runTest {
        val registro = RegistroAnimalConChapeta(db, clock = { 1L })
        val nuevo = animal("Manchas")

        val resultado = registro.registrar(nuevo, "04A1B2C3")

        assertEquals(RegistroAnimalConChapeta.Resultado.Exito(nuevo.idAnimal), resultado)
        assertEquals(nuevo, db.animalDao().findById(nuevo.idAnimal))
        assertEquals(
            "04A1B2C3",
            db.chapetaDao().findActivaByCodigo("04A1B2C3")?.codigo,
        )
    }

    @Test
    fun conCodigoYaActivo_noGuardaNada() = runTest {
        val registro = RegistroAnimalConChapeta(db, clock = { 1L })
        val existente = animal("Canela")
        registro.registrar(existente, "04A1B2C3")
        val duplicado = animal("Manchas")

        val resultado = registro.registrar(duplicado, "04A1B2C3")

        assertEquals(RegistroAnimalConChapeta.Resultado.CodigoYaActivo("04A1B2C3"), resultado)
        assertTrue(db.animalDao().findById(duplicado.idAnimal) == null)
    }

    @Test
    fun siFallaLaChapeta_noQuedaAnimalHuerfano() = runTest {
        val registro = RegistroAnimalConChapeta(
            db,
            clock = { throw IllegalStateException("fallo simulado") },
        )
        val nuevo = animal("Manchas")

        assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking { registro.registrar(nuevo, "04A1B2C3") }
        }

        assertTrue(db.animalDao().getAllOnce().isEmpty())
        assertTrue(db.chapetaDao().findActivaByCodigo("04A1B2C3") == null)
    }
}
