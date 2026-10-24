package com.devcris80.prototipo.ui.screens

import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Chapeta
import com.devcris80.prototipo.domain.model.Conciliacion
import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.MotivoPesajePendiente
import com.devcris80.prototipo.domain.model.PerfilFinca
import com.devcris80.prototipo.domain.model.PesajePendiente
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.model.ResolucionPesajePendiente
import com.devcris80.prototipo.domain.model.Rol
import com.devcris80.prototipo.domain.model.Sexo
import com.devcris80.prototipo.domain.model.Usuario
import com.devcris80.prototipo.domain.usecase.AsignarPesajesPendientes
import com.devcris80.prototipo.domain.usecase.RegistrarAnimal
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class RegistrarAnimalViewModelTest {
    private val cuentaRepository = FakeCuentaRepository()
    private val animalRepository = FakeAnimalRepository()
    private val chapetaRepository = FakeChapetaRepository()
    private val registroRepository = FakeRegistroRepository()
    private val pesajePendienteRepository = FakePesajePendienteRepository()

    private fun crearViewModel(codigoPrecargado: String? = null, avisoAnterior: String? = null) =
        RegistrarAnimalViewModel(
            codigoPrecargado,
            avisoAnterior,
            cuentaRepository,
            animalRepository,
            RegistrarAnimal(animalRepository, chapetaRepository, pesajePendienteRepository, FakeTransaccion()),
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
        cuentaRepository.perfil.value = PerfilFinca(idPerfilFinca = "perfil_local", nombreFinca = "Finca Test")
        cuentaRepository.usuario.value = Usuario(
            idUsuario = UUID.randomUUID().toString(),
            idPerfilFinca = "perfil_local",
            nombre = "Ana",
            rol = Rol.GANADERO,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun llenarFormularioValido(viewModel: RegistrarAnimalViewModel) {
        viewModel.onNombreCambiado("Manchas")
        viewModel.onRazaCambiada("Holstein")
        viewModel.onSexoSeleccionado(Sexo.HEMBRA)
        viewModel.onEtapaSeleccionada(Etapa.VACA)
        viewModel.onFechaNacimientoCambiada(1L)
        viewModel.onPropositoSeleccionado(Proposito.LECHE)
    }

    private fun pendiente(codigo: String, peso: Float = 150f, timestamp: Long = 1L) = PesajePendiente(
        idPesajePendiente = UUID.randomUUID().toString(),
        idLectura = UUID.randomUUID().toString(),
        codigo = codigo,
        peso = peso,
        timestamp = timestamp,
        idUsuario = UUID.randomUUID().toString(),
        motivo = MotivoPesajePendiente.CODIGO_SIN_CHAPETA_ACTIVA,
    )

    private fun progenitor(nombre: String, sexo: Sexo, etapa: Etapa) = Animal(
        idAnimal = UUID.randomUUID().toString(),
        idPerfilFinca = "perfil_local",
        nombre = nombre,
        raza = "Cebú",
        sexo = sexo,
        etapa = etapa,
        fechaNacimiento = 0L,
        proposito = Proposito.CARNE,
    )

    @Test
    fun losCandidatosAPadreSonLosMachosYAMadreLasHembras() {
        val toro = progenitor("Zeus", Sexo.MACHO, Etapa.TORO)
        val vaca = progenitor("Lola", Sexo.HEMBRA, Etapa.VACA)
        animalRepository.activos.value = listOf(toro, vaca)

        val estado = crearViewModel().estado.value

        assertEquals(listOf(toro), estado.candidatosPadre)
        assertEquals(listOf(vaca), estado.candidatosMadre)
    }

    @Test
    fun unToroDadoDeBajaSigueSiendoCandidato_despuesDeLosActivos() {
        val vendido = progenitor("Atlas", Sexo.MACHO, Etapa.TORO).copy(fechaBaja = 5L)
        val activo = progenitor("Zeus", Sexo.MACHO, Etapa.TORO)
        animalRepository.set(vendido)
        animalRepository.activos.value = listOf(activo)

        assertEquals(listOf(activo, vendido), crearViewModel().estado.value.candidatosPadre)
    }

    @Test
    fun guardarConPadreYMadre_losGuardaEnElAnimal() = runTest {
        val toro = progenitor("Zeus", Sexo.MACHO, Etapa.TORO)
        val vaca = progenitor("Lola", Sexo.HEMBRA, Etapa.VACA)
        animalRepository.activos.value = listOf(toro, vaca)
        val viewModel = crearViewModel()
        llenarFormularioValido(viewModel)
        viewModel.onPadreSeleccionado(toro.idAnimal)
        viewModel.onMadreSeleccionada(vaca.idAnimal)

        viewModel.onGuardar()

        assertEquals(RegistrarAnimalEvento.Guardado, viewModel.eventos.first())
        val guardado = animalRepository.insertados.single()
        assertEquals(toro.idAnimal, guardado.idPadre)
        assertEquals(vaca.idAnimal, guardado.idMadre)
    }

    @Test
    fun quitarElPadreElegido_loDejaSinPadre() {
        val toro = progenitor("Zeus", Sexo.MACHO, Etapa.TORO)
        animalRepository.activos.value = listOf(toro)
        val viewModel = crearViewModel()
        viewModel.onPadreSeleccionado(toro.idAnimal)

        viewModel.onPadreSeleccionado(null)

        assertEquals(null, viewModel.estado.value.idPadre)
    }

    @Test
    fun codigoPrecargado_quedaDeSoloLecturaConElAviso() {
        val viewModel = crearViewModel(codigoPrecargado = "04A1B2C3", avisoAnterior = "Este tag estuvo asociado a Canela")

        val estado = viewModel.estado.value
        assertEquals("04A1B2C3", estado.codigo)
        assertTrue(estado.codigoSoloLectura)
        assertEquals("Este tag estuvo asociado a Canela", estado.aviso)
    }

    @Test
    fun cambiarSexo_actualizaEtapasDisponiblesYLimpiaLaEtapaElegida() {
        val viewModel = crearViewModel()
        viewModel.onSexoSeleccionado(Sexo.MACHO)
        viewModel.onEtapaSeleccionada(Etapa.TORO)

        viewModel.onSexoSeleccionado(Sexo.HEMBRA)

        val estado = viewModel.estado.value
        assertEquals(listOf(Etapa.VACA, Etapa.TERNERA), estado.etapasDisponibles)
        assertEquals(null, estado.etapa)
    }

    @Test
    fun formularioIncompleto_noSePuedeGuardar() {
        val viewModel = crearViewModel()
        viewModel.onNombreCambiado("Manchas")

        assertFalse(viewModel.estado.value.puedeGuardar)
    }

    @Test
    fun guardarSinCodigo_emiteGuardadoYNoCreaChapeta() = runTest {
        val viewModel = crearViewModel()
        llenarFormularioValido(viewModel)

        viewModel.onGuardar()

        assertEquals(RegistrarAnimalEvento.Guardado, viewModel.eventos.first())
        assertTrue(chapetaRepository.observeActivas().first().isEmpty())
    }

    @Test
    fun guardarConCodigoYaActivo_noGuardaYMarcaElEstado() = runTest {
        val otroAnimal = UUID.randomUUID().toString()
        chapetaRepository.set(Chapeta(UUID.randomUUID().toString(), "04A1B2C3", otroAnimal, fechaAsociacion = 1L))
        val viewModel = crearViewModel(codigoPrecargado = "04A1B2C3")
        llenarFormularioValido(viewModel)

        viewModel.onGuardar()

        assertTrue(viewModel.estado.value.codigoYaActivo)
    }

    @Test
    fun guardarConPesajesPendientesDelCodigo_ofreceAsignarlosSinEmitirGuardadoTodavia() = runTest {
        pesajePendienteRepository.set(pendiente("04A1B2C3"))
        val viewModel = crearViewModel(codigoPrecargado = "04A1B2C3")
        llenarFormularioValido(viewModel)

        viewModel.onGuardar()

        val oferta = viewModel.estado.value.ofertaPesajesPendientes
        assertEquals("04A1B2C3", oferta?.codigo)
        assertEquals(1, oferta?.pesajes?.size)
    }

    @Test
    fun aceptarLaOferta_asignaLosPesajesYLuegoEmiteGuardado() = runTest {
        pesajePendienteRepository.set(pendiente("04A1B2C3"))
        val viewModel = crearViewModel(codigoPrecargado = "04A1B2C3")
        llenarFormularioValido(viewModel)
        viewModel.onGuardar()

        viewModel.onOfertaPesajesPendientesRespondida(true)

        assertEquals(RegistrarAnimalEvento.Guardado, viewModel.eventos.first())
        assertEquals(
            ResolucionPesajePendiente.ASIGNADO,
            pesajePendienteRepository.todos.value.first().conciliacion?.resolucion,
        )
    }

    @Test
    fun rechazarLaOferta_noAsignaPeroIgualEmiteGuardado() = runTest {
        pesajePendienteRepository.set(pendiente("04A1B2C3"))
        val viewModel = crearViewModel(codigoPrecargado = "04A1B2C3")
        llenarFormularioValido(viewModel)
        viewModel.onGuardar()

        viewModel.onOfertaPesajesPendientesRespondida(false)

        assertEquals(RegistrarAnimalEvento.Guardado, viewModel.eventos.first())
        assertTrue(pesajePendienteRepository.todos.value.first().conciliacion == null)
    }

    @Test
    fun aceptarLaOfertaConPendienteYaResuelto_marcaElErrorYIgualEmiteGuardado() = runTest {
        val pesajePendiente = pendiente("04A1B2C3")
        pesajePendienteRepository.set(pesajePendiente)
        val viewModel = crearViewModel(codigoPrecargado = "04A1B2C3")
        llenarFormularioValido(viewModel)
        viewModel.onGuardar()
        pesajePendienteRepository.conciliar(
            pesajePendiente.idPesajePendiente,
            Conciliacion(ResolucionPesajePendiente.DESCARTADO, UUID.randomUUID().toString(), 2L),
        )

        viewModel.onOfertaPesajesPendientesRespondida(true)

        assertEquals(RegistrarAnimalEvento.Guardado, viewModel.eventos.first())
        assertTrue(viewModel.estado.value.errorAsignacionPendientes)
    }
}
