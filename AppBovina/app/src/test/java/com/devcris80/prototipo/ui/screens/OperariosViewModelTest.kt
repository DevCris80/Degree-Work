package com.devcris80.prototipo.ui.screens

import com.devcris80.prototipo.domain.model.Rol
import com.devcris80.prototipo.domain.model.Usuario
import com.devcris80.prototipo.domain.repository.OperarioDeLaFinca
import com.devcris80.prototipo.domain.repository.ResultadoListaOperarios
import com.devcris80.prototipo.domain.repository.ResultadoQuitarOperario
import com.devcris80.prototipo.domain.usecase.QuitarOperario
import com.devcris80.prototipo.testing.FakeCuentaRepository
import com.devcris80.prototipo.testing.FakeOperarioRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OperariosViewModelTest {
    private val operarioRepository = FakeOperarioRepository()
    private val cuentaRepository = FakeCuentaRepository()
    private val beto = OperarioDeLaFinca("operario-1", "Beto")
    private val carla = OperarioDeLaFinca("operario-2", "Carla")

    private fun crearViewModel() =
        OperariosViewModel(operarioRepository, QuitarOperario(operarioRepository, cuentaRepository))

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        cuentaRepository.usuario.value = Usuario("usuario-1", "perfil_local", "Ana", Rol.GANADERO)
        operarioRepository.lista = ResultadoListaOperarios.Lista(listOf(beto, carla))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun alAbrirLeeLosOperariosDeLaFinca() = runTest {
        val viewModel = crearViewModel()

        assertEquals(listOf(beto, carla), viewModel.estado.value.operarios)
        assertFalse(viewModel.estado.value.cargando)
        assertNull(viewModel.estado.value.error)
    }

    @Test
    fun mientrasLeeLaListaDiceQueEstaCargando() = runTest {
        operarioRepository.espera = CompletableDeferred()

        val viewModel = crearViewModel()

        assertTrue(viewModel.estado.value.cargando)
        assertNull(viewModel.estado.value.operarios)
        operarioRepository.espera!!.complete(Unit)
        assertFalse(viewModel.estado.value.cargando)
    }

    @Test
    fun sinSenalLoDiceYPermiteReintentar() = runTest {
        operarioRepository.lista = ResultadoListaOperarios.SinConexion
        val viewModel = crearViewModel()
        assertTrue(viewModel.estado.value.error!!.startsWith("Sin señal"))
        assertNull(viewModel.estado.value.operarios)

        operarioRepository.lista = ResultadoListaOperarios.Lista(listOf(beto))
        viewModel.onReintentar()

        assertEquals(listOf(beto), viewModel.estado.value.operarios)
        assertNull(viewModel.estado.value.error)
    }

    @Test
    fun unErrorInesperadoAlLeerLoDice() = runTest {
        operarioRepository.lista = ResultadoListaOperarios.NoSePudo

        assertNotNull(crearViewModel().estado.value.error)
    }

    @Test
    fun quitarAUnOperarioLoSacaDeLaListaYAvisa() = runTest {
        val viewModel = crearViewModel()

        viewModel.onQuitar(beto)

        assertEquals(listOf("operario-1"), operarioRepository.quitados)
        assertEquals(listOf(carla), viewModel.estado.value.operarios)
        assertEquals("Se quitó a Beto de la finca.", viewModel.estado.value.aviso)
        assertFalse(viewModel.estado.value.quitando)
        viewModel.onAvisoMostrado()
        assertNull(viewModel.estado.value.aviso)
    }

    @Test
    fun sinSenalAlQuitarNoCambiaLaListaYLoDice() = runTest {
        operarioRepository.resultadoQuitar = ResultadoQuitarOperario.SinConexion
        val viewModel = crearViewModel()

        viewModel.onQuitar(beto)

        assertEquals(listOf(beto, carla), viewModel.estado.value.operarios)
        assertTrue(viewModel.estado.value.error!!.startsWith("Sin señal"))
        assertFalse(viewModel.estado.value.quitando)
    }

    @Test
    fun siElOperarioYaNoEstabaRecargaLaLista() = runTest {
        val viewModel = crearViewModel()
        operarioRepository.resultadoQuitar = ResultadoQuitarOperario.NoDisponible
        operarioRepository.lista = ResultadoListaOperarios.Lista(listOf(carla))

        viewModel.onQuitar(beto)

        assertEquals(listOf(carla), viewModel.estado.value.operarios)
        assertEquals("Beto ya no estaba en la finca.", viewModel.estado.value.aviso)
    }

    @Test
    fun unOperarioNoQuitaANadie() = runTest {
        cuentaRepository.usuario.value = Usuario("usuario-2", "perfil_local", "Beto", Rol.OPERARIO)
        val viewModel = crearViewModel()

        viewModel.onQuitar(carla)

        assertTrue(operarioRepository.quitados.isEmpty())
        assertEquals(listOf(beto, carla), viewModel.estado.value.operarios)
        assertNotNull(viewModel.estado.value.error)
    }
}
