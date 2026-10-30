package com.devcris80.prototipo.ui.screens

import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Chapeta
import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.model.Rol
import com.devcris80.prototipo.domain.model.Sexo
import com.devcris80.prototipo.domain.model.Usuario
import com.devcris80.prototipo.domain.usecase.LiberarChapeta
import com.devcris80.prototipo.domain.usecase.ResolverDestinoEscaneo
import com.devcris80.prototipo.testing.FakeAnimalRepository
import com.devcris80.prototipo.testing.FakeChapetaRepository
import com.devcris80.prototipo.testing.FakeCuentaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.UUID

class EscanearViewModelTest {
    private val animalRepository = FakeAnimalRepository()
    private val chapetaRepository = FakeChapetaRepository()
    private val cuentaRepository = FakeCuentaRepository()
    private val viewModel = EscanearViewModel(
        ResolverDestinoEscaneo(chapetaRepository, animalRepository),
        LiberarChapeta(chapetaRepository, cuentaRepository, clock = { 2L }),
        cuentaRepository,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        conUsuario(Rol.GANADERO)
    }

    private fun conUsuario(rol: Rol) {
        cuentaRepository.usuario.value = Usuario("usuario-1", "perfil_local", "Ana", rol)
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

    private fun chapeta(animalId: String, codigo: String, fechaDesasociacion: Long? = null) = Chapeta(
        idChapeta = UUID.randomUUID().toString(),
        codigo = codigo,
        idAnimal = animalId,
        fechaAsociacion = 1L,
        fechaDesasociacion = fechaDesasociacion,
    )

    @Test
    fun tagDeAnimalActivo_emiteAbrirAnimal() = runTest {
        val manchas = animal("Manchas")
        animalRepository.set(manchas)
        chapetaRepository.set(chapeta(manchas.idAnimal, "04A1B2C3"))

        viewModel.onCodigoLeido("04A1B2C3")

        assertEquals(EscanearEvento.AbrirAnimal(manchas.idAnimal), viewModel.eventos.first())
        assertEquals(EscanearUiState.EsperandoTag, viewModel.estado.value)
    }

    @Test
    fun tagDeAnimalDadoDeBaja_quedaEnEstadoDadoDeBaja() = runTest {
        val canela = animal("Canela", fechaBaja = 5L)
        animalRepository.set(canela)
        val activa = chapeta(canela.idAnimal, "04A1B2C3")
        chapetaRepository.set(activa)

        viewModel.onCodigoLeido("04A1B2C3")

        assertEquals(
            EscanearUiState.AnimalDadoDeBaja(activa.idChapeta, "Canela", "04A1B2C3"),
            viewModel.estado.value,
        )
    }

    @Test
    fun elGanaderoPuedeLiberarLaChapeta() = runTest {
        assertEquals(true, viewModel.puedeLiberarChapeta.first())
    }

    @Test
    fun elOperarioNoPuedeLiberarLaChapetaNiAunqueSeLlameLaAccion() = runTest {
        conUsuario(Rol.OPERARIO)
        val canela = animal("Canela", fechaBaja = 5L)
        animalRepository.set(canela)
        val activa = chapeta(canela.idAnimal, "04A1B2C3")
        chapetaRepository.set(activa)
        viewModel.onCodigoLeido("04A1B2C3")

        viewModel.onLiberarChapeta()

        assertEquals(false, viewModel.puedeLiberarChapeta.first())
        assertEquals(EscanearUiState.AnimalDadoDeBaja(activa.idChapeta, "Canela", "04A1B2C3"), viewModel.estado.value)
        assertEquals(emptyList<String>(), chapetaRepository.liberadas)
    }

    @Test
    fun siCambiaElRolDespuesDeEscanearElPermisoSeActualiza() = runTest {
        val canela = animal("Canela", fechaBaja = 5L)
        animalRepository.set(canela)
        chapetaRepository.set(chapeta(canela.idAnimal, "04A1B2C3"))
        viewModel.onCodigoLeido("04A1B2C3")
        assertEquals(true, viewModel.puedeLiberarChapeta.first())

        conUsuario(Rol.OPERARIO)

        assertEquals(false, viewModel.puedeLiberarChapeta.first())
    }

    @Test
    fun liberarDesdeDadoDeBaja_reresuelveYOfreceRegistrarOAsociarConAviso() = runTest {
        val canela = animal("Canela", fechaBaja = 5L)
        animalRepository.set(canela)
        val activa = chapeta(canela.idAnimal, "04A1B2C3")
        chapetaRepository.set(activa)
        viewModel.onCodigoLeido("04A1B2C3")

        viewModel.onLiberarChapeta()

        assertEquals(
            EscanearUiState.CodigoSinChapeta("04A1B2C3", "Este tag estuvo asociado a Canela"),
            viewModel.estado.value,
        )
        assertEquals(2L, chapetaRepository.chapeta(activa.idChapeta)!!.fechaDesasociacion)
    }

    @Test
    fun tagNuncaVisto_ofreceRegistrarOAsociar() = runTest {
        viewModel.onCodigoLeido("FFEEDD00")

        assertEquals(EscanearUiState.CodigoSinChapeta("FFEEDD00", null), viewModel.estado.value)
    }

    @Test
    fun registrarNuevoDesdeUnTagSinChapeta_emiteRegistrarConElAviso() = runTest {
        val canela = animal("Canela")
        animalRepository.set(canela)
        chapetaRepository.set(chapeta(canela.idAnimal, "04A1B2C3", fechaDesasociacion = 4L))
        viewModel.onCodigoLeido("04A1B2C3")

        viewModel.onRegistrarNuevo()

        assertEquals(
            EscanearEvento.Registrar("04A1B2C3", "Este tag estuvo asociado a Canela"),
            viewModel.eventos.first(),
        )
        assertEquals(EscanearUiState.EsperandoTag, viewModel.estado.value)
    }

    @Test
    fun asociarAUnoExistenteDesdeUnTagSinChapeta_emiteAsociarConElCodigo() = runTest {
        viewModel.onCodigoLeido("FFEEDD00")

        viewModel.onAsociarAExistente()

        assertEquals(EscanearEvento.Asociar("FFEEDD00"), viewModel.eventos.first())
        assertEquals(EscanearUiState.EsperandoTag, viewModel.estado.value)
    }

    @Test
    fun registrarOAsociarSinUnTagSinChapeta_noHaceNada() = runTest {
        viewModel.onRegistrarNuevo()
        viewModel.onAsociarAExistente()

        assertEquals(EscanearUiState.EsperandoTag, viewModel.estado.value)
    }

    @Test
    fun cancelarDesdeDadoDeBaja_vuelveAEsperandoTag() = runTest {
        val canela = animal("Canela", fechaBaja = 5L)
        animalRepository.set(canela)
        chapetaRepository.set(chapeta(canela.idAnimal, "04A1B2C3"))
        viewModel.onCodigoLeido("04A1B2C3")

        viewModel.onCancelar()

        assertEquals(EscanearUiState.EsperandoTag, viewModel.estado.value)
    }
}
