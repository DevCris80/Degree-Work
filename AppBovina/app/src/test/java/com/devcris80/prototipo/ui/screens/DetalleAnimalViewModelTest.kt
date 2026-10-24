package com.devcris80.prototipo.ui.screens

import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Chapeta
import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.Evento
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.model.Registro
import com.devcris80.prototipo.domain.model.Sexo
import com.devcris80.prototipo.domain.usecase.DarDeBajaAnimal
import com.devcris80.prototipo.domain.usecase.LiberarChapeta
import com.devcris80.prototipo.testing.FakeAnimalRepository
import com.devcris80.prototipo.testing.FakeChapetaRepository
import com.devcris80.prototipo.testing.FakeConflictoRepository
import com.devcris80.prototipo.testing.conflictoDeAnimal
import com.devcris80.prototipo.testing.conflictoDeEvento
import com.devcris80.prototipo.testing.FakeEventoRepository
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

class DetalleAnimalViewModelTest {
    private val animalRepository = FakeAnimalRepository()
    private val eventoRepository = FakeEventoRepository()
    private val registroRepository = FakeRegistroRepository()
    private val chapetaRepository = FakeChapetaRepository()
    private val conflictoRepository = FakeConflictoRepository()
    private val idAnimal = UUID.randomUUID().toString()

    private fun crearViewModel() = DetalleAnimalViewModel(
        idAnimal,
        animalRepository,
        eventoRepository,
        registroRepository,
        chapetaRepository,
        DarDeBajaAnimal(animalRepository, clock = { 5L }),
        LiberarChapeta(chapetaRepository, clock = { 9L }),
        conflictoRepository,
    )

    private fun animal() = Animal(
        idAnimal = idAnimal,
        idPerfilFinca = "perfil_local",
        nombre = "Manchas",
        raza = "Holstein",
        sexo = Sexo.HEMBRA,
        etapa = Etapa.VACA,
        fechaNacimiento = 1L,
        proposito = Proposito.LECHE,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun combinaAnimalEventosRegistrosYChapetaActiva() = runTest {
        animalRepository.set(animal())
        val evento = Evento(UUID.randomUUID().toString(), idAnimal, idUsuario = UUID.randomUUID().toString(), tipoEvento = "vacuna", fecha = 1L, detalle = "aftosa")
        eventoRepository.set(idAnimal, listOf(evento))
        val registro = Registro(UUID.randomUUID().toString(), idAnimal, "usuario", peso = 300f, timestamp = 1L)
        registroRepository.set(idAnimal, listOf(registro))
        val chapeta = Chapeta(UUID.randomUUID().toString(), "04A1B2C3", idAnimal, fechaAsociacion = 1L)
        chapetaRepository.set(chapeta)

        val viewModel = crearViewModel()
        val estados = mutableListOf<DetalleAnimalUiState>()
        val job = launch { viewModel.estado.collect { estados.add(it) } }
        advanceUntilIdle()

        val ultimo = estados.last()
        assertEquals("Manchas", ultimo.animal?.nombre)
        assertEquals(listOf(evento), ultimo.eventos)
        assertEquals(listOf(registro), ultimo.registros)
        assertEquals(chapeta, ultimo.chapetaActiva)
        job.cancel()
    }

    @Test
    fun marcaElAnimalYLosEventosEnConflictoSinEsconderlos() = runTest {
        animalRepository.set(animal())
        val evento = Evento("evento-1", idAnimal, idUsuario = "usuario", tipoEvento = "vacuna", fecha = 1L, detalle = "aftosa")
        eventoRepository.set(idAnimal, listOf(evento))
        conflictoRepository.conflictos.value = listOf(
            conflictoDeAnimal(idAnimal, "v1" to "Jersey"),
            conflictoDeEvento("evento-1", "v2"),
        )

        val viewModel = crearViewModel()
        val estados = mutableListOf<DetalleAnimalUiState>()
        val job = launch { viewModel.estado.collect { estados.add(it) } }
        advanceUntilIdle()

        val ultimo = estados.last()
        assertEquals(true, ultimo.animalEnConflicto)
        assertEquals(setOf("evento-1"), ultimo.eventosEnConflicto)
        assertEquals(emptySet<String>(), ultimo.registrosEnConflicto)
        assertEquals(listOf(evento), ultimo.eventos)
        job.cancel()
    }

    @Test
    fun muestraElPadreYLaMadre_aunqueElPadreEsteDadoDeBaja() = runTest {
        val padre = animal().copy(idAnimal = "padre", nombre = "Zeus", sexo = Sexo.MACHO, etapa = Etapa.TORO, fechaBaja = 3L)
        val madre = animal().copy(idAnimal = "madre", nombre = "Lola")
        animalRepository.set(padre)
        animalRepository.set(madre)
        animalRepository.set(animal().copy(idPadre = "padre", idMadre = "madre"))

        val viewModel = crearViewModel()
        val estados = mutableListOf<DetalleAnimalUiState>()
        val job = launch { viewModel.estado.collect { estados.add(it) } }
        advanceUntilIdle()

        assertEquals(padre, estados.last().padre)
        assertEquals(madre, estados.last().madre)
        job.cancel()
    }

    @Test
    fun sinPadreNiMadre_ambosQuedanNulos() = runTest {
        animalRepository.set(animal())

        val viewModel = crearViewModel()
        val estados = mutableListOf<DetalleAnimalUiState>()
        val job = launch { viewModel.estado.collect { estados.add(it) } }
        advanceUntilIdle()

        assertEquals("Manchas", estados.last().animal?.nombre)
        assertEquals(null, estados.last().padre)
        assertEquals(null, estados.last().madre)
        job.cancel()
    }

    @Test
    fun onDarDeBaja_llamaAlCasoDeUsoConElAnimalCorrecto() = runTest {
        animalRepository.set(animal())
        val viewModel = crearViewModel()

        viewModel.onDarDeBaja()

        assertEquals(listOf(idAnimal to 5L), animalRepository.bajas)
    }

    @Test
    fun onLiberarChapeta_liberaLaChapetaIndicada() = runTest {
        animalRepository.set(animal())
        val chapeta = Chapeta(UUID.randomUUID().toString(), "04A1B2C3", idAnimal, fechaAsociacion = 1L)
        chapetaRepository.set(chapeta)
        val viewModel = crearViewModel()

        viewModel.onLiberarChapeta(chapeta.idChapeta)

        assertEquals(listOf(chapeta.idChapeta), chapetaRepository.liberadas)
        assertEquals(9L, chapetaRepository.chapeta(chapeta.idChapeta)?.fechaDesasociacion)
    }
}
