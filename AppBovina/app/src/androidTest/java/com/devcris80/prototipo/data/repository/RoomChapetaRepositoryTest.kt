package com.devcris80.prototipo.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.devcris80.prototipo.data.local.AppDatabase
import com.devcris80.prototipo.data.local.entity.PerfilFincaEntity
import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Chapeta
import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.model.Sexo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomChapetaRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repositorio: RoomChapetaRepository
    private var ahora = 100L

    @Before
    fun crearBase() = runTest {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .build()
        db.perfilFincaDao().guardar(PerfilFincaEntity("finca-1", "Finca de prueba", fechaModificacion = 0))
        val animales = RoomAnimalRepository(db.animalDao())
        animales.insert(animal("animal-1"))
        animales.insert(animal("animal-2"))
        repositorio = RoomChapetaRepository(db.chapetaDao(), clock = { ahora })
    }

    @After
    fun cerrarBase() = db.close()

    private fun animal(id: String) =
        Animal(id, "finca-1", "Animal $id", "Brahman", Sexo.HEMBRA, Etapa.VACA, 0, proposito = Proposito.LECHE)

    @Test
    fun guardaYLeeLaChapetaConTodosSusCampos() = runTest {
        val chapeta = Chapeta("chapeta-1", "04A1B2C3", "animal-1", fechaAsociacion = 7L)

        repositorio.insert(chapeta)

        assertEquals(chapeta, repositorio.findActivaByCodigo("04A1B2C3"))
        assertEquals(chapeta, repositorio.observeActivaByAnimal("animal-1").first())
        assertEquals(listOf(chapeta), repositorio.observeActivas().first())
    }

    @Test
    fun cadaEscrituraLocalMarcaLaFilaComoPendienteDeSubirYGuardaLaFechaDeModificacion() = runTest {
        repositorio.insert(Chapeta("chapeta-1", "04A1B2C3", "animal-1", fechaAsociacion = 7L))
        val insertada = db.chapetaDao().findById("chapeta-1")!!
        assertFalse(insertada.sincronizado)
        assertEquals(100L, insertada.fechaModificacion)

        // La subida la marca como sincronizada; liberar es una edición nueva y la vuelve a marcar.
        db.chapetaDao().update(insertada.copy(sincronizado = true))
        ahora = 200L
        repositorio.liberar("chapeta-1", fecha = 150L)

        val liberada = db.chapetaDao().findById("chapeta-1")!!
        assertFalse(liberada.sincronizado)
        assertEquals(200L, liberada.fechaModificacion)
        assertEquals(150L, liberada.fechaDesasociacion)
    }

    @Test
    fun unaChapetaLiberadaDejaDeSerActivaPeroSeConservaComoUltima() = runTest {
        repositorio.insert(Chapeta("chapeta-1", "04A1B2C3", "animal-1", fechaAsociacion = 7L))
        repositorio.liberar("chapeta-1", fecha = 9L)

        assertNull(repositorio.findActivaByCodigo("04A1B2C3"))
        assertNull(repositorio.observeActivaByAnimal("animal-1").first())
        assertTrue(repositorio.observeActivas().first().isEmpty())
        assertEquals(9L, repositorio.findUltimaByCodigo("04A1B2C3")?.fechaDesasociacion)
    }

    @Test
    fun laUltimaChapetaDeUnCodigoEsLaAsociadaMasReciente() = runTest {
        repositorio.insert(Chapeta("chapeta-1", "04A1B2C3", "animal-1", fechaAsociacion = 7L))
        repositorio.liberar("chapeta-1", fecha = 9L)
        repositorio.insert(Chapeta("chapeta-2", "04A1B2C3", "animal-2", fechaAsociacion = 20L))

        assertEquals("chapeta-2", repositorio.findUltimaByCodigo("04A1B2C3")?.idChapeta)
        assertEquals("animal-2", repositorio.findActivaByCodigo("04A1B2C3")?.idAnimal)
    }
}
