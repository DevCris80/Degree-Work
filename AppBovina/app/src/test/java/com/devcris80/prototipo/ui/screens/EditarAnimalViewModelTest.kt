package com.devcris80.prototipo.ui.screens

import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.model.Sexo
import com.devcris80.prototipo.domain.usecase.ActualizarAnimal
import com.devcris80.prototipo.testing.FakeAnimalRepository
import com.devcris80.prototipo.testing.FakeTransaccion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EditarAnimalViewModelTest {
    private val animalRepository = FakeAnimalRepository()

    private fun animal(
        id: String = "animal-1",
        sexo: Sexo = Sexo.HEMBRA,
        etapa: Etapa = Etapa.VACA,
        fechaBaja: Long? = null,
    ) = Animal(
        idAnimal = id,
        idPerfilFinca = "perfil_local",
        nombre = "Manchas",
        raza = "Holstein",
        sexo = sexo,
        etapa = etapa,
        fechaNacimiento = 1L,
        proposito = Proposito.LECHE,
        fechaBaja = fechaBaja,
    )

    private fun crearViewModel(idAnimal: String = "animal-1") = EditarAnimalViewModel(
        idAnimal,
        animalRepository,
        ActualizarAnimal(animalRepository, FakeTransaccion()),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun alAbrirCargaLosDatosDelAnimalEnElFormulario() = runTest {
        animalRepository.set(animal())

        val estado = crearViewModel().estado.value

        assertEquals("Manchas", estado.nombre)
        assertEquals("Holstein", estado.raza)
        assertEquals(Sexo.HEMBRA, estado.sexo)
        assertEquals(Etapa.VACA, estado.etapa)
        assertEquals(Proposito.LECHE, estado.proposito)
        assertTrue(estado.puedeGuardar)
    }

    @Test
    fun cambiarLaRazaYGuardarEscribeElAnimalConLaRazaNuevaYAvisaQueGuardo() = runTest {
        animalRepository.set(animal())
        val viewModel = crearViewModel()

        viewModel.onRazaCambiada("Jersey")
        viewModel.onGuardar()

        assertEquals(listOf(animal().copy(raza = "Jersey")), animalRepository.actualizados)
        assertEquals(EditarAnimalEvento.Guardado, viewModel.eventos.first())
    }

    @Test
    fun cambiarElSexoObligaAElegirDeNuevoLaEtapaYSinEllaNoSePuedeGuardar() = runTest {
        animalRepository.set(animal())
        val viewModel = crearViewModel()

        viewModel.onSexoSeleccionado(Sexo.MACHO)

        assertNull(viewModel.estado.value.etapa)
        assertFalse(viewModel.estado.value.puedeGuardar)

        viewModel.onEtapaSeleccionada(Etapa.TORO)

        assertTrue(viewModel.estado.value.puedeGuardar)
    }

    @Test
    fun guardarSinCambiosNoCreaUnaEscrituraNueva() = runTest {
        animalRepository.set(animal())
        val viewModel = crearViewModel()

        viewModel.onGuardar()

        assertTrue(animalRepository.actualizados.isEmpty())
        assertEquals(EditarAnimalEvento.Guardado, viewModel.eventos.first())
    }

    @Test
    fun unAnimalDadoDeBajaNoSeEdita() = runTest {
        animalRepository.set(animal(fechaBaja = 5L))
        val viewModel = crearViewModel()

        viewModel.onRazaCambiada("Jersey")
        viewModel.onGuardar()

        assertTrue(animalRepository.actualizados.isEmpty())
        assertTrue(viewModel.estado.value.noEditable)
    }

    @Test
    fun losCandidatosAPadreYMadreNoIncluyenAlPropioAnimal() = runTest {
        animalRepository.set(animal(id = "animal-1"))
        animalRepository.set(animal(id = "madre-1"))
        val viewModel = crearViewModel()

        assertEquals(listOf("madre-1"), viewModel.estado.value.candidatosMadre.map { it.idAnimal })
    }
}
