package com.devcris80.prototipo.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.devcris80.prototipo.data.local.AppDatabase
import com.devcris80.prototipo.data.local.entity.PerfilFincaEntity
import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Etapa
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
class RoomAnimalRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repositorio: RoomAnimalRepository

    @Before
    fun crearBase() = runTest {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .build()
        db.perfilFincaDao().guardar(PerfilFincaEntity("finca-1", "Finca de prueba", fechaModificacion = 0))
        repositorio = RoomAnimalRepository(db.animalDao())
    }

    @After
    fun cerrarBase() = db.close()

    @Test
    fun losCandidatosAPadreIncluyenLosDadosDeBaja_primeroLosActivosYCadaGrupoPorNombre() = runTest {
        repositorio.insert(toro("toro-1", "Zeus"))
        repositorio.insert(toro("toro-2", "Atlas", fechaBaja = 5L))
        repositorio.insert(toro("toro-3", "Bravo"))
        repositorio.insert(toro("vaca-1", "Lola").copy(sexo = Sexo.HEMBRA, etapa = Etapa.VACA))

        val candidatos = repositorio.observePorSexo(Sexo.MACHO).first()

        assertEquals(listOf("Bravo", "Zeus", "Atlas"), candidatos.map { it.nombre })
    }

    private fun toro(idAnimal: String, nombre: String, fechaBaja: Long? = null) = Animal(
        idAnimal = idAnimal,
        idPerfilFinca = "finca-1",
        nombre = nombre,
        raza = "Cebú",
        sexo = Sexo.MACHO,
        etapa = Etapa.TORO,
        fechaNacimiento = 0,
        proposito = Proposito.CARNE,
        fechaBaja = fechaBaja,
    )
}
