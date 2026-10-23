package com.devcris80.prototipo.ui.screens

import com.devcris80.prototipo.domain.repository.ResultadoInicioSesion
import com.devcris80.prototipo.testing.FakeSesionRepository
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

class LoginViewModelTest {
    private val sesionRepository = FakeSesionRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun llenado() = LoginViewModel(sesionRepository).apply {
        onCorreoCambiado("ana@finca.co")
        onContrasenaCambiada("secreta")
    }

    @Test
    fun sinCorreoOContrasena_noSePuedeIniciar() {
        val viewModel = LoginViewModel(sesionRepository)
        viewModel.onCorreoCambiado("ana@finca.co")

        assertFalse(viewModel.estado.value.puedeIniciar)
    }

    @Test
    fun credencialesValidas_inicianSesionYEmitenElEvento() = runTest {
        val viewModel = llenado()

        viewModel.onIniciarSesion()

        assertEquals(LoginEvento.SesionIniciada, viewModel.eventos.first())
        assertEquals(listOf("ana@finca.co" to "secreta"), sesionRepository.intentos)
        assertNull(viewModel.estado.value.error)
    }

    @Test
    fun credencialesIncorrectas_muestranErrorSinNavegar() {
        sesionRepository.resultado = ResultadoInicioSesion.CredencialesInvalidas
        val viewModel = llenado()

        viewModel.onIniciarSesion()

        assertEquals("Correo o contraseña incorrectos.", viewModel.estado.value.error)
        assertFalse(viewModel.estado.value.cargando)
    }

    @Test
    fun sinSenal_avisaQueHaceFaltaConexion() {
        sesionRepository.resultado = ResultadoInicioSesion.SinConexion
        val viewModel = llenado()

        viewModel.onIniciarSesion()

        assertTrue(viewModel.estado.value.error!!.contains("conexión"))
    }

    @Test
    fun conDatosDeOtraFinca_pideDesinstalar() {
        sesionRepository.resultado = ResultadoInicioSesion.OtraFincaEnElDispositivo
        val viewModel = llenado()

        viewModel.onIniciarSesion()

        assertTrue(viewModel.estado.value.error!!.contains("Desinstala"))
    }
}
