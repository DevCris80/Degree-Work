package com.devcris80.prototipo.ui.nav

import com.devcris80.prototipo.domain.model.Rol
import com.devcris80.prototipo.domain.model.Usuario
import com.devcris80.prototipo.testing.FakeCuentaRepository
import com.devcris80.prototipo.testing.FakeSesionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class InicioViewModelTest {
    private val cuentaRepository = FakeCuentaRepository()
    private val sesionRepository = FakeSesionRepository()
    private val usuario = Usuario("usuario-1", "finca-1", "Ana", Rol.GANADERO)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun conSesionGuardadaYUsuario_entraSinPedirInicioDeSesion() {
        sesionRepository.guardada.value = true
        cuentaRepository.usuario.value = usuario

        assertEquals(true, InicioViewModel(cuentaRepository, sesionRepository).puedeEntrar.value)
    }

    @Test
    fun conUnUsuarioLocalPeroSinSesion_pideIniciarSesion() {
        cuentaRepository.usuario.value = usuario

        assertEquals(false, InicioViewModel(cuentaRepository, sesionRepository).puedeEntrar.value)
    }

    // La pantalla de inicio se decide una vez al abrir la app. Si cambiara con la sesión, iniciar
    // sesión en un celular con otro Usuario local saltaría a la lista y volvería al Login.
    @Test
    fun laDecisionSeTomaAlAbrirYNoCambiaConLaSesion() {
        cuentaRepository.usuario.value = usuario
        val viewModel = InicioViewModel(cuentaRepository, sesionRepository)

        sesionRepository.guardada.value = true

        assertEquals(false, viewModel.puedeEntrar.value)
    }

    // Cerrar sesión, o que el servidor rechace renovarla, saca de la app aunque ya esté abierta.
    @Test
    fun cuandoLaSesionGuardadaDesaparece_avisaQueSePerdio() = runTest {
        sesionRepository.guardada.value = true
        val viewModel = InicioViewModel(cuentaRepository, sesionRepository)
        val avisos = mutableListOf<Unit>()
        val job = launch { viewModel.sesionPerdida.collect { avisos.add(it) } }
        runCurrent()

        sesionRepository.guardada.value = false
        runCurrent()

        assertEquals(1, avisos.size)
        job.cancel()
    }
}
