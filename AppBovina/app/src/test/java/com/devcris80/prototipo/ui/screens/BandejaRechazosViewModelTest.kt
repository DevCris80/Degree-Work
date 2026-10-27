package com.devcris80.prototipo.ui.screens

import com.devcris80.prototipo.domain.model.AccionSugerida
import com.devcris80.prototipo.domain.model.EntidadSincronizada
import com.devcris80.prototipo.domain.model.MotivoRechazo
import com.devcris80.prototipo.domain.model.Rechazo
import com.devcris80.prototipo.domain.usecase.ObservarBandejaRechazos
import com.devcris80.prototipo.testing.FakeAnimalRepository
import com.devcris80.prototipo.testing.FakeChapetaRepository
import com.devcris80.prototipo.testing.FakeRechazoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
class BandejaRechazosViewModelTest {
    private val rechazoRepository = FakeRechazoRepository()

    private fun crearViewModel() = BandejaRechazosViewModel(
        ObservarBandejaRechazos(rechazoRepository, FakeAnimalRepository(), FakeChapetaRepository()),
        rechazoRepository,
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
    fun laBandejaMuestraLosRechazosAbiertosConSuAccion() = runTest {
        rechazoRepository.set(rechazo("rechazo-1", EntidadSincronizada.PESAJE_PENDIENTE, "55000"))
        val viewModel = crearViewModel()
        val job = launch { viewModel.estado.collect {} }
        advanceUntilIdle()

        val enBandeja = viewModel.estado.value.rechazos.single()
        assertEquals(MotivoRechazo.YaConciliado, enBandeja.motivo)
        assertEquals(AccionSugerida.ACEPTAR_LA_DEL_SERVIDOR, enBandeja.accion)
        job.cancel()
    }

    @Test
    fun cerrarUnRechazoLoSacaDeLaBandeja() = runTest {
        rechazoRepository.set(rechazo("rechazo-1", EntidadSincronizada.EVENTO, "42501"))
        rechazoRepository.set(rechazo("rechazo-2", EntidadSincronizada.REGISTRO, "42501"))
        val viewModel = crearViewModel()
        val job = launch { viewModel.estado.collect {} }

        viewModel.onCerrar("rechazo-1")
        advanceUntilIdle()

        assertEquals(listOf("rechazo-1"), rechazoRepository.cerrados)
        assertEquals(listOf("rechazo-2"), viewModel.estado.value.rechazos.map { it.rechazo.idRechazo })
        job.cancel()
    }
}

internal fun rechazo(idRechazo: String, entidad: EntidadSincronizada, codigo: String) = Rechazo(
    idRechazo = idRechazo,
    entidad = entidad,
    idFila = "fila-$idRechazo",
    contenido = "{}",
    motivo = "$codigo: mensaje",
    fechaRechazo = 1L,
    codigo = codigo,
    idAnimal = null,
    codigoChapeta = null,
    porSuAnimal = false,
)
