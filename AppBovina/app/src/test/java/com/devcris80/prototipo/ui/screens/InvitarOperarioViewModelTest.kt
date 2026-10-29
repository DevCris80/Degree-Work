package com.devcris80.prototipo.ui.screens

import com.devcris80.prototipo.domain.repository.Invitacion
import com.devcris80.prototipo.domain.repository.ResultadoInvitacion
import com.devcris80.prototipo.testing.FakeInvitacionRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InvitarOperarioViewModelTest {
    private val invitacionRepository = FakeInvitacionRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun alAbrirNoHayCodigoNiSeLlamaAlServidor() {
        val viewModel = InvitarOperarioViewModel(invitacionRepository)

        assertNull(viewModel.estado.value.invitacion)
        assertEquals(0, invitacionRepository.generaciones)
    }

    @Test
    fun generarMuestraElCodigoConSuVencimiento() {
        val invitacion = Invitacion("QWER5678", 1_800_000_000_000)
        invitacionRepository.resultado = ResultadoInvitacion.Generada(invitacion)
        val viewModel = InvitarOperarioViewModel(invitacionRepository)

        viewModel.onGenerar()

        assertEquals(invitacion, viewModel.estado.value.invitacion)
        assertNull(viewModel.estado.value.error)
        assertFalse(viewModel.estado.value.generando)
    }

    @Test
    fun mientrasGeneraNoSePuedePedirOtroCodigo() {
        invitacionRepository.espera = CompletableDeferred()
        val viewModel = InvitarOperarioViewModel(invitacionRepository)

        viewModel.onGenerar()
        viewModel.onGenerar()

        assertTrue(viewModel.estado.value.generando)
        assertEquals(1, invitacionRepository.generaciones)

        invitacionRepository.espera?.complete(Unit)

        assertFalse(viewModel.estado.value.generando)
    }

    @Test
    fun sinSenalLoDiceYNoMuestraCodigo() {
        invitacionRepository.resultado = ResultadoInvitacion.SinConexion
        val viewModel = InvitarOperarioViewModel(invitacionRepository)

        viewModel.onGenerar()

        assertTrue(viewModel.estado.value.error.orEmpty().contains("señal"))
        assertNull(viewModel.estado.value.invitacion)
    }

    @Test
    fun cadaRespuestaDelServidorTieneSuMensaje() {
        val mensajes = listOf(
            ResultadoInvitacion.SinConexion,
            ResultadoInvitacion.NoPermitido,
            ResultadoInvitacion.NoSePudo,
        ).map { resultado ->
            invitacionRepository.resultado = resultado
            InvitarOperarioViewModel(invitacionRepository).apply { onGenerar() }.estado.value.error
        }

        assertTrue(mensajes.all { !it.isNullOrBlank() })
        assertEquals(3, mensajes.toSet().size)
    }

    @Test
    fun siFallaUnNuevoIntentoNoBorraElCodigoAnterior() {
        val viewModel = InvitarOperarioViewModel(invitacionRepository)
        viewModel.onGenerar()
        val primero = viewModel.estado.value.invitacion

        invitacionRepository.resultado = ResultadoInvitacion.SinConexion
        viewModel.onGenerar()

        assertEquals(primero, viewModel.estado.value.invitacion)
        assertTrue(viewModel.estado.value.error != null)
    }
}
