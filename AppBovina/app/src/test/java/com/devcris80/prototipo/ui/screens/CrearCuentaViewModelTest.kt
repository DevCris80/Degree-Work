package com.devcris80.prototipo.ui.screens

import com.devcris80.prototipo.domain.model.AutorizacionDatos
import com.devcris80.prototipo.domain.repository.ResultadoCreacionCuenta
import com.devcris80.prototipo.domain.usecase.CrearCuenta
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

class CrearCuentaViewModelTest {
    private val sesionRepository = FakeSesionRepository()

    private fun crearViewModel() = CrearCuentaViewModel(CrearCuenta(sesionRepository))

    private fun llenado(acepta: Boolean = true) = crearViewModel().apply {
        onNombreGanaderoCambiado("Ana")
        onNombreFincaCambiado("La Esperanza")
        onCorreoCambiado("ana@finca.co")
        onContrasenaCambiada("secreta")
        onConfirmacionCambiada("secreta")
        onAutorizacionCambiada(acepta)
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun sinAceptarLaAutorizacionNoSePuedeCrearAunqueTodoLoDemasEsteBien() {
        val viewModel = llenado(acepta = false)

        assertFalse(viewModel.estado.value.puedeCrear)

        viewModel.onAutorizacionCambiada(true)

        assertTrue(viewModel.estado.value.puedeCrear)
    }

    @Test
    fun unaContrasenaCortaOUnaConfirmacionDistintaNoDejanCrear() {
        val viewModel = llenado()

        viewModel.onContrasenaCambiada("12345")
        viewModel.onConfirmacionCambiada("12345")
        assertFalse(viewModel.estado.value.puedeCrear)

        viewModel.onContrasenaCambiada("secreta")
        viewModel.onConfirmacionCambiada("secretA")
        assertFalse(viewModel.estado.value.puedeCrear)
    }

    @Test
    fun crearLaCuentaLlamaAlServidorConLaVersionAceptadaYAvisaQueSeCreo() = runTest {
        val viewModel = llenado()

        viewModel.onCrearCuenta()

        assertEquals(CrearCuentaEvento.CuentaCreada, viewModel.eventos.first())
        val creacion = sesionRepository.creaciones.single()
        assertEquals("ana@finca.co", creacion.correo)
        assertEquals(AutorizacionDatos.VERSION, creacion.versionAutorizacion)
        assertNull(viewModel.estado.value.error)
    }

    @Test
    fun sinAceptarCrearNoHaceNada() {
        val viewModel = llenado(acepta = false)

        viewModel.onCrearCuenta()

        assertTrue(sesionRepository.creaciones.isEmpty())
    }

    @Test
    fun sinSenalLoIndicaYDejaReintentar() {
        sesionRepository.resultadoCreacion = ResultadoCreacionCuenta.SinConexion
        val viewModel = llenado()

        viewModel.onCrearCuenta()

        val estado = viewModel.estado.value
        assertTrue(estado.error!!.contains("señal"))
        assertFalse(estado.cargando)
        assertTrue(estado.puedeCrear)
    }

    @Test
    fun conUnCorreoYaRegistradoLoIndicaYOfreceIniciarSesion() {
        sesionRepository.resultadoCreacion = ResultadoCreacionCuenta.CorreoYaRegistrado
        val viewModel = llenado()

        viewModel.onCrearCuenta()

        assertTrue(viewModel.estado.value.ofrecerIniciarSesion)
        assertTrue(viewModel.estado.value.error!!.contains("ya tiene una cuenta"))
    }

    @Test
    fun cambiarElCorreoQuitaElErrorYLaOfertaDeIniciarSesion() {
        sesionRepository.resultadoCreacion = ResultadoCreacionCuenta.CorreoYaRegistrado
        val viewModel = llenado()
        viewModel.onCrearCuenta()

        viewModel.onCorreoCambiado("otra@finca.co")

        assertNull(viewModel.estado.value.error)
        assertFalse(viewModel.estado.value.ofrecerIniciarSesion)
    }

    @Test
    fun cadaRespuestaDelServidorTieneSuMensaje() {
        val mensajes = listOf(
            ResultadoCreacionCuenta.ContrasenaRechazada,
            ResultadoCreacionCuenta.OtraCuentaEnElDispositivo,
            ResultadoCreacionCuenta.NoSePudo,
        ).map { resultado ->
            sesionRepository.resultadoCreacion = resultado
            llenado().apply { onCrearCuenta() }.estado.value.error
        }

        assertTrue(mensajes.all { !it.isNullOrBlank() })
        assertEquals("cada caso dice algo distinto", 3, mensajes.toSet().size)
    }

    private fun conCodigo(codigo: String = "ABCD2345") = crearViewModel().apply {
        onNombreGanaderoCambiado("Luis")
        onCorreoCambiado("luis@finca.co")
        onContrasenaCambiada("secreta")
        onConfirmacionCambiada("secreta")
        onAutorizacionCambiada(true)
        onTengoCodigoCambiado(true)
        onCodigoInvitacionCambiado(codigo)
    }

    @Test
    fun conCodigoNoPideElNombreDeLaFincaPeroSiElCodigo() {
        val viewModel = conCodigo(codigo = "")

        assertFalse(viewModel.estado.value.puedeCrear)

        viewModel.onCodigoInvitacionCambiado("ABCD2345")

        assertTrue(viewModel.estado.value.puedeCrear)
        assertEquals("", viewModel.estado.value.nombreFinca)
    }

    @Test
    fun conCodigoSeUneALaFincaYAvisaQueLaCuentaSeCreo() = runTest {
        val viewModel = conCodigo()

        viewModel.onCrearCuenta()

        assertEquals(CrearCuentaEvento.CuentaCreada, viewModel.eventos.first())
        assertEquals("ABCD2345", sesionRepository.uniones.single().codigoInvitacion)
        assertTrue(sesionRepository.creaciones.isEmpty())
    }

    @Test
    fun unCodigoRechazadoDiceQueNoSirveYNoOfreceIniciarSesion() {
        sesionRepository.resultadoCreacion = ResultadoCreacionCuenta.CodigoRechazado
        val viewModel = conCodigo()

        viewModel.onCrearCuenta()

        assertTrue(viewModel.estado.value.error.orEmpty().contains("código"))
        assertFalse(viewModel.estado.value.ofrecerIniciarSesion)
        assertFalse(viewModel.estado.value.cargando)
    }

    @Test
    fun cambiarElCodigoLimpiaElError() {
        sesionRepository.resultadoCreacion = ResultadoCreacionCuenta.CodigoRechazado
        val viewModel = conCodigo().apply { onCrearCuenta() }

        viewModel.onCodigoInvitacionCambiado("OTROCOD2")

        assertNull(viewModel.estado.value.error)
    }

    @Test
    fun sinSenalConCodigoLoDiceYNoQuedaNadaCreado() {
        sesionRepository.resultadoCreacion = ResultadoCreacionCuenta.SinConexion
        val viewModel = conCodigo()

        viewModel.onCrearCuenta()

        assertTrue(viewModel.estado.value.error.orEmpty().contains("señal"))
        assertFalse(sesionRepository.guardada.value)
    }
}
