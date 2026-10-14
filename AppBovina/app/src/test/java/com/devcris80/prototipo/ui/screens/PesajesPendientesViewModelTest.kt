package com.devcris80.prototipo.ui.screens

import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Chapeta
import com.devcris80.prototipo.domain.model.Conciliacion
import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.MotivoPesajePendiente
import com.devcris80.prototipo.domain.model.PesajePendiente
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.model.ResolucionPesajePendiente
import com.devcris80.prototipo.domain.model.Rol
import com.devcris80.prototipo.domain.model.Sexo
import com.devcris80.prototipo.domain.model.Usuario
import com.devcris80.prototipo.domain.usecase.AsignarPesajesPendientes
import com.devcris80.prototipo.domain.usecase.DescartarPesajePendiente
import com.devcris80.prototipo.domain.usecase.EstadoCodigo
import com.devcris80.prototipo.domain.usecase.ObservarPesajesPendientes
import com.devcris80.prototipo.testing.FakeAnimalRepository
import com.devcris80.prototipo.testing.FakeChapetaRepository
import com.devcris80.prototipo.testing.FakeCuentaRepository
import com.devcris80.prototipo.testing.FakePesajePendienteRepository
import com.devcris80.prototipo.testing.FakeRegistroRepository
import com.devcris80.prototipo.testing.FakeTransaccion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.UUID

class PesajesPendientesViewModelTest {
    private val animalRepository = FakeAnimalRepository()
    private val chapetaRepository = FakeChapetaRepository()
    private val registroRepository = FakeRegistroRepository()
    private val cuentaRepository = FakeCuentaRepository()
    private val pesajePendienteRepository = FakePesajePendienteRepository()

    private fun crearViewModel() = PesajesPendientesViewModel(
        ObservarPesajesPendientes(pesajePendienteRepository, chapetaRepository, animalRepository),
        animalRepository,
        chapetaRepository,
        pesajePendienteRepository,
        AsignarPesajesPendientes(pesajePendienteRepository, animalRepository, registroRepository, cuentaRepository, FakeTransaccion()),
        DescartarPesajePendiente(pesajePendienteRepository, cuentaRepository, FakeTransaccion()),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        cuentaRepository.usuario.value = Usuario(UUID.randomUUID().toString(), "perfil_local", "Ana", Rol.GANADERO)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun animal(nombre: String, fechaBaja: Long? = null) = Animal(
        idAnimal = UUID.randomUUID().toString(),
        idPerfilFinca = "perfil_local",
        nombre = nombre,
        raza = "Holstein",
        sexo = Sexo.HEMBRA,
        etapa = Etapa.VACA,
        fechaNacimiento = 1L,
        proposito = Proposito.LECHE,
        fechaBaja = fechaBaja,
    )

    private fun pendiente(codigo: String, peso: Float = 100f, timestamp: Long = 1L) = PesajePendiente(
        idPesajePendiente = UUID.randomUUID().toString(),
        idLectura = UUID.randomUUID().toString(),
        codigo = codigo,
        peso = peso,
        timestamp = timestamp,
        idUsuario = UUID.randomUUID().toString(),
        motivo = MotivoPesajePendiente.CODIGO_SIN_CHAPETA_ACTIVA,
    )

    @Test
    fun codigoSinChapeta_quedaComoSinChapetaActiva() = runTest {
        pesajePendienteRepository.set(pendiente("04A1B2C3"))
        val viewModel = crearViewModel()
        val estados = mutableListOf<PesajesPendientesUiState>()
        val job = launch { viewModel.estado.collect { estados.add(it) } }
        advanceUntilIdle()

        assertEquals(EstadoCodigo.SinChapetaActiva, estados.last().pendientes.single().estadoCodigo)
        job.cancel()
    }

    @Test
    fun codigoDeAnimalActivo_sugiereEseAnimal() = runTest {
        val manchas = animal("Manchas")
        animalRepository.activos.value = listOf(manchas)
        chapetaRepository.set(Chapeta(UUID.randomUUID().toString(), "04A1B2C3", manchas.idAnimal, fechaAsociacion = 1L))
        pesajePendienteRepository.set(pendiente("04A1B2C3"))

        val viewModel = crearViewModel()
        val estados = mutableListOf<PesajesPendientesUiState>()
        val job = launch { viewModel.estado.collect { estados.add(it) } }
        advanceUntilIdle()

        val estadoCodigo = estados.last().pendientes.single().estadoCodigo as EstadoCodigo.ChapetaDeAnimalActivo
        assertEquals(manchas.idAnimal, estadoCodigo.animal.idAnimal)
        job.cancel()
    }

    @Test
    fun codigoDeAnimalDadoDeBaja_quedaComoChapetaDeAnimalDadoDeBaja() = runTest {
        val canela = animal("Canela", fechaBaja = 5L)
        chapetaRepository.set(Chapeta(UUID.randomUUID().toString(), "04A1B2C3", canela.idAnimal, fechaAsociacion = 1L))
        pesajePendienteRepository.set(pendiente("04A1B2C3"))

        val viewModel = crearViewModel()
        val estados = mutableListOf<PesajesPendientesUiState>()
        val job = launch { viewModel.estado.collect { estados.add(it) } }
        advanceUntilIdle()

        assertEquals(EstadoCodigo.ChapetaDeAnimalDadoDeBaja, estados.last().pendientes.single().estadoCodigo)
        job.cancel()
    }

    @Test
    fun confirmarAsignar_resuelveElPendienteYSinOtrosNoOfreceRestantes() = runTest {
        val manchas = animal("Manchas")
        animalRepository.activos.value = listOf(manchas)
        val unico = pendiente("04A1B2C3")
        pesajePendienteRepository.set(unico)
        val viewModel = crearViewModel()

        viewModel.onConfirmarAsignar(unico, manchas.idAnimal)

        assertEquals(
            ResolucionPesajePendiente.ASIGNADO,
            pesajePendienteRepository.todos.value.first().conciliacion?.resolucion,
        )
        assertNull(viewModel.estado.value.ofertaRestantes)
    }

    @Test
    fun confirmarAsignar_conOtrosDelMismoCodigo_ofreceAsignarlosTambien() = runTest {
        val manchas = animal("Manchas")
        animalRepository.activos.value = listOf(manchas)
        val primero = pendiente("04A1B2C3", timestamp = 2L)
        val segundo = pendiente("04A1B2C3", timestamp = 1L)
        pesajePendienteRepository.set(primero)
        pesajePendienteRepository.set(segundo)
        val viewModel = crearViewModel()
        val estados = mutableListOf<PesajesPendientesUiState>()
        val job = launch { viewModel.estado.collect { estados.add(it) } }
        advanceUntilIdle()

        viewModel.onConfirmarAsignar(primero, manchas.idAnimal)
        advanceUntilIdle()

        val oferta = estados.last().ofertaRestantes
        assertEquals("04A1B2C3", oferta?.codigo)
        assertEquals(listOf(segundo.idPesajePendiente), oferta?.idsPesajePendiente)
        job.cancel()
    }

    @Test
    fun asignarRestantesConfirmado_asignaLosQueQuedaban() = runTest {
        val manchas = animal("Manchas")
        animalRepository.activos.value = listOf(manchas)
        val primero = pendiente("04A1B2C3", timestamp = 2L)
        val segundo = pendiente("04A1B2C3", timestamp = 1L)
        pesajePendienteRepository.set(primero)
        pesajePendienteRepository.set(segundo)
        val viewModel = crearViewModel()
        val job = launch { viewModel.estado.collect { } }
        advanceUntilIdle()
        viewModel.onConfirmarAsignar(primero, manchas.idAnimal)
        advanceUntilIdle()

        viewModel.onAsignarRestantesConfirmado()

        assertEquals(
            ResolucionPesajePendiente.ASIGNADO,
            pesajePendienteRepository.todos.value.first { it.idPesajePendiente == segundo.idPesajePendiente }.conciliacion?.resolucion,
        )
        assertNull(viewModel.estado.value.ofertaRestantes)
        job.cancel()
    }

    @Test
    fun asignarRestantesDescartado_noAsignaYLimpiaLaOferta() = runTest {
        val manchas = animal("Manchas")
        animalRepository.activos.value = listOf(manchas)
        val primero = pendiente("04A1B2C3", timestamp = 2L)
        val segundo = pendiente("04A1B2C3", timestamp = 1L)
        pesajePendienteRepository.set(primero)
        pesajePendienteRepository.set(segundo)
        val viewModel = crearViewModel()
        val job = launch { viewModel.estado.collect { } }
        advanceUntilIdle()
        viewModel.onConfirmarAsignar(primero, manchas.idAnimal)
        advanceUntilIdle()

        viewModel.onAsignarRestantesDescartado()

        assertNull(pesajePendienteRepository.todos.value.first { it.idPesajePendiente == segundo.idPesajePendiente }.conciliacion)
        assertNull(viewModel.estado.value.ofertaRestantes)
        job.cancel()
    }

    @Test
    fun asignarRestantesConfirmado_conUnoYaResuelto_marcaPendienteNoDisponible() = runTest {
        val manchas = animal("Manchas")
        animalRepository.activos.value = listOf(manchas)
        val primero = pendiente("04A1B2C3", timestamp = 2L)
        val segundo = pendiente("04A1B2C3", timestamp = 1L)
        pesajePendienteRepository.set(primero)
        pesajePendienteRepository.set(segundo)
        val viewModel = crearViewModel()
        val job = launch { viewModel.estado.collect { } }
        advanceUntilIdle()
        viewModel.onConfirmarAsignar(primero, manchas.idAnimal)
        advanceUntilIdle()
        pesajePendienteRepository.conciliar(
            segundo.idPesajePendiente,
            Conciliacion(ResolucionPesajePendiente.DESCARTADO, UUID.randomUUID().toString(), 3L),
        )

        viewModel.onAsignarRestantesConfirmado()
        advanceUntilIdle()

        assertEquals(PesajesPendientesError.PendienteNoDisponible, viewModel.estado.value.error)
        job.cancel()
    }

    @Test
    fun confirmarAsignar_conAnimalDadoDeBaja_marcaAnimalNoDisponible() = runTest {
        val canela = animal("Canela", fechaBaja = 5L)
        animalRepository.set(canela)
        val unico = pendiente("04A1B2C3")
        pesajePendienteRepository.set(unico)
        val viewModel = crearViewModel()
        val job = launch { viewModel.estado.collect { } }
        advanceUntilIdle()

        viewModel.onConfirmarAsignar(unico, canela.idAnimal)
        advanceUntilIdle()

        assertEquals(PesajesPendientesError.AnimalNoDisponible, viewModel.estado.value.error)
        job.cancel()
    }

    @Test
    fun confirmarDescartar_pendienteYaResuelto_marcaPendienteNoDisponible() = runTest {
        val unico = pendiente("04A1B2C3")
        pesajePendienteRepository.set(unico)
        val viewModel = crearViewModel()
        val job = launch { viewModel.estado.collect { } }
        advanceUntilIdle()
        viewModel.onConfirmarDescartar(unico.idPesajePendiente)
        advanceUntilIdle()

        viewModel.onConfirmarDescartar(unico.idPesajePendiente)
        advanceUntilIdle()

        assertEquals(PesajesPendientesError.PendienteNoDisponible, viewModel.estado.value.error)
        job.cancel()
    }

    @Test
    fun errorVisto_limpiaElError() = runTest {
        val unico = pendiente("04A1B2C3")
        pesajePendienteRepository.set(unico)
        val viewModel = crearViewModel()
        val job = launch { viewModel.estado.collect { } }
        advanceUntilIdle()
        viewModel.onConfirmarDescartar(unico.idPesajePendiente)
        advanceUntilIdle()
        viewModel.onConfirmarDescartar(unico.idPesajePendiente)
        advanceUntilIdle()
        assertEquals(PesajesPendientesError.PendienteNoDisponible, viewModel.estado.value.error)

        viewModel.onErrorVisto()
        advanceUntilIdle()

        assertNull(viewModel.estado.value.error)
        job.cancel()
    }

    @Test
    fun confirmarDescartar_resuelveComoDescartado() = runTest {
        val unico = pendiente("04A1B2C3")
        pesajePendienteRepository.set(unico)
        val viewModel = crearViewModel()

        viewModel.onConfirmarDescartar(unico.idPesajePendiente)

        assertEquals(
            ResolucionPesajePendiente.DESCARTADO,
            pesajePendienteRepository.todos.value.first().conciliacion?.resolucion,
        )
    }
}
