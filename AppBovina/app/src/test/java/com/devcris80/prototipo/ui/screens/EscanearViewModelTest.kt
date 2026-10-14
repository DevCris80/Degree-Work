package com.devcris80.prototipo.ui.screens

import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Chapeta
import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.model.Sexo
import com.devcris80.prototipo.domain.repository.AnimalRepository
import com.devcris80.prototipo.domain.repository.ChapetaRepository
import com.devcris80.prototipo.domain.usecase.LiberarChapeta
import com.devcris80.prototipo.domain.usecase.ResolverDestinoEscaneo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

private class FakeAnimalRepository : AnimalRepository {
    val animales = mutableMapOf<String, Animal>()

    override suspend fun insert(animal: Animal) {
        animales[animal.idAnimal] = animal
    }

    override fun observeActivos(): Flow<List<Animal>> = MutableStateFlow(animales.values.toList())

    override fun observeById(idAnimal: String): Flow<Animal?> = MutableStateFlow(animales[idAnimal])

    override suspend fun findById(idAnimal: String): Animal? = animales[idAnimal]

    override suspend fun darDeBaja(idAnimal: String, fecha: Long) {
        animales[idAnimal]?.let { animales[idAnimal] = it.copy(fechaBaja = fecha) }
    }
}

private class FakeChapetaRepository : ChapetaRepository {
    val chapetas = mutableMapOf<String, Chapeta>()

    override suspend fun insert(chapeta: Chapeta) {
        chapetas[chapeta.idChapeta] = chapeta
    }

    override suspend fun findActivaByCodigo(codigo: String): Chapeta? =
        chapetas.values.firstOrNull { it.codigo == codigo && it.fechaDesasociacion == null }

    override suspend fun findUltimaByCodigo(codigo: String): Chapeta? =
        chapetas.values.filter { it.codigo == codigo }.maxByOrNull { it.fechaAsociacion }

    override fun observeActivas(): Flow<List<Chapeta>> =
        MutableStateFlow(chapetas.values.filter { it.fechaDesasociacion == null })

    override fun observeActivaByAnimal(idAnimal: String): Flow<Chapeta?> =
        MutableStateFlow(chapetas.values.firstOrNull { it.idAnimal == idAnimal && it.fechaDesasociacion == null })

    override suspend fun liberar(idChapeta: String, fecha: Long) {
        chapetas[idChapeta]?.let { chapetas[idChapeta] = it.copy(fechaDesasociacion = fecha) }
    }
}

class EscanearViewModelTest {
    private val animalRepository = FakeAnimalRepository()
    private val chapetaRepository = FakeChapetaRepository()
    private val viewModel = EscanearViewModel(
        ResolverDestinoEscaneo(chapetaRepository, animalRepository),
        LiberarChapeta(chapetaRepository, clock = { 2L }),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
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
        animalRepository.animales[manchas.idAnimal] = manchas
        chapetaRepository.chapetas[UUID.randomUUID().toString()] = chapeta(manchas.idAnimal, "04A1B2C3")

        viewModel.onCodigoLeido("04A1B2C3")

        assertEquals(EscanearEvento.AbrirAnimal(manchas.idAnimal), viewModel.eventos.first())
        assertEquals(EscanearUiState.EsperandoTag, viewModel.estado.value)
    }

    @Test
    fun tagDeAnimalDadoDeBaja_quedaEnEstadoDadoDeBaja() = runTest {
        val canela = animal("Canela", fechaBaja = 5L)
        animalRepository.animales[canela.idAnimal] = canela
        val idChapeta = UUID.randomUUID().toString()
        chapetaRepository.chapetas[idChapeta] = chapeta(canela.idAnimal, "04A1B2C3").copy(idChapeta = idChapeta)

        viewModel.onCodigoLeido("04A1B2C3")

        assertEquals(
            EscanearUiState.AnimalDadoDeBaja(idChapeta, "Canela", "04A1B2C3"),
            viewModel.estado.value,
        )
    }

    @Test
    fun liberarDesdeDadoDeBaja_reresuelveYRegistraConAviso() = runTest {
        val canela = animal("Canela", fechaBaja = 5L)
        animalRepository.animales[canela.idAnimal] = canela
        val idChapeta = UUID.randomUUID().toString()
        chapetaRepository.chapetas[idChapeta] = chapeta(canela.idAnimal, "04A1B2C3").copy(idChapeta = idChapeta)
        viewModel.onCodigoLeido("04A1B2C3")

        viewModel.onLiberarChapeta()

        assertEquals(
            EscanearEvento.Registrar("04A1B2C3", "Este tag estuvo asociado a Canela"),
            viewModel.eventos.first(),
        )
        assertEquals(2L, chapetaRepository.chapetas[idChapeta]!!.fechaDesasociacion)
    }

    @Test
    fun tagNuncaVisto_emiteRegistrarSinAviso() = runTest {
        viewModel.onCodigoLeido("FFEEDD00")

        assertEquals(EscanearEvento.Registrar("FFEEDD00", null), viewModel.eventos.first())
    }

    @Test
    fun cancelarDesdeDadoDeBaja_vuelveAEsperandoTag() = runTest {
        val canela = animal("Canela", fechaBaja = 5L)
        animalRepository.animales[canela.idAnimal] = canela
        chapetaRepository.chapetas[UUID.randomUUID().toString()] = chapeta(canela.idAnimal, "04A1B2C3")
        viewModel.onCodigoLeido("04A1B2C3")

        viewModel.onCancelar()

        assertEquals(EscanearUiState.EsperandoTag, viewModel.estado.value)
    }
}
