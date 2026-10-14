package com.devcris80.prototipo.ui.screens

import com.devcris80.prototipo.testing.FakeEventoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import java.util.UUID

class NuevoEventoViewModelTest {
    private val eventoRepository = FakeEventoRepository()
    private val idAnimal = UUID.randomUUID().toString()

    private fun crearViewModel() = NuevoEventoViewModel(idAnimal, eventoRepository, clock = { 7L })

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun sinTipoNiDetalle_noSePuedeGuardar() {
        assertFalse(crearViewModel().estado.value.puedeGuardar)
    }

    @Test
    fun conTipoYDetalle_sePuedeGuardar() {
        val viewModel = crearViewModel()
        viewModel.onTipoEventoCambiado("vacuna")
        viewModel.onDetalleCambiado("aftosa")

        assertEquals(true, viewModel.estado.value.puedeGuardar)
    }

    @Test
    fun guardar_insertaElEventoYEmiteGuardado() = runTest {
        val viewModel = crearViewModel()
        viewModel.onTipoEventoCambiado("vacuna")
        viewModel.onDetalleCambiado("aftosa")

        viewModel.onGuardar()

        assertEquals(NuevoEventoEvento.Guardado, viewModel.eventos.first())
        val eventos = eventoRepository.observeByAnimal(idAnimal).first()
        assertEquals(1, eventos.size)
        assertEquals("vacuna", eventos[0].tipoEvento)
        assertEquals("aftosa", eventos[0].detalle)
        assertEquals(idAnimal, eventos[0].idAnimal)
        assertEquals(7L, eventos[0].fecha)
    }

    @Test
    fun guardarSinCompletar_noHaceNada() = runTest {
        val viewModel = crearViewModel()

        viewModel.onGuardar()

        assertEquals(emptyList<Any>(), eventoRepository.observeByAnimal(idAnimal).first())
    }
}
