package com.devcris80.prototipo.ui.screens

import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.model.Rol
import com.devcris80.prototipo.domain.model.TipoEvento
import com.devcris80.prototipo.domain.model.Usuario
import com.devcris80.prototipo.domain.usecase.RegistrarEvento
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

class NuevoEventoViewModelTest {
    private val animalRepository = FakeAnimalRepository()
    private val eventoRepository = FakeEventoRepository()
    private val cuentaRepository = FakeCuentaRepository()
    private val registrarEvento =
        RegistrarEvento(animalRepository, eventoRepository, cuentaRepository, FakeTransaccion())

    private fun vaca(id: String = "vaca-1", fechaBaja: Long? = null) = Animal(
        id, "perfil_local", "Lola", "Holstein", Etapa.VACA.sexo, Etapa.VACA, 1L,
        proposito = Proposito.LECHE, fechaBaja = fechaBaja,
    )

    private fun cria() = vaca("cria-1").copy(nombre = "Lucero", etapa = Etapa.TERNERA)

    private fun crearViewModel(idAnimal: String? = null) =
        NuevoEventoViewModel(idAnimal, animalRepository, registrarEvento, clock = { AHORA })

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        cuentaRepository.usuario.value = Usuario("usuario-1", "perfil_local", "Ana", Rol.OPERARIO)
        animalRepository.activos.value = listOf(vaca())
        animalRepository.set(vaca())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun desdeElDetalleDeUnAnimalElAnimalYaVieneElegidoYDesdeLaPestanaNo() {
        assertEquals("vaca-1", crearViewModel(idAnimal = "vaca-1").estado.value.idAnimal)
        assertNull(crearViewModel().estado.value.idAnimal)
    }

    @Test
    fun empiezaComoVacunacionConLaFechaDeAhoraYOfreceLosAnimalesActivos() {
        val estado = crearViewModel().estado.value

        assertEquals(TipoEvento.VACUNACION, estado.tipo)
        assertEquals(AHORA, estado.fecha)
        assertFalse(estado.programado)
        assertEquals(listOf("vaca-1"), estado.animales.map { it.idAnimal })
    }

    @Test
    fun unaVacunacionNecesitaAnimalTituloYBiologico() {
        val viewModel = crearViewModel()
        assertFalse(viewModel.estado.value.puedeGuardar)

        viewModel.onAnimalSeleccionado("vaca-1")
        viewModel.onTituloCambiado("Vacunación del ciclo I")
        assertFalse("falta el biológico", viewModel.estado.value.puedeGuardar)

        viewModel.onBiologicoCambiado("Aftosa")
        assertTrue(viewModel.estado.value.puedeGuardar)
    }

    @Test
    fun unPartoNoPideBiologicoYLaCriaEsOpcional() {
        val viewModel = crearViewModel("vaca-1")

        viewModel.onTipoSeleccionado(TipoEvento.PARTO)
        viewModel.onTituloCambiado("Parto sin complicaciones")

        assertTrue(viewModel.estado.value.puedeGuardar)
    }

    @Test
    fun unaFechaPosteriorAAhoraEsUnEventoProgramado() {
        val viewModel = crearViewModel()

        viewModel.onFechaCambiada(AHORA + DIA)
        assertTrue(viewModel.estado.value.programado)

        viewModel.onFechaCambiada(AHORA - DIA)
        assertFalse(viewModel.estado.value.programado)
    }

    @Test
    fun laCriaNoPuedeSerElMismoAnimalDelEvento() {
        animalRepository.activos.value = listOf(vaca(), cria())
        val viewModel = crearViewModel("vaca-1")

        assertEquals(listOf("cria-1"), viewModel.estado.value.candidatasCria.map { it.idAnimal })
    }

    @Test
    fun guardarUnaVacunacionFuturaRegistraElEventoConSusCamposYEmiteGuardado() = runTest {
        val viewModel = crearViewModel("vaca-1")
        viewModel.onTituloCambiado("Vacunación del ciclo I")
        viewModel.onBiologicoCambiado("Aftosa")
        viewModel.onDosisCambiada("2 ml")
        viewModel.onViaSeleccionada("Subcutánea")
        viewModel.onFechaCambiada(AHORA + 3 * DIA)

        viewModel.onGuardar()

        assertEquals(NuevoEventoEvento.Guardado, viewModel.eventos.first())
        val evento = eventoRepository.observeByAnimal("vaca-1").first().single()
        assertEquals("Vacunación", evento.tipoEvento)
        assertEquals("Vacunación del ciclo I", evento.detalle)
        assertEquals(AHORA + 3 * DIA, evento.fecha)
        assertEquals("Aftosa", evento.biologico)
        assertEquals("2 ml", evento.dosis)
        assertEquals("Subcutánea", evento.via)
        assertEquals("usuario-1", evento.idUsuario)
    }

    @Test
    fun guardarUnPartoConCriaDejaALaCriaConSuMadre() = runTest {
        animalRepository.activos.value = listOf(vaca(), cria())
        animalRepository.set(cria())
        val viewModel = crearViewModel("vaca-1")
        viewModel.onTipoSeleccionado(TipoEvento.PARTO)
        viewModel.onTituloCambiado("Parto")
        viewModel.onCriaSeleccionada("cria-1")

        viewModel.onGuardar()

        assertEquals(NuevoEventoEvento.Guardado, viewModel.eventos.first())
        assertEquals("cria-1", eventoRepository.observeByAnimal("vaca-1").first().single().idCriaResultante)
        assertEquals("vaca-1", animalRepository.findById("cria-1")!!.idMadre)
    }

    @Test
    fun unAnimalQueYaNoEstaActivoMuestraElErrorYPermiteReintentar() = runTest {
        animalRepository.set(vaca(fechaBaja = 9L))
        val viewModel = crearViewModel("vaca-1")
        viewModel.onTituloCambiado("Vacuna")
        viewModel.onBiologicoCambiado("Aftosa")

        viewModel.onGuardar()

        val estado = viewModel.estado.value
        assertEquals(NuevoEventoError.AnimalNoDisponible, estado.error)
        assertFalse(estado.guardando)
        assertTrue(eventoRepository.observeByAnimal("vaca-1").first().isEmpty())
    }

    private companion object {
        const val DIA = 24L * 60 * 60 * 1000
        const val AHORA = 100 * DIA
    }
}
