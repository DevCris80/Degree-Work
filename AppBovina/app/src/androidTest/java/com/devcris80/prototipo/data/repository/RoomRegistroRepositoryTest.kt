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
        db.usuarioDao().insert(UsuarioEntity("usuario-1", "finca-1", "Ganadero", fechaModificacion = 0))
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
}
