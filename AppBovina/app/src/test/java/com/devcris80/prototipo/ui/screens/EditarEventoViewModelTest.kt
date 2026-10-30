package com.devcris80.prototipo.ui.screens

import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.Evento
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.model.Rol
import com.devcris80.prototipo.domain.model.Sexo
import com.devcris80.prototipo.domain.model.TipoEvento
import com.devcris80.prototipo.domain.model.Usuario
import com.devcris80.prototipo.domain.usecase.ActualizarEvento
import com.devcris80.prototipo.domain.usecase.DarDeBajaEvento
import com.devcris80.prototipo.testing.FakeAnimalRepository
import com.devcris80.prototipo.testing.FakeCuentaRepository
import com.devcris80.prototipo.testing.FakeEventoRepository
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

class EditarEventoViewModelTest {
    private val animalRepository = FakeAnimalRepository()
    private val eventoRepository = FakeEventoRepository()
    private val cuentaRepository = FakeCuentaRepository()

    private fun crearViewModel(idEvento: String = "evento-1") = EditarEventoViewModel(
        idEvento,
        eventoRepository,
        animalRepository,
        cuentaRepository,
        ActualizarEvento(animalRepository, eventoRepository, FakeTransaccion()),
        DarDeBajaEvento(eventoRepository, cuentaRepository, clock = { AHORA }),
        clock = { AHORA },
    )

    private fun animal(idAnimal: String, nombre: String, etapa: Etapa) =
        Animal(idAnimal, "perfil_local", nombre, "Holstein", Sexo.HEMBRA, etapa, 1L, proposito = Proposito.LECHE)

    private fun conUsuario(rol: Rol) {
        cuentaRepository.usuario.value = Usuario("usuario-1", "perfil_local", "Ana", rol)
    }

    private fun conEvento(evento: Evento) = eventoRepository.set("vaca-1", listOf(evento))

    private fun vacunacion() = Evento(
        "evento-1", "vaca-1", "usuario-1", TipoEvento.VACUNACION.nombre, 1_000L, "Vacunación del ciclo I",
        biologico = "Aftosa", dosis = "2 ml", via = "Subcutánea",
    )

    private fun parto() = Evento(
        "evento-1", "vaca-1", "usuario-1", TipoEvento.PARTO.nombre, 1_000L, "Parto de Lola", idCriaResultante = "cria-1",
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        conUsuario(Rol.OPERARIO)
        animalRepository.set(animal("vaca-1", "Lola", Etapa.VACA))
        animalRepository.set(animal("cria-1", "Lucero", Etapa.TERNERA))
        conEvento(vacunacion())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun parteDeLosValoresGuardadosDelEventoYMuestraSuAnimal() = runTest {
        val estado = crearViewModel().estado.value

        assertTrue(estado.cargado)
        assertEquals("Vacunación del ciclo I", estado.titulo)
        assertEquals(1_000L, estado.fecha)
        assertEquals("Aftosa", estado.biologico)
        assertEquals("2 ml", estado.dosis)
        assertEquals("Subcutánea", estado.via)
        assertEquals("Lola", estado.nombreAnimal)
        assertTrue(estado.esVacunacion)
    }

    @Test
    fun guardarMuestraLosValoresNuevosEnElHistorialYEmiteGuardado() = runTest {
        val viewModel = crearViewModel()
        viewModel.onTituloCambiado("Vacunación del ciclo II")
        viewModel.onFechaCambiada(2_000L)
        viewModel.onBiologicoCambiado("Brucelosis")
        viewModel.onDosisCambiada("3 ml")
        viewModel.onViaSeleccionada("Oral")

        viewModel.onGuardar()

        assertEquals(EditarEventoEvento.Guardado, viewModel.eventos.first())
        val evento = eventoRepository.observeByAnimal("vaca-1").first().single()
        assertEquals("Vacunación del ciclo II", evento.detalle)
        assertEquals(2_000L, evento.fecha)
        assertEquals("Brucelosis", evento.biologico)
        assertEquals("3 ml", evento.dosis)
        assertEquals("Oral", evento.via)
    }

    @Test
    fun noSeGuardaSinTituloNiUnaVacunacionSinBiologico() = runTest {
        val viewModel = crearViewModel()

        viewModel.onTituloCambiado("   ")
        assertFalse(viewModel.estado.value.puedeGuardar)
        viewModel.onGuardar()
        assertEquals("Vacunación del ciclo I", eventoRepository.findById("evento-1")!!.detalle)

        viewModel.onTituloCambiado("Ciclo II")
        assertTrue(viewModel.estado.value.puedeGuardar)
        viewModel.onBiologicoCambiado("")
        assertFalse(viewModel.estado.value.puedeGuardar)
    }

    @Test
    fun unPartoMuestraSuCriaSinOfrecerCambiarla() = runTest {
        conEvento(parto())

        val estado = crearViewModel().estado.value

        assertFalse(estado.esVacunacion)
        assertEquals("Lucero", estado.nombreCria)
        assertEquals("Parto", estado.tipo)
    }

    @Test
    fun elGanaderoDaDeBajaElEventoYElOperarioNoPuede() = runTest {
        conUsuario(Rol.OPERARIO)
        val operario = crearViewModel()
        assertFalse(operario.estado.value.puedeDarDeBaja)
        operario.onDarDeBaja()
        assertEquals(1, eventoRepository.observeByAnimal("vaca-1").first().size)

        conUsuario(Rol.GANADERO)
        val ganadero = crearViewModel()
        assertTrue(ganadero.estado.value.puedeDarDeBaja)
        ganadero.onDarDeBaja()

        assertEquals(EditarEventoEvento.DadoDeBaja, ganadero.eventos.first())
        assertTrue(eventoRepository.observeByAnimal("vaca-1").first().isEmpty())
        assertEquals(AHORA, eventoRepository.findById("evento-1")!!.fechaBaja)
    }

    @Test
    fun unEventoQueYaNoExisteODadoDeBajaNoSeCargaNiSeGuarda() = runTest {
        val noExiste = crearViewModel("no-existe")
        assertFalse(noExiste.estado.value.cargado)
        assertEquals(EditarEventoError.EventoNoDisponible, noExiste.estado.value.error)
        assertFalse(noExiste.estado.value.puedeGuardar)

        conEvento(vacunacion().copy(fechaBaja = 5L))
        val deBaja = crearViewModel()
        assertFalse(deBaja.estado.value.cargado)
        assertEquals(EditarEventoError.EventoNoDisponible, deBaja.estado.value.error)
    }

    private companion object {
        const val AHORA = 10_000L
    }
}
