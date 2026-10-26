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
import com.devcris80.prototipo.domain.model.Registro
import com.devcris80.prototipo.domain.model.Sexo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Los mappers de Animal, Evento y Registro devuelven todos sus campos, y toda escritura local los deja sin
 * sincronizar y con su fecha de modificación (la regla que la subida usa para saber qué falta).
 */
@RunWith(AndroidJUnit4::class)
class EscriturasLocalesTest {
    private lateinit var db: AppDatabase
    private lateinit var animales: RoomAnimalRepository
    private lateinit var eventos: RoomEventoRepository
    private lateinit var registros: RoomRegistroRepository
    private var ahora = 100L

    @Before
    fun crearBase() = runTest {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .build()
        db.perfilFincaDao().guardar(PerfilFincaEntity("finca-1", "Finca de prueba", fechaModificacion = 0))
        db.usuarioDao().insert(UsuarioEntity("usuario-1", "finca-1", "Ana", fechaModificacion = 0, delDispositivo = true))
        animales = RoomAnimalRepository(db.animalDao(), clock = { ahora })
        eventos = RoomEventoRepository(db.eventoDao(), clock = { ahora })
        registros = RoomRegistroRepository(db.registroDao(), clock = { ahora })
    }

    @After
    fun cerrarBase() = db.close()

    private fun animal(id: String, sexo: Sexo = Sexo.HEMBRA, etapa: Etapa = Etapa.VACA) =
        Animal(id, "finca-1", "Animal $id", "Brahman", sexo, etapa, 5L, proposito = Proposito.LECHE)

    @Test
    fun unAnimalGuardadoVuelveConTodosSusCampos() = runTest {
        animales.insert(animal("padre", Sexo.MACHO, Etapa.TORO))
        animales.insert(animal("madre"))
        val cria = Animal(
            idAnimal = "cria",
            idPerfilFinca = "finca-1",
            nombre = "Lucero",
            raza = "Gyr",
            sexo = Sexo.HEMBRA,
            etapa = Etapa.TERNERA,
            fechaNacimiento = 1_000L,
            fechaNacimientoEsEstimada = true,
            proposito = Proposito.DOBLE_PROPOSITO,
            fotoUri = "content://fotos/lucero.jpg",
            fechaBaja = 2_000L,
            idPadre = "padre",
            idMadre = "madre",
        )

        animales.insert(cria)

        assertEquals(cria, animales.findById("cria"))
        assertEquals(cria, animales.observeById("cria").first())
    }

    @Test
    fun insertarYDarDeBajaUnAnimalLoDejanSinSincronizarConSuFechaDeModificacion() = runTest {
        animales.insert(animal("animal-1"))
        val insertado = db.animalDao().findById("animal-1")!!
        assertFalse(insertado.sincronizado)
        assertEquals(100L, insertado.fechaModificacion)

        db.animalDao().update(insertado.copy(sincronizado = true))
        ahora = 200L
        animales.darDeBaja("animal-1", fecha = 150L)

        val dadoDeBaja = db.animalDao().findById("animal-1")!!
        assertFalse(dadoDeBaja.sincronizado)
        assertEquals(200L, dadoDeBaja.fechaModificacion)
        assertEquals(150L, dadoDeBaja.fechaBaja)
    }

    @Test
    fun unEventoGuardadoVuelveConTodosSusCamposYSinSincronizar() = runTest {
        animales.insert(animal("animal-1"))
        val evento = Evento("evento-1", "animal-1", "usuario-1", "Vacunación", fecha = 50L, detalle = "Aftosa", fechaBaja = 60L)

        eventos.insert(evento)

        // Las lecturas del repositorio dejan fuera lo dado de baja; el mapper se comprueba sobre la fila guardada.
        val fila = db.eventoDao().findById("evento-1")!!
        assertEquals(evento, fila.toDomain())
        assertFalse(fila.sincronizado)
        assertEquals(100L, fila.fechaModificacion)
    }

    @Test
    fun unRegistroGuardadoVuelveConTodosSusCamposYSinSincronizar() = runTest {
        animales.insert(animal("animal-1"))
        val registro = Registro("registro-1", "animal-1", "usuario-1", "lectura-1", 412.5f, 77L, fechaBaja = 88L)

        registros.insert(registro)

        assertEquals(registro, registros.findByIdLectura("lectura-1"))
        val fila = db.registroDao().findById("registro-1")!!
        assertFalse(fila.sincronizado)
        assertEquals(100L, fila.fechaModificacion)
    }

    @Test
    fun unRegistroManualGuardaSuIdLecturaNulo() = runTest {
        animales.insert(animal("animal-1"))
        val manual = Registro("registro-2", "animal-1", "usuario-1", idLectura = null, peso = 300f, timestamp = 5L)

        registros.insert(manual)

        assertEquals(manual, registros.observeByAnimal("animal-1").first().single())
    }
}
