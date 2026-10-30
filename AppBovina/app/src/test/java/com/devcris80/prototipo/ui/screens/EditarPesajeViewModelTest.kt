package com.devcris80.prototipo.ui.screens

import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.model.Registro
import com.devcris80.prototipo.domain.model.Rol
import com.devcris80.prototipo.domain.model.Sexo
import com.devcris80.prototipo.domain.model.Usuario
import com.devcris80.prototipo.domain.usecase.ActualizarRegistro
import com.devcris80.prototipo.domain.usecase.DarDeBajaRegistro
import com.devcris80.prototipo.testing.FakeAnimalRepository
import com.devcris80.prototipo.testing.FakeCuentaRepository
import com.devcris80.prototipo.testing.FakeRegistroRepository
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EditarPesajeViewModelTest {
    private val animalRepository = FakeAnimalRepository()
    private val registroRepository = FakeRegistroRepository()
    private val cuentaRepository = FakeCuentaRepository()

    private fun crearViewModel(idRegistro: String = "registro-1") = EditarPesajeViewModel(
        idRegistro,
        registroRepository,
        cuentaRepository,
        ActualizarRegistro(animalRepository, registroRepository, FakeTransaccion(), clock = { AHORA }),
        DarDeBajaRegistro(registroRepository, cuentaRepository, clock = { AHORA }),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        conUsuario(Rol.OPERARIO)
        animalRepository.set(
            Animal("animal-1", "perfil_local", "Manchas", "Holstein", Sexo.HEMBRA, Etapa.VACA, 1L, proposito = Proposito.LECHE),
        )
        registroRepository.set(
            "animal-1",
            listOf(Registro("registro-1", "animal-1", "usuario-1", "lectura-1", 312.5f, 1_000L)),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun conUsuario(rol: Rol) {
        cuentaRepository.usuario.value = Usuario("usuario-1", "perfil_local", "Ana", rol)
    }

    @Test
    fun parteDelPesoYLaHoraGuardados() = runTest {
        val viewModel = crearViewModel()

        assertEquals("312.5", viewModel.estado.value.peso)
        assertEquals(1_000L, viewModel.estado.value.fecha)
        assertTrue(viewModel.estado.value.cargado)
    }

    @Test
    fun unPesoEnteroSeMuestraSinDecimales() = runTest {
        registroRepository.set("animal-1", listOf(Registro("registro-1", "animal-1", "usuario-1", null, 300f, 1_000L)))

        assertEquals("300", crearViewModel().estado.value.peso)
    }

    @Test
    fun guardarMuestraElValorNuevoEnElHistorialYEmiteGuardado() = runTest {
        val viewModel = crearViewModel()
        viewModel.onPesoCambiado("340.5")
        viewModel.onFechaCambiada(2_000L)

        viewModel.onGuardar()

        assertEquals(EditarPesajeEvento.Guardado, viewModel.eventos.first())
        val registro = registroRepository.observeByAnimal("animal-1").first().single()
        assertEquals(340.5f, registro.peso)
        assertEquals(2_000L, registro.timestamp)
        assertEquals("lectura-1", registro.idLectura)
    }

    @Test
    fun unPesoFueraDeRangoNoSeGuardaYMuestraElError() = runTest {
        val viewModel = crearViewModel()
        viewModel.onPesoCambiado("2000")

        viewModel.onGuardar()

        assertEquals(NuevoPesajeError.PesoFueraDeRango, viewModel.estado.value.error)
        assertFalse(viewModel.estado.value.guardando)
        assertEquals(312.5f, registroRepository.observeByAnimal("animal-1").first().single().peso)
    }

    @Test
    fun cambiarElPesoLimpiaElError() = runTest {
        val viewModel = crearViewModel()
        viewModel.onPesoCambiado("2000")
        viewModel.onGuardar()

        viewModel.onPesoCambiado("340")

        assertNull(viewModel.estado.value.error)
    }

    @Test
    fun soloAceptaDigitosYUnPuntoDecimal() = runTest {
        val viewModel = crearViewModel()

        viewModel.onPesoCambiado("12a")
        viewModel.onPesoCambiado("1,5")

        assertEquals("312.5", viewModel.estado.value.peso)
    }

    @Test
    fun unPesajeInexistenteODadoDeBajaNoSeCargaNiSeGuarda() = runTest {
        val viewModel = crearViewModel("no-existe")

        assertEquals(NuevoPesajeError.PesajeNoDisponible, viewModel.estado.value.error)
        assertFalse(viewModel.estado.value.cargado)
        assertFalse(viewModel.estado.value.puedeGuardar)
    }

    @Test
    fun elOperarioPuedeEditarPeroNoSeLeOfreceDarDeBaja() = runTest {
        val viewModel = crearViewModel()

        assertFalse(viewModel.estado.value.puedeDarDeBaja)
        viewModel.onDarDeBaja()

        assertEquals(1, registroRepository.observeByAnimal("animal-1").first().size)
    }

    @Test
    fun elGanaderoDaDeBajaElPesajeYDejaDeVerseEnElHistorialPeroSeConserva() = runTest {
        conUsuario(Rol.GANADERO)
        val viewModel = crearViewModel()
        assertTrue(viewModel.estado.value.puedeDarDeBaja)

        viewModel.onDarDeBaja()

        assertEquals(EditarPesajeEvento.DadoDeBaja, viewModel.eventos.first())
        assertTrue(registroRepository.observeByAnimal("animal-1").first().isEmpty())
        assertEquals(AHORA, registroRepository.findById("registro-1")!!.fechaBaja)
    }

    private companion object {
        const val AHORA = 10_000L
    }
}
