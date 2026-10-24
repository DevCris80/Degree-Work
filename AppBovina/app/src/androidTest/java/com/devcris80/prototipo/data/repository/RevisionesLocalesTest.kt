package com.devcris80.prototipo.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.devcris80.prototipo.data.local.AppDatabase
import com.devcris80.prototipo.data.local.entity.AnimalEntity
import com.devcris80.prototipo.data.local.entity.PerfilFincaEntity
import com.devcris80.prototipo.data.local.entity.UsuarioEntity
import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.Evento
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.model.Registro
import com.devcris80.prototipo.domain.model.Sexo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Toda escritura local de Animal, Evento o Registro deja lo que la subida necesita (ADR-0004, regla 2). */
@RunWith(AndroidJUnit4::class)
class RevisionesLocalesTest {
    private lateinit var db: AppDatabase
    private val versiones = ArrayDeque(listOf("version-1", "version-2", "version-3"))
    private val nuevoIdVersion = { versiones.removeFirst() }

    private val animal = Animal(
        idAnimal = "animal-1",
        idPerfilFinca = "finca-1",
        nombre = "Lucero",
        raza = "Brahman",
        sexo = Sexo.HEMBRA,
        etapa = Etapa.VACA,
        fechaNacimiento = 0,
        proposito = Proposito.LECHE,
    )

    @Before
    fun crearBase() = runTest {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .build()
        db.perfilFincaDao().guardar(PerfilFincaEntity("finca-1", "Finca de prueba", fechaModificacion = 0))
        db.usuarioDao().insert(UsuarioEntity("usuario-1", "finca-1", "Ganadero", fechaModificacion = 0, delDispositivo = true))
    }

    @After
    fun cerrarBase() = db.close()

    @Test
    fun unAnimalNuevoNoTieneRevisionPeroSiIdVersion() = runTest {
        RoomAnimalRepository(db.animalDao(), nuevoIdVersion = nuevoIdVersion).insert(animal)

        val guardado = db.animalDao().findById("animal-1")!!
        assertNull(guardado.rev)
        assertNull(guardado.revBase)
        assertEquals("version-1", guardado.idVersion)
    }

    @Test
    fun unEventoYUnRegistroNuevosTienenIdVersion() = runTest {
        RoomAnimalRepository(db.animalDao(), nuevoIdVersion = nuevoIdVersion).insert(animal)
        RoomEventoRepository(db.eventoDao(), nuevoIdVersion = nuevoIdVersion)
            .insert(Evento("evento-1", "animal-1", "usuario-1", "Vacunación", fecha = 0, detalle = "Aftosa"))
        RoomRegistroRepository(db.registroDao(), nuevoIdVersion = nuevoIdVersion)
            .insert(Registro("registro-1", "animal-1", "usuario-1", peso = 410f, timestamp = 0))

        val evento = db.eventoDao().observeByAnimal("animal-1").first().single()
        val registro = db.registroDao().observeByAnimal("animal-1").first().single()
        assertEquals("version-2", evento.idVersion)
        assertNull(evento.rev)
        assertEquals("version-3", registro.idVersion)
        assertNull(registro.rev)
    }

    @Test
    fun dosEscriturasSinSincronizarCambianElIdVersionYConservanElRevBase() = runTest {
        // Un Animal que ya subió y quedó en la revisión 3.
        db.animalDao().insert(
            animal.toEntity(fechaModificacion = 0, idVersion = "del-servidor")
                .copy(rev = 3, sincronizado = true),
        )
        val repositorio = RoomAnimalRepository(db.animalDao(), clock = { 100 }, nuevoIdVersion = nuevoIdVersion)

        repositorio.darDeBaja("animal-1", fecha = 50)
        val primera = db.animalDao().findById("animal-1")!!
        repositorio.darDeBaja("animal-1", fecha = 60)
        val segunda = db.animalDao().findById("animal-1")!!

        assertEquals(3, primera.revBase)
        assertEquals("version-1", primera.idVersion)
        assertFalse(primera.sincronizado)
        assertEquals(3, segunda.revBase)
        assertEquals("version-2", segunda.idVersion)
        assertEquals(3, segunda.rev)
    }

    @Test
    fun editarUnAnimalSincronizadoGuardaLosCambiosYLoDejaPendienteConSuRevBase() = runTest {
        db.animalDao().insert(
            animal.toEntity(fechaModificacion = 0, idVersion = "del-servidor")
                .copy(rev = 3, sincronizado = true),
        )
        val repositorio = RoomAnimalRepository(db.animalDao(), clock = { 100 }, nuevoIdVersion = nuevoIdVersion)

        repositorio.update(animal.copy(raza = "Jersey", nombre = "Lucero II"))

        val editado = db.animalDao().findById("animal-1")!!
        assertEquals("Jersey", editado.raza)
        assertEquals("Lucero II", editado.nombre)
        assertFalse(editado.sincronizado)
        assertEquals(3, editado.revBase)
        assertEquals("version-1", editado.idVersion)
        assertEquals(100L, editado.fechaModificacion)
    }
}
