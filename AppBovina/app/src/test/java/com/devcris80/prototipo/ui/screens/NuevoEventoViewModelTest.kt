package com.devcris80.prototipo.ui.screens

import com.devcris80.prototipo.domain.model.Rol
import com.devcris80.prototipo.domain.model.Usuario
import com.devcris80.prototipo.testing.FakeCuentaRepository
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
    private val cuentaRepository = FakeCuentaRepository()
    private val idAnimal = UUID.randomUUID().toString()
    private val usuario = Usuario(UUID.randomUUID().toString(), UUID.randomUUID().toString(), "Ana", Rol.GANADERO)

    private fun crearViewModel() = NuevoEventoViewModel(idAnimal, eventoRepository, cuentaRepository, clock = { 7L })

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        cuentaRepository.usuario.value = usuario
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
        assertEquals(usuario.idUsuario, eventos[0].idUsuario)
    }

    @Test
    fun guardarSinCompletar_noHaceNada() = runTest {
        val viewModel = crearViewModel()

        viewModel.onGuardar()

        assertEquals(emptyList<Any>(), eventoRepository.observeByAnimal(idAnimal).first())
    }
}
