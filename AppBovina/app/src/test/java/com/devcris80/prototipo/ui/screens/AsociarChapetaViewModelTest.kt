package com.devcris80.prototipo.ui.screens

import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Chapeta
import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.MotivoPesajePendiente
import com.devcris80.prototipo.domain.model.PesajePendiente
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.model.Rol
import com.devcris80.prototipo.domain.model.Sexo
import com.devcris80.prototipo.domain.model.Usuario
import com.devcris80.prototipo.domain.usecase.AsignarPesajesPendientes
import com.devcris80.prototipo.domain.usecase.AsociarChapeta
import com.devcris80.prototipo.testing.FakeAnimalRepository
import com.devcris80.prototipo.testing.FakeChapetaRepository
import com.devcris80.prototipo.testing.FakeCuentaRepository
import com.devcris80.prototipo.testing.FakePesajePendienteRepository
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AsociarChapetaViewModelTest {
    private val animalRepository = FakeAnimalRepository()
    private val chapetaRepository = FakeChapetaRepository()
    private val pesajePendienteRepository = FakePesajePendienteRepository()
    private val registroRepository = FakeRegistroRepository()
    private val cuentaRepository = FakeCuentaRepository()

    private fun crearViewModel(idAnimal: String? = null, codigo: String? = null) = AsociarChapetaViewModel(
        idAnimal,
        codigo,
        animalRepository,
        chapetaRepository,
        AsociarChapeta(animalRepository, chapetaRepository, pesajePendienteRepository, FakeTransaccion(), clock = { 9L }),
        AsignarPesajesPendientes(
            pesajePendienteRepository,
            animalRepository,
            registroRepository,
            cuentaRepository,
            FakeTransaccion(),
        ),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        cuentaRepository.usuario.value = Usuario("usuario-1", "perfil_local", "Ana", Rol.OPERARIO)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun animal(id: String, nombre: String = "Animal $id", fechaBaja: Long? = null) = Animal(
        idAnimal = id,
        idPerfilFinca = "perfil_local",
        nombre = nombre,
        raza = "Holstein",
        sexo = Sexo.HEMBRA,
        etapa = Etapa.VACA,
        fechaNacimiento = 1L,
        proposito = Proposito.LECHE,
        fechaBaja = fechaBaja,
    ).also {
        animalRepository.set(it)
        if (fechaBaja == null) animalRepository.activos.value = animalRepository.activos.value + it
    }

    private fun chapeta(id: String, codigo: String, idAnimal: String) =
        Chapeta(id, codigo, idAnimal, fechaAsociacion = 1L).also(chapetaRepository::set)

    private fun pendiente(id: String, codigo: String, timestamp: Long) = PesajePendiente(
        id, "lectura-$id", codigo, 100f, timestamp, "usuario-1", MotivoPesajePendiente.CODIGO_SIN_CHAPETA_ACTIVA,
    ).also(pesajePendienteRepository::set)

    @Test
    fun alAsociarDesdeElDetalleElAnimalYaVieneElegido() = runTest {
        animal("a-1", "Manchas")

        val viewModel = crearViewModel(idAnimal = "a-1")

        assertEquals("a-1", viewModel.estado.value.idAnimal)
        assertEquals("Manchas", viewModel.estado.value.nombreAnimal)
        assertFalse(viewModel.estado.value.codigoSoloLectura)
        assertFalse(viewModel.estado.value.puedeAsociar)
    }

    @Test
    fun alAsociarUnCodigoLeidoSoloOfreceLosAnimalesActivosSinChapetaActiva() = runTest {
        animal("sin-chapeta", "Lucero")
        animal("con-chapeta", "Manchas").also { chapeta("c-1", "AAAA", it.idAnimal) }
        animal("liberada", "Canela").also { chapeta("c-2", "BBBB", it.idAnimal) }
        chapetaRepository.liberar("c-2", 5L)
        animal("baja", "Nube", fechaBaja = 4L)

        val viewModel = crearViewModel(codigo = "CCCC")

        assertEquals(listOf("Canela", "Lucero"), viewModel.estado.value.candidatos.map { it.nombre })
        assertEquals("CCCC", viewModel.estado.value.codigo)
        assertTrue(viewModel.estado.value.codigoSoloLectura)
        assertNull(viewModel.estado.value.idAnimal)
    }

    @Test
    fun noSePuedeAsociarSinAnimalNiCodigo() = runTest {
        animal("a-1")
        val viewModel = crearViewModel(codigo = "AAAA")
        assertFalse(viewModel.estado.value.puedeAsociar)

        viewModel.onAnimalSeleccionado("a-1")

        assertTrue(viewModel.estado.value.puedeAsociar)
    }

    @Test
    fun asociaLaChapetaYEmiteAsociadaSiElCodigoNoTienePendientes() = runTest {
        animal("a-1")
        val viewModel = crearViewModel(idAnimal = "a-1")
        viewModel.onCodigoCambiado("04:a1")

        viewModel.onAsociar()

        assertEquals(AsociarChapetaEvento.Asociada, viewModel.eventos.first())
        assertEquals("04A1", chapetaRepository.findActivaByAnimal("a-1")!!.codigo)
    }

    @Test
    fun unCodigoConChapetaActivaEnOtroAnimalNoSeAsociaYLoIndica() = runTest {
        animal("a-1")
        animal("a-2").also { chapeta("c-2", "AAAA", it.idAnimal) }
        val viewModel = crearViewModel(idAnimal = "a-1", codigo = "AAAA")

        viewModel.onAsociar()

        assertNotNull(viewModel.estado.value.error)
        assertTrue(viewModel.estado.value.error!!.contains("AAAA"))
        assertNull(chapetaRepository.findActivaByAnimal("a-1"))
        assertFalse(viewModel.estado.value.guardando)
    }

    @Test
    fun cambiarElCodigoLimpiaElError() = runTest {
        animal("a-1")
        animal("a-2").also { chapeta("c-2", "AAAA", it.idAnimal) }
        val viewModel = crearViewModel(idAnimal = "a-1", codigo = "AAAA")
        viewModel.onAsociar()

        viewModel.onCodigoCambiado("BBBB")

        assertNull(viewModel.estado.value.error)
    }

    @Test
    fun conPesajesPendientesOfreceAsignarlosSinEmitirTodavia() = runTest {
        animal("a-1", "Manchas")
        pendiente("p-1", "AAAA", timestamp = 1_000L)
        pendiente("p-2", "AAAA", timestamp = 2_000L)
        val viewModel = crearViewModel(idAnimal = "a-1", codigo = "AAAA")

        viewModel.onAsociar()

        val oferta = viewModel.estado.value.ofertaPesajesPendientes!!
        assertEquals("Manchas", oferta.nombreAnimal)
        assertEquals("AAAA", oferta.codigo)
        assertEquals(listOf("p-2", "p-1"), oferta.pesajes.map { it.idPesajePendiente })
        assertTrue(registroRepository.observeByAnimal("a-1").first().isEmpty())
    }

    @Test
    fun aceptarLaOfertaAsignaLosPesajesConSuHoraOriginalYEmiteAsociada() = runTest {
        animal("a-1")
        pendiente("p-1", "AAAA", timestamp = 1_000L)
        pendiente("p-2", "AAAA", timestamp = 2_000L)
        val viewModel = crearViewModel(idAnimal = "a-1", codigo = "AAAA").apply { onAsociar() }

        viewModel.onOfertaPesajesPendientesRespondida(true)

        val registros = registroRepository.observeByAnimal("a-1").first()
        assertEquals(setOf(1_000L, 2_000L), registros.map { it.timestamp }.toSet())
        assertEquals(2, registros.size)
        assertNull(viewModel.estado.value.ofertaPesajesPendientes)
        assertEquals(AsociarChapetaEvento.Asociada, viewModel.eventos.first())
    }

    @Test
    fun rechazarLaOfertaNoAsignaPeroLaChapetaYaQuedoAsociada() = runTest {
        animal("a-1")
        pendiente("p-1", "AAAA", timestamp = 1_000L)
        val viewModel = crearViewModel(idAnimal = "a-1", codigo = "AAAA").apply { onAsociar() }

        viewModel.onOfertaPesajesPendientesRespondida(false)

        assertTrue(registroRepository.observeByAnimal("a-1").first().isEmpty())
        assertEquals("AAAA", chapetaRepository.findActivaByAnimal("a-1")!!.codigo)
        assertEquals(AsociarChapetaEvento.Asociada, viewModel.eventos.first())
    }
}
