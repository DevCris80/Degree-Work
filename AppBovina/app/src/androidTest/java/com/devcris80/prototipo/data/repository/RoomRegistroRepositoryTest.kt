package com.devcris80.prototipo.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.devcris80.prototipo.data.local.AppDatabase
import com.devcris80.prototipo.data.local.entity.PerfilFincaEntity
import com.devcris80.prototipo.data.local.entity.UsuarioEntity
import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.model.Registro
import com.devcris80.prototipo.domain.model.Sexo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Los indicadores de peso (#97, #98) confían en que estas lecturas dejan fuera lo dado de baja. */
@RunWith(AndroidJUnit4::class)
class RoomRegistroRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repositorio: RoomRegistroRepository

    @Before
    fun crearBase() = runTest {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .build()
        db.perfilFincaDao().guardar(PerfilFincaEntity("finca-1", "Finca de prueba", fechaModificacion = 0))
        db.usuarioDao().insert(UsuarioEntity("usuario-1", "finca-1", "Ganadero", fechaModificacion = 0, delDispositivo = true))
        RoomAnimalRepository(db.animalDao()).insert(
            Animal("animal-1", "finca-1", "Lucero", "Brahman", Sexo.HEMBRA, Etapa.VACA, 0, proposito = Proposito.LECHE),
        )
        repositorio = RoomRegistroRepository(db.registroDao())
    }

    @After
    fun cerrarBase() = db.close()

    @Test
    fun losRegistrosDeUnAnimalYSuUltimoDejanFueraLosDadosDeBaja() = runTest {
        repositorio.insert(Registro("registro-1", "animal-1", "usuario-1", peso = 400f, timestamp = 1L))
        repositorio.insert(Registro("registro-2", "animal-1", "usuario-1", peso = 430f, timestamp = 2L))
        repositorio.insert(Registro("registro-3", "animal-1", "usuario-1", peso = 999f, timestamp = 3L, fechaBaja = 4L))

        val delAnimal = repositorio.observeByAnimal("animal-1").first()
        assertEquals(listOf("registro-2", "registro-1"), delAnimal.map { it.idRegistro })
        assertEquals("registro-2", repositorio.observeUltimoByAnimal("animal-1").first()?.idRegistro)
    }

    @Test
    fun editarUnPesajeSincronizadoCambiaPesoYHoraLoDejaPendienteYConservaSuIdLectura() = runTest {
        db.registroDao().insert(
            Registro("registro-1", "animal-1", "usuario-1", "lectura-1", 400f, 1L).toEntity(fechaModificacion = 0, idVersion = "del-servidor")
                .copy(rev = 3, sincronizado = true),
        )
        val repositorio = RoomRegistroRepository(db.registroDao(), clock = { 100 }, nuevoIdVersion = { "version-1" })

        repositorio.update(Registro("registro-1", "animal-1", "usuario-1", "otra-lectura", 420.5f, 2L))

        val editado = db.registroDao().findById("registro-1")!!
        assertEquals(420.5f, editado.peso)
        assertEquals(2L, editado.timestamp)
        assertEquals("lectura-1", editado.idLectura)
        assertEquals(false, editado.sincronizado)
        assertEquals(3, editado.revBase)
        assertEquals("version-1", editado.idVersion)
        assertEquals(100L, editado.fechaModificacion)
    }

    @Test
    fun unPesajeDadoDeBajaSaleDelHistorialPeroSigueEnLaBaseYPendienteDeSubir() = runTest {
        db.registroDao().insert(
            Registro("registro-1", "animal-1", "usuario-1", null, 400f, 1L).toEntity(fechaModificacion = 0, idVersion = "del-servidor")
                .copy(rev = 2, sincronizado = true),
        )
        val repositorio = RoomRegistroRepository(db.registroDao(), clock = { 100 }, nuevoIdVersion = { "version-1" })

        repositorio.darDeBaja("registro-1", fecha = 50)

        assertEquals(emptyList<Registro>(), repositorio.observeByAnimal("animal-1").first())
        val guardado = db.registroDao().findById("registro-1")!!
        assertEquals(50L, guardado.fechaBaja)
        assertEquals(false, guardado.sincronizado)
        assertEquals(2, guardado.revBase)
        assertEquals(400f, repositorio.findById("registro-1")!!.peso)
    }
}
