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
import com.devcris80.prototipo.domain.repository.ResultadoSincronizacion
import com.devcris80.prototipo.domain.usecase.ObservarPesajesPendientes
import com.devcris80.prototipo.testing.FakeAnimalRepository
import com.devcris80.prototipo.testing.FakeChapetaRepository
import com.devcris80.prototipo.testing.FakeConflictoRepository
import com.devcris80.prototipo.testing.conflictoDeAnimal
import com.devcris80.prototipo.testing.FakeCuentaRepository
import com.devcris80.prototipo.testing.FakePesajePendienteRepository
import com.devcris80.prototipo.testing.FakeRegistroRepository
import com.devcris80.prototipo.testing.FakeSesionRepository
import com.devcris80.prototipo.testing.FakeSincronizacionRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.flow.first
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
    private val sesionRepository = FakeSesionRepository()
    private val sincronizacionRepository = FakeSincronizacionRepository()
    private val conflictoRepository = FakeConflictoRepository()

    private fun crearViewModel() = ListaAnimalesViewModel(
        cuentaRepository,
        animalRepository,
        chapetaRepository,
        registroRepository,
        ObservarPesajesPendientes(pesajePendienteRepository, chapetaRepository, animalRepository),
        sesionRepository,
        sincronizacionRepository,
        conflictoRepository,
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
    fun marcaLosAnimalesQueTienenUnSiblingSinResolver() = runTest {
        val manchas = animal("Manchas")
        animalRepository.activos.value = listOf(manchas, animal("Lola"))
        conflictoRepository.conflictos.value = listOf(conflictoDeAnimal(manchas.idAnimal, "v1" to "Jersey"))

        val viewModel = crearViewModel()
        val estados = mutableListOf<ListaAnimalesUiState>()
        val job = launch { viewModel.estado.collect { estados.add(it) } }
        advanceUntilIdle()

        assertEquals(setOf(manchas.idAnimal), estados.last().animalesEnConflicto)
        // Una fila en Conflicto se consulta normalmente: sigue en la lista.
        assertEquals(2, estados.last().animales.size)
        job.cancel()
    }

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
    fun soloElGanaderoPuedeRegistrarLaUbicacionDeLaFinca() = runTest {
        suspend fun puede(usuario: Usuario?): Boolean {
            cuentaRepository.usuario.value = usuario
            val viewModel = crearViewModel()
            val estados = mutableListOf<ListaAnimalesUiState>()
            val job = launch { viewModel.estado.collect { estados.add(it) } }
            advanceUntilIdle()
            job.cancel()
            return estados.last().puedeRegistrarUbicacion
        }

        assertEquals(true, puede(Usuario("u1", "perfil_local", "Ana", Rol.GANADERO)))
        assertEquals(false, puede(Usuario("u2", "perfil_local", "Beto", Rol.OPERARIO)))
        assertEquals("sin Usuario no hay permiso", false, puede(null))
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

    @Test
    fun cerrarSesion_olvidaLaSesionGuardada() = runTest {
        sesionRepository.guardada.value = true
        val viewModel = crearViewModel()

        viewModel.onCerrarSesion()

        assertEquals(false, sesionRepository.guardada.value)
    }

    @Test
    fun sincronizar_subeYDescargaYAvisaLosConflictosYRechazos() = runTest {
        sincronizacionRepository.resultado = ResultadoSincronizacion.Completa(aceptados = 3, enConflicto = 1, rechazados = 2)
        val viewModel = crearViewModel()
        val estados = mutableListOf<ListaAnimalesUiState>()
        val job = launch { viewModel.estado.collect { estados.add(it) } }

        viewModel.onSincronizar()
        advanceUntilIdle()

        assertEquals(1, sincronizacionRepository.sincronizaciones)
        assertEquals(0, sincronizacionRepository.programaciones)
        assertEquals(false, estados.last().sincronizando)
        assertEquals("Sincronizado. 1 en conflicto. 2 rechazados.", estados.last().avisoSincronizacion)
        viewModel.onAvisoMostrado()
        advanceUntilIdle()
        assertEquals(null, estados.last().avisoSincronizacion)
        job.cancel()
    }

    @Test
    fun sincronizarSinSenal_dejaProgramadoElTrabajoYAvisaQueSeSincronizaDespues() = runTest {
        sincronizacionRepository.resultado = ResultadoSincronizacion.SinConexion
        val viewModel = crearViewModel()
        val estados = mutableListOf<ListaAnimalesUiState>()
        val job = launch { viewModel.estado.collect { estados.add(it) } }

        viewModel.onSincronizar()
        advanceUntilIdle()

        assertEquals(1, sincronizacionRepository.programaciones)
        assertEquals("Sin señal. Lo que falta se sincroniza después.", estados.last().avisoSincronizacion)
        job.cancel()
    }

    @Test
    fun mientrasSincroniza_otroToqueNoVuelveASubir() = runTest {
        sincronizacionRepository.espera = CompletableDeferred()
        val viewModel = crearViewModel()
        val estados = mutableListOf<ListaAnimalesUiState>()
        val job = launch { viewModel.estado.collect { estados.add(it) } }

        viewModel.onSincronizar()
        advanceUntilIdle()
        assertEquals(true, estados.last().sincronizando)
        viewModel.onSincronizar()
        sincronizacionRepository.espera!!.complete(Unit)
        advanceUntilIdle()

        assertEquals(1, sincronizacionRepository.sincronizaciones)
        assertEquals(false, estados.last().sincronizando)
        job.cancel()
    }
}
