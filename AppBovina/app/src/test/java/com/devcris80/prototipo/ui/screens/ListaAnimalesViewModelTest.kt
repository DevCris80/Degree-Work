package com.devcris80.prototipo.ui.screens

import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Chapeta
import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.MotivoPesajePendiente
import com.devcris80.prototipo.domain.model.PerfilFinca
import com.devcris80.prototipo.domain.model.PesajePendiente
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.model.Registro
import com.devcris80.prototipo.domain.model.Rol
import com.devcris80.prototipo.domain.model.Sexo
import com.devcris80.prototipo.domain.model.Usuario
import com.devcris80.prototipo.domain.usecase.ObservarPesajesPendientes
import com.devcris80.prototipo.testing.FakeAnimalRepository
import com.devcris80.prototipo.testing.FakeChapetaRepository
import com.devcris80.prototipo.testing.FakeCuentaRepository
import com.devcris80.prototipo.testing.FakePesajePendienteRepository
import com.devcris80.prototipo.testing.FakeRegistroRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.UUID

class ListaAnimalesViewModelTest {
    private val cuentaRepository = FakeCuentaRepository()
    private val animalRepository = FakeAnimalRepository()
    private val chapetaRepository = FakeChapetaRepository()
    private val registroRepository = FakeRegistroRepository()
    private val pesajePendienteRepository = FakePesajePendienteRepository()

    private fun crearViewModel() = ListaAnimalesViewModel(
        cuentaRepository,
        animalRepository,
        chapetaRepository,
        registroRepository,
        ObservarPesajesPendientes(pesajePendienteRepository, chapetaRepository, animalRepository),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun animal(nombre: String, proposito: Proposito = Proposito.LECHE) = Animal(
        idAnimal = UUID.randomUUID().toString(),
        idPerfilFinca = "perfil_local",
        nombre = nombre,
        raza = "Holstein",
        sexo = Sexo.HEMBRA,
        etapa = Etapa.VACA,
        fechaNacimiento = 1L,
        proposito = proposito,
    )

    @Test
    fun combinaFincaUsuarioAnimalesChapetaYUltimoRegistro() = runTest {
        val manchas = animal("Manchas")
        cuentaRepository.perfil.value = PerfilFinca(idPerfilFinca = "perfil_local", nombreFinca = "Finca Test")
        cuentaRepository.usuario.value = Usuario(UUID.randomUUID().toString(), "perfil_local", "Ana", Rol.GANADERO)
        animalRepository.activos.value = listOf(manchas)
        chapetaRepository.set(Chapeta(UUID.randomUUID().toString(), "04A1B2C3", manchas.idAnimal, fechaAsociacion = 1L))
        val registro = Registro(UUID.randomUUID().toString(), manchas.idAnimal, "usuario", peso = 320f, timestamp = 1L)
        registroRepository.setUltimo(manchas.idAnimal, registro)

        val viewModel = crearViewModel()
        val estados = mutableListOf<ListaAnimalesUiState>()
        val job = launch { viewModel.estado.collect { estados.add(it) } }
        advanceUntilIdle()

        val ultimo = estados.last()
        assertEquals("Finca Test", ultimo.nombreFinca)
        assertEquals("Ana", ultimo.nombreUsuario)
        assertEquals(1, ultimo.animales.size)
        assertEquals("04A1B2C3", ultimo.animales[0].codigoChapeta)
        assertEquals(registro, ultimo.animales[0].ultimoRegistro)
        job.cancel()
    }

    @Test
    fun onFiltroSeleccionado_filtraLosAnimalesPorProposito() = runTest {
        val leche = animal("Manchas", Proposito.LECHE)
        val carne = animal("Toro", Proposito.CARNE)
        animalRepository.activos.value = listOf(leche, carne)

        val viewModel = crearViewModel()
        val estados = mutableListOf<ListaAnimalesUiState>()
        val job = launch { viewModel.estado.collect { estados.add(it) } }
        advanceUntilIdle()

        viewModel.onFiltroSeleccionado(Proposito.CARNE)
        advanceUntilIdle()

        val ultimo = estados.last()
        assertEquals(Proposito.CARNE, ultimo.filtro)
        assertEquals(listOf(carne.idAnimal), ultimo.animales.map { it.animal.idAnimal })
        job.cancel()
    }

    @Test
    fun conPesajesPendientesSinResolver_elEstadoLosCuenta() = runTest {
        pesajePendienteRepository.set(
            PesajePendiente(
                idPesajePendiente = UUID.randomUUID().toString(),
                idLectura = UUID.randomUUID().toString(),
                codigo = "04A1B2C3",
                peso = 100f,
                timestamp = 1L,
                idUsuario = UUID.randomUUID().toString(),
                motivo = MotivoPesajePendiente.CODIGO_SIN_CHAPETA_ACTIVA,
            ),
        )

        val viewModel = crearViewModel()
        val estados = mutableListOf<ListaAnimalesUiState>()
        val job = launch { viewModel.estado.collect { estados.add(it) } }
        advanceUntilIdle()

        assertEquals(1, estados.last().pesajesPendientes)
        job.cancel()
    }
}
