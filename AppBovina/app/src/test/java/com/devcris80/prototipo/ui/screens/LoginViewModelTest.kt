package com.devcris80.prototipo.ui.screens

import com.devcris80.prototipo.domain.repository.ResultadoInicioSesion
import com.devcris80.prototipo.testing.FakeSesionRepository
import com.devcris80.prototipo.testing.FakeSincronizacionRepository
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
    private val sincronizacionRepository = FakeSincronizacionRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun llenado() = LoginViewModel(sesionRepository, sincronizacionRepository).apply {
        onCorreoCambiado("ana@finca.co")
        onContrasenaCambiada("secreta")
    }

    @Test
    fun sinCorreoOContrasena_noSePuedeIniciar() {
        val viewModel = LoginViewModel(sesionRepository, sincronizacionRepository)
        viewModel.onCorreoCambiado("ana@finca.co")

        assertFalse(viewModel.estado.value.puedeIniciar)
    }

    @Test
    fun credencialesValidas_inicianSesionProgramanLaSincronizacionYEmitenElEvento() = runTest {
        val viewModel = llenado()

        viewModel.onIniciarSesion()

        assertEquals(LoginEvento.SesionIniciada, viewModel.eventos.first())
        assertEquals(listOf("ana@finca.co" to "secreta"), sesionRepository.intentos)
        assertNull(viewModel.estado.value.error)
        assertEquals("trae lo que ya tiene la finca", 1, sincronizacionRepository.programaciones)
    }

    @Test
    fun credencialesIncorrectas_muestranErrorSinNavegar() {
        sesionRepository.resultado = ResultadoInicioSesion.CredencialesInvalidas
        val viewModel = llenado()

        viewModel.onIniciarSesion()

        assertEquals("Correo o contraseña incorrectos.", viewModel.estado.value.error)
        assertFalse(viewModel.estado.value.cargando)
        assertEquals(0, sincronizacionRepository.programaciones)
    }

    @Test
    fun sinSenal_avisaQueHaceFaltaConexion() {
        sesionRepository.resultado = ResultadoInicioSesion.SinConexion
        val viewModel = llenado()

        viewModel.onIniciarSesion()

        assertTrue(viewModel.estado.value.error!!.contains("conexión"))
    }

    @Test
    fun unErrorDelServidor_muestraUnMensajeGenerico() {
        sesionRepository.resultado = ResultadoInicioSesion.NoSePudo
        val viewModel = llenado()

        viewModel.onIniciarSesion()

        assertTrue(viewModel.estado.value.error!!.contains("No se pudo iniciar sesión"))
        assertFalse(viewModel.estado.value.cargando)
    }

    @Test
    fun conDatosDeOtraCuenta_pideDesinstalar() {
        sesionRepository.resultado = ResultadoInicioSesion.OtraCuentaEnElDispositivo
        val viewModel = llenado()

        viewModel.onIniciarSesion()

        assertTrue(viewModel.estado.value.error!!.contains("Desinstala"))
    }
}
