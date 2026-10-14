package com.devcris80.prototipo.ui.screens

import com.devcris80.prototipo.domain.model.Chapeta
import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.PerfilFinca
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.model.Sexo
import com.devcris80.prototipo.domain.usecase.RegistrarAnimal
import com.devcris80.prototipo.testing.FakeAnimalRepository
import com.devcris80.prototipo.testing.FakeChapetaRepository
import com.devcris80.prototipo.testing.FakeCuentaRepository
import com.devcris80.prototipo.testing.FakePesajePendienteRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class RegistrarAnimalViewModelTest {
    private val cuentaRepository = FakeCuentaRepository()
    private val animalRepository = FakeAnimalRepository()
    private val chapetaRepository = FakeChapetaRepository()

    private fun crearViewModel(codigoPrecargado: String? = null, avisoAnterior: String? = null) =
        RegistrarAnimalViewModel(
            codigoPrecargado,
            avisoAnterior,
            cuentaRepository,
            RegistrarAnimal(animalRepository, chapetaRepository, FakePesajePendienteRepository(), FakeTransaccion()),
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        cuentaRepository.perfil.value = PerfilFinca(idPerfilFinca = "perfil_local", nombreFinca = "Finca Test")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun llenarFormularioValido(viewModel: RegistrarAnimalViewModel) {
        viewModel.onNombreCambiado("Manchas")
        viewModel.onRazaCambiada("Holstein")
        viewModel.onSexoSeleccionado(Sexo.HEMBRA)
        viewModel.onEtapaSeleccionada(Etapa.VACA)
        viewModel.onFechaNacimientoCambiada(1L)
        viewModel.onPropositoSeleccionado(Proposito.LECHE)
    }

    @Test
    fun codigoPrecargado_quedaDeSoloLecturaConElAviso() {
        val viewModel = crearViewModel(codigoPrecargado = "04A1B2C3", avisoAnterior = "Este tag estuvo asociado a Canela")

        val estado = viewModel.estado.value
        assertEquals("04A1B2C3", estado.codigo)
        assertTrue(estado.codigoSoloLectura)
        assertEquals("Este tag estuvo asociado a Canela", estado.aviso)
    }

    @Test
    fun cambiarSexo_actualizaEtapasDisponiblesYLimpiaLaEtapaElegida() {
        val viewModel = crearViewModel()
        viewModel.onSexoSeleccionado(Sexo.MACHO)
        viewModel.onEtapaSeleccionada(Etapa.TORO)

        viewModel.onSexoSeleccionado(Sexo.HEMBRA)

        val estado = viewModel.estado.value
        assertEquals(listOf(Etapa.VACA, Etapa.TERNERA), estado.etapasDisponibles)
        assertEquals(null, estado.etapa)
    }

    @Test
    fun formularioIncompleto_noSePuedeGuardar() {
        val viewModel = crearViewModel()
        viewModel.onNombreCambiado("Manchas")

        assertFalse(viewModel.estado.value.puedeGuardar)
    }

    @Test
    fun guardarSinCodigo_emiteGuardadoYNoCreaChapeta() = runTest {
        val viewModel = crearViewModel()
        llenarFormularioValido(viewModel)

        viewModel.onGuardar()

        assertEquals(RegistrarAnimalEvento.Guardado, viewModel.eventos.first())
        assertTrue(chapetaRepository.observeActivas().first().isEmpty())
    }

    @Test
    fun guardarConCodigoYaActivo_noGuardaYMarcaElEstado() = runTest {
        val otroAnimal = UUID.randomUUID().toString()
        chapetaRepository.set(Chapeta(UUID.randomUUID().toString(), "04A1B2C3", otroAnimal, fechaAsociacion = 1L))
        val viewModel = crearViewModel(codigoPrecargado = "04A1B2C3")
        llenarFormularioValido(viewModel)

        viewModel.onGuardar()

        assertTrue(viewModel.estado.value.codigoYaActivo)
    }
}
