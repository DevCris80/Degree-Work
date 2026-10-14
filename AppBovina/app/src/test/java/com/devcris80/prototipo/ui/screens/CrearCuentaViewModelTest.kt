package com.devcris80.prototipo.ui.screens

import com.devcris80.prototipo.domain.model.Rol
import com.devcris80.prototipo.domain.usecase.CrearCuenta
import com.devcris80.prototipo.testing.FakeCuentaRepository
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
import org.junit.Before
import org.junit.Test

class CrearCuentaViewModelTest {
    private val cuentaRepository = FakeCuentaRepository()

    private fun crearViewModel() = CrearCuentaViewModel(CrearCuenta(cuentaRepository, FakeTransaccion()))

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun sinNombreNiFinca_noSePuedeCrear() {
        assertFalse(crearViewModel().estado.value.puedeCrear)
    }

    @Test
    fun conNombreYFinca_sePuedeCrear() {
        val viewModel = crearViewModel()
        viewModel.onNombreGanaderoCambiado("Ana")
        viewModel.onNombreFincaCambiado("Finca Test")

        assertEquals(true, viewModel.estado.value.puedeCrear)
    }

    @Test
    fun crearCuenta_guardaLaFincaYElUsuarioYEmiteCuentaCreada() = runTest {
        val viewModel = crearViewModel()
        viewModel.onNombreGanaderoCambiado("Ana")
        viewModel.onNombreFincaCambiado("Finca Test")

        viewModel.onCrearCuenta()

        assertEquals(CrearCuentaEvento.CuentaCreada, viewModel.eventos.first())
        assertEquals("Finca Test", cuentaRepository.getPerfilFinca()?.nombreFinca)
        val usuario = cuentaRepository.getUsuario()
        assertEquals("Ana", usuario?.nombre)
        assertEquals(Rol.GANADERO, usuario?.rol)
    }

    @Test
    fun crearCuentaIncompleta_noHaceNada() = runTest {
        val viewModel = crearViewModel()

        viewModel.onCrearCuenta()

        assertEquals(null, cuentaRepository.getPerfilFinca())
    }
}
