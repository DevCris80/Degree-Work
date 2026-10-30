package com.devcris80.prototipo.ui.screens

import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.model.Registro
import com.devcris80.prototipo.domain.model.Rol
import com.devcris80.prototipo.domain.model.Sexo
import com.devcris80.prototipo.domain.model.Usuario
import com.devcris80.prototipo.domain.usecase.RegistrarPesajeManual
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.UUID

class NuevoPesajeViewModelTest {
    private val animalRepository = FakeAnimalRepository()
    private val registroRepository = FakeRegistroRepository()
    private val cuentaRepository = FakeCuentaRepository()
    private val usuario = Usuario(UUID.randomUUID().toString(), "perfil_local", "Ana", Rol.OPERARIO)
    private val vaca = Animal(
        idAnimal = UUID.randomUUID().toString(),
        idPerfilFinca = "perfil_local",
        nombre = "Lucero",
        raza = "Brahman",
        sexo = Sexo.HEMBRA,
        etapa = Etapa.VACA,
        fechaNacimiento = 1L,
        proposito = Proposito.LECHE,
    )

    private fun crearViewModel() = NuevoPesajeViewModel(
        vaca.idAnimal,
        RegistrarPesajeManual(
            animalRepository,
            registroRepository,
            cuentaRepository,
            FakeTransaccion(),
            clock = { AHORA },
        ),
        clock = { AHORA },
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        cuentaRepository.usuario.value = usuario
        animalRepository.set(vaca)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun registros() = registroRepository.observeByAnimal(vaca.idAnimal).first()

    @Test
    fun laFechaDeLaMedicionEmpiezaEnAhora() {
        assertEquals(AHORA, crearViewModel().estado.value.fecha)
    }

    @Test
    fun conUnPesoValido_guardaElPesajeConLaFechaIngresadaANombreDelUsuario() = runTest {
        val viewModel = crearViewModel()
        viewModel.onPesoCambiado("432.5")
        viewModel.onFechaCambiada(AHORA - 3_600_000)

        viewModel.onGuardar()

        assertEquals(NuevoPesajeEvento.Guardado, viewModel.eventos.first())
        val registro = registros().single()
        assertEquals(432.5f, registro.peso)
        assertEquals(AHORA - 3_600_000, registro.timestamp)
        assertEquals(usuario.idUsuario, registro.idUsuario)
        assertNull(registro.idLectura)
        assertNull(viewModel.estado.value.error)
    }

    @Test
    fun conPesoCeroOMayorA1500_noGuardaYLoIndica() = runTest {
        val viewModel = crearViewModel()
        for (peso in listOf("0", "1500.1", ".")) {
            viewModel.onPesoCambiado(peso)
            viewModel.onGuardar()

            assertEquals(PesajeError.PesoFueraDeRango, viewModel.estado.value.error)
        }
        assertEquals(emptyList<Registro>(), registros())
    }

    @Test
    fun elPesoSoloAceptaDigitosYUnPuntoDecimal() {
        val viewModel = crearViewModel()
        viewModel.onPesoCambiado("1500")

        for (rechazado in listOf("1,500", "1.500.5", "15a", "-3")) viewModel.onPesoCambiado(rechazado)

        assertEquals("1500", viewModel.estado.value.peso)
    }

    @Test
    fun conExactamente1500kg_seGuarda() = runTest {
        val viewModel = crearViewModel()
        viewModel.onPesoCambiado("1500")

        viewModel.onGuardar()

        assertEquals(1500f, registros().single().peso)
    }

    @Test
    fun unDobleToqueEnGuardar_guardaUnSoloPesaje() = runTest {
        val viewModel = crearViewModel()
        viewModel.onPesoCambiado("400")

        viewModel.onGuardar()
        viewModel.onGuardar()

        assertEquals(1, registros().size)
    }

    @Test
    fun conFechaFutura_noGuardaYLoIndica() = runTest {
        val viewModel = crearViewModel()
        viewModel.onPesoCambiado("400")
        viewModel.onFechaCambiada(AHORA + 60_000)

        viewModel.onGuardar()

        assertEquals(PesajeError.FechaFutura, viewModel.estado.value.error)
        assertEquals(emptyList<Registro>(), registros())
    }

    @Test
    fun aUnAnimalDadoDeBaja_noSeLeRegistraPesaje() = runTest {
        animalRepository.set(vaca.copy(fechaBaja = 1L))
        val viewModel = crearViewModel()
        viewModel.onPesoCambiado("400")

        viewModel.onGuardar()

        assertEquals(PesajeError.AnimalNoDisponible, viewModel.estado.value.error)
        assertEquals(emptyList<Registro>(), registros())
    }

    private companion object {
        const val AHORA = 1_000_000_000L
    }
}
