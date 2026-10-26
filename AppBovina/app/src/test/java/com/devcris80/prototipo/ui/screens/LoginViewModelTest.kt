package com.devcris80.prototipo.ui.screens

import com.devcris80.prototipo.domain.repository.ResultadoInicioSesion
import com.devcris80.prototipo.domain.repository.ResultadoRecuperacion
import com.devcris80.prototipo.testing.FakeSesionRepository
import com.devcris80.prototipo.testing.FakeSincronizacionRepository
import kotlinx.coroutines.CompletableDeferred
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

    @Test
    fun olvideMiContrasena_abreElDialogoConElCorreoQueYaEstabaEscrito() {
        val viewModel = llenado()

        viewModel.onOlvideContrasena()

        assertEquals("ana@finca.co", viewModel.estado.value.recuperacion?.correo)
    }

    @Test
    fun enviarLaRecuperacion_pideElCorreoSinEspaciosYConfirmaSinRevelarSiLaCuentaExiste() = runTest {
        val viewModel = LoginViewModel(sesionRepository, sincronizacionRepository)
        viewModel.onOlvideContrasena()
        viewModel.onRecuperacionCorreoCambiado("  ana@finca.co ")

        viewModel.onEnviarRecuperacion()

        assertEquals(listOf("ana@finca.co"), sesionRepository.recuperaciones)
        val recuperacion = viewModel.estado.value.recuperacion!!
        assertTrue(recuperacion.enviado)
        assertFalse(recuperacion.enviando)
        assertEquals(
            "Si el correo está registrado, te enviamos un enlace para recuperar la contraseña.",
            recuperacion.mensaje,
        )
    }

    @Test
    fun sinCorreoNoSeEnviaLaRecuperacion() {
        val viewModel = LoginViewModel(sesionRepository, sincronizacionRepository)
        viewModel.onOlvideContrasena()

        viewModel.onEnviarRecuperacion()

        assertTrue(sesionRepository.recuperaciones.isEmpty())
        assertFalse(viewModel.estado.value.recuperacion!!.puedeEnviar)
    }

    @Test
    fun sinSenal_laRecuperacionLoIndicaYNoDaPorEnviado() {
        sesionRepository.resultadoRecuperacion = ResultadoRecuperacion.SinConexion
        val viewModel = llenado()
        viewModel.onOlvideContrasena()

        viewModel.onEnviarRecuperacion()

        val recuperacion = viewModel.estado.value.recuperacion!!
        assertFalse(recuperacion.enviado)
        assertTrue(recuperacion.mensaje!!.contains("señal"))
    }

    @Test
    fun siElServidorFalla_laRecuperacionPermiteReintentar() {
        sesionRepository.resultadoRecuperacion = ResultadoRecuperacion.NoSePudo
        val viewModel = llenado()
        viewModel.onOlvideContrasena()

        viewModel.onEnviarRecuperacion()

        val recuperacion = viewModel.estado.value.recuperacion!!
        assertFalse(recuperacion.enviado)
        assertTrue(recuperacion.puedeEnviar)
        assertTrue(recuperacion.mensaje!!.contains("Inténtalo"))
    }

    @Test
    fun siSeCierraElDialogoConUnEnvioEnCursoSuResultadoNoLlegaAlDialogoNuevo() = runTest {
        val enCurso = CompletableDeferred<Unit>()
        sesionRepository.pausaRecuperacion = enCurso
        val viewModel = llenado()
        viewModel.onOlvideContrasena()
        viewModel.onEnviarRecuperacion()
        viewModel.onCerrarRecuperacion()
        viewModel.onOlvideContrasena()

        enCurso.complete(Unit)

        val recuperacion = viewModel.estado.value.recuperacion!!
        assertFalse(recuperacion.enviado)
        assertNull(recuperacion.mensaje)
    }

    @Test
    fun cerrarElDialogoLoDescarta() {
        val viewModel = llenado()
        viewModel.onOlvideContrasena()

        viewModel.onCerrarRecuperacion()

        assertNull(viewModel.estado.value.recuperacion)
    }
}
