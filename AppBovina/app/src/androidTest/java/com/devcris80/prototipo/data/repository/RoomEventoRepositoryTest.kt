package com.devcris80.prototipo.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.devcris80.prototipo.data.local.AppDatabase
import com.devcris80.prototipo.data.local.entity.PerfilFincaEntity
import com.devcris80.prototipo.data.local.entity.UsuarioEntity
import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.Evento
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.model.Sexo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomEventoRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repositorio: RoomEventoRepository

    @Before
    fun crearBase() = runTest {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .build()
        db.perfilFincaDao().guardar(PerfilFincaEntity("finca-1", "Finca de prueba", fechaModificacion = 0))
        db.usuarioDao().insert(UsuarioEntity("usuario-1", "finca-1", "Ganadero", fechaModificacion = 0))
        val animales = RoomAnimalRepository(db.animalDao())
        animales.insert(animal("activo", Sexo.HEMBRA, Etapa.VACA))
        animales.insert(animal("de-baja", Sexo.MACHO, Etapa.TORO).copy(fechaBaja = 9L))
        repositorio = RoomEventoRepository(db.eventoDao())
    }

    private fun animal(idAnimal: String, sexo: Sexo, etapa: Etapa) =
        Animal(idAnimal, "finca-1", idAnimal, "Brahman", sexo, etapa, fechaNacimiento = 0, proposito = Proposito.CARNE)

    @After
    fun cerrarBase() = db.close()

    @Test
    fun losEventosActivosDejanFueraLosDadosDeBajaPeroNoLosDeAnimalesDadosDeBaja() = runTest {
        repositorio.insert(Evento("evento-1", "activo", "usuario-1", "Vacunación", fecha = 1L, detalle = ""))
        repositorio.insert(
            Evento("evento-2", "activo", "usuario-1", "Vacunación", fecha = 3L, detalle = "", fechaBaja = 4L),
        )
        repositorio.insert(Evento("evento-3", "de-baja", "usuario-1", "Tratamiento", fecha = 2L, detalle = ""))

        assertEquals(listOf("evento-3", "evento-1"), repositorio.observeActivos().first().map { it.idEvento })
    }
}
