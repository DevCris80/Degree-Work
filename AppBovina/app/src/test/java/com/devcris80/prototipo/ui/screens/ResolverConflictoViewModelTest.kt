package com.devcris80.prototipo.ui.screens

import com.devcris80.prototipo.domain.model.Eleccion
import com.devcris80.prototipo.domain.model.EntidadConRevisiones
import com.devcris80.prototipo.domain.usecase.ResolverConflicto
import com.devcris80.prototipo.testing.FakeConflictoRepository
import com.devcris80.prototipo.testing.conflictoDeAnimal
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

class ResolverConflictoViewModelTest {
    private val conflictoRepository = FakeConflictoRepository()

    private fun crearViewModel() = ResolverConflictoViewModel(
        EntidadConRevisiones.ANIMAL,
        "animal-1",
        conflictoRepository,
        ResolverConflicto(conflictoRepository),
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
    fun alAbrirMuestraLaVigenteYLosSiblingsSinNingunaEleccion() = runTest {
        conflictoRepository.conflictos.value = listOf(conflictoDeAnimal("animal-1", "v1" to "Jersey"))

        val estado = crearViewModel().estado.value

        assertEquals(1, estado.conflicto?.siblings?.size)
        assertNull(estado.eleccion)
        assertFalse(estado.puedeResolver)
    }

    @Test
    fun sinElegirNoSeResuelveNada() = runTest {
        conflictoRepository.conflictos.value = listOf(conflictoDeAnimal("animal-1", "v1" to "Jersey"))
        val viewModel = crearViewModel()

        viewModel.onResolver()

        assertTrue(conflictoRepository.resoluciones.isEmpty())
    }

    @Test
    fun elegirUnSiblingLoDejaComoLaFilaYCierraTodosLosQueVio() = runTest {
        conflictoRepository.conflictos.value =
            listOf(conflictoDeAnimal("animal-1", "v1" to "Jersey", "v2" to "Brahman"))
        val viewModel = crearViewModel()

        viewModel.onEleccionCambiada(Eleccion.DeSibling("v2"))
        viewModel.onResolver()

        assertEquals(
            listOf(FakeConflictoRepository.Resolucion(EntidadConRevisiones.ANIMAL, "animal-1", "v2", listOf("v1", "v2"))),
            conflictoRepository.resoluciones,
        )
        assertEquals(ResolverConflictoEvento.Resuelto, viewModel.eventos.first())
    }

    @Test
    fun conservarLaVigenteNoNombraUnaVersionElegida() = runTest {
        conflictoRepository.conflictos.value = listOf(conflictoDeAnimal("animal-1", "v1" to "Jersey"))
        val viewModel = crearViewModel()

        viewModel.onEleccionCambiada(Eleccion.Vigente)
        viewModel.onResolver()

        assertNull(conflictoRepository.resoluciones.single().idVersionElegida)
        assertEquals(listOf("v1"), conflictoRepository.resoluciones.single().idsResueltos)
    }

    @Test
    fun unSiblingQueLlegaDespuesDeAbrirNoSeResuelveYSeAvisa() = runTest {
        conflictoRepository.conflictos.value = listOf(conflictoDeAnimal("animal-1", "v1" to "Jersey"))
        val viewModel = crearViewModel()

        conflictoRepository.conflictos.value =
            listOf(conflictoDeAnimal("animal-1", "v1" to "Jersey", "v2" to "Brahman"))
        assertTrue(viewModel.estado.value.hayNuevos)

        viewModel.onEleccionCambiada(Eleccion.DeSibling("v1"))
        viewModel.onResolver()

        assertEquals(listOf("v1"), conflictoRepository.resoluciones.single().idsResueltos)
        // El que llegó después sigue sin resolver.
        assertEquals(listOf("v2"), conflictoRepository.conflictos.value.single().siblings.map { it.sibling.idVersion })
    }
}
