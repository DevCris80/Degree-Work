package com.devcris80.prototipo.ui.screens

import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.Evento
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.testing.FakeAnimalRepository
import com.devcris80.prototipo.testing.FakeEventoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class EventosViewModelTest {
    private val animalRepository = FakeAnimalRepository()
    private val eventoRepository = FakeEventoRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun animal(id: String, nombre: String, fechaBaja: Long? = null) = Animal(
        id, "perfil_local", nombre, "Criollo", Etapa.VACA.sexo, Etapa.VACA, 1L,
        proposito = Proposito.LECHE, fechaBaja = fechaBaja,
    )

    private fun evento(id: String, idAnimal: String, fecha: Long, tipo: String = "Vacunación", detalle: String = "Aftosa") =
        Evento(id, idAnimal, "usuario-1", tipo, fecha, detalle)

    private fun TestScope.ultimoEstado(): EventosUiState {
        val viewModel = EventosViewModel(eventoRepository, animalRepository, clock = { AHORA })
        val estados = mutableListOf<EventosUiState>()
        val job = launch { viewModel.estado.collect { estados.add(it) } }
        advanceUntilIdle()
        job.cancel()
        return estados.last()
    }

    @Test
    fun sinEventosLaListaEstaVacia() = runTest {
        assertTrue(ultimoEstado().eventos.isEmpty())
    }

    @Test
    fun cadaEventoMuestraElNombreDelAnimalYSuTipoDetalleYFecha() = runTest {
        animalRepository.activos.value = listOf(animal("a1", "Lola"))
        eventoRepository.set("a1", listOf(evento("e1", "a1", AHORA - DIA, tipo = "Parto", detalle = "Sin complicaciones")))

        val item = ultimoEstado().eventos.single()

        assertEquals("e1", item.idEvento)
        assertEquals("Lola", item.nombreAnimal)
        assertEquals("Parto", item.tipo)
        assertEquals("Sin complicaciones", item.detalle)
        assertEquals(AHORA - DIA, item.fecha)
    }

    @Test
    fun unEventoConFechaFuturaEstaProgramadoYUnoPasadoNo() = runTest {
        animalRepository.activos.value = listOf(animal("a1", "Lola"))
        eventoRepository.set("a1", listOf(evento("pasado", "a1", AHORA - DIA), evento("futuro", "a1", AHORA + DIA)))

        val porId = ultimoEstado().eventos.associateBy { it.idEvento }

        assertEquals(false, porId.getValue("pasado").programado)
        assertEquals(true, porId.getValue("futuro").programado)
    }

    @Test
    fun losProgramadosVanPrimeroElMasProximoPrimeroYDespuesLosPasadosDelMasRecienteAlMasAntiguo() = runTest {
        animalRepository.activos.value = listOf(animal("a1", "Lola"))
        eventoRepository.set(
            "a1",
            listOf(
                evento("pasado-viejo", "a1", AHORA - 5 * DIA),
                evento("futuro-lejano", "a1", AHORA + 9 * DIA),
                evento("pasado-reciente", "a1", AHORA - DIA),
                evento("futuro-cercano", "a1", AHORA + 2 * DIA),
            ),
        )

        assertEquals(
            listOf("futuro-cercano", "futuro-lejano", "pasado-reciente", "pasado-viejo"),
            ultimoEstado().eventos.map { it.idEvento },
        )
    }

    @Test
    fun losEventosDeUnAnimalDadoDeBajaNoAparecen() = runTest {
        animalRepository.activos.value = listOf(animal("a1", "Lola"))
        animalRepository.set(animal("baja", "Vieja", fechaBaja = 3L))
        eventoRepository.set("a1", listOf(evento("e1", "a1", AHORA)))
        eventoRepository.set("baja", listOf(evento("e2", "baja", AHORA)))

        assertEquals(listOf("e1"), ultimoEstado().eventos.map { it.idEvento })
    }

    private companion object {
        const val DIA = 24L * 60 * 60 * 1000
        const val AHORA = 100 * DIA
    }
}
