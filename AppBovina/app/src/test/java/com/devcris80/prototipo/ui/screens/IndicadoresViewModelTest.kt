package com.devcris80.prototipo.ui.screens

import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Chapeta
import com.devcris80.prototipo.domain.model.Conciliacion
import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.Evento
import com.devcris80.prototipo.domain.model.MotivoPesajePendiente
import com.devcris80.prototipo.domain.model.PesajePendiente
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.model.Registro
import com.devcris80.prototipo.domain.model.ResolucionPesajePendiente
import com.devcris80.prototipo.domain.repository.EstadoSincronizacion
import com.devcris80.prototipo.domain.usecase.AnioMes
import com.devcris80.prototipo.domain.usecase.EventosDelMes
import com.devcris80.prototipo.domain.usecase.GrupoInventario
import com.devcris80.prototipo.domain.usecase.ObservarEvolucionPeso
import com.devcris80.prototipo.domain.usecase.ObservarIndicadoresHato
import com.devcris80.prototipo.testing.FakeAnimalRepository
import com.devcris80.prototipo.testing.FakeChapetaRepository
import com.devcris80.prototipo.testing.FakeEventoRepository
import com.devcris80.prototipo.testing.FakePesajePendienteRepository
import com.devcris80.prototipo.testing.FakeRegistroRepository
import com.devcris80.prototipo.testing.FakeSincronizacionRepository
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.UUID

class IndicadoresViewModelTest {
    private val animalRepository = FakeAnimalRepository()
    private val chapetaRepository = FakeChapetaRepository()
    private val registroRepository = FakeRegistroRepository()
    private val pesajePendienteRepository = FakePesajePendienteRepository()
    private val eventoRepository = FakeEventoRepository()
    private val sincronizacionRepository = FakeSincronizacionRepository()

    private fun crearViewModel() = IndicadoresViewModel(
        ObservarIndicadoresHato(
            animalRepository,
            chapetaRepository,
            registroRepository,
            pesajePendienteRepository,
            eventoRepository,
            clock = { AHORA },
        ),
        ObservarEvolucionPeso(registroRepository),
        animalRepository,
        chapetaRepository,
        sincronizacionRepository,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun animal(etapa: Etapa, proposito: Proposito) = Animal(
        idAnimal = UUID.randomUUID().toString(),
        idPerfilFinca = "perfil_local",
        nombre = "Animal ${UUID.randomUUID()}",
        raza = "Criollo",
        sexo = etapa.sexo,
        etapa = etapa,
        fechaNacimiento = 1L,
        proposito = proposito,
    )

    private fun pendiente(timestamp: Long, conciliacion: Conciliacion? = null) = PesajePendiente(
        idPesajePendiente = UUID.randomUUID().toString(),
        idLectura = UUID.randomUUID().toString(),
        codigo = "04A1B2C3",
        peso = 300f,
        timestamp = timestamp,
        idUsuario = "usuario",
        motivo = MotivoPesajePendiente.CODIGO_SIN_CHAPETA_ACTIVA,
        conciliacion = conciliacion,
    )

    private fun chapeta(animal: Animal, fechaDesasociacion: Long? = null) = Chapeta(
        idChapeta = UUID.randomUUID().toString(),
        codigo = UUID.randomUUID().toString(),
        idAnimal = animal.idAnimal,
        fechaAsociacion = 1L,
        fechaDesasociacion = fechaDesasociacion,
    )

    private fun registro(animal: Animal, timestamp: Long) = Registro(
        idRegistro = UUID.randomUUID().toString(),
        idAnimal = animal.idAnimal,
        idUsuario = "usuario",
        peso = 400f,
        timestamp = timestamp,
    )

    private fun TestScope.ultimoEstado(idAnimalElegido: String? = null): IndicadoresUiState {
        val viewModel = crearViewModel()
        val estados = mutableListOf<IndicadoresUiState>()
        val job = launch { viewModel.estado.collect { estados.add(it) } }
        idAnimalElegido?.let(viewModel::onAnimalElegido)
        advanceUntilIdle()
        job.cancel()
        return estados.last()
    }

    @Test
    fun conTresVacasDeLecheYDosNovillosDeCarne_elInventarioMuestraEsosConteos() = runTest {
        animalRepository.activos.value =
            List(3) { animal(Etapa.VACA, Proposito.LECHE) } + List(2) { animal(Etapa.NOVILLO, Proposito.CARNE) }

        val indicadores = checkNotNull(ultimoEstado().indicadores)

        assertEquals(
            listOf(
                GrupoInventario(Proposito.LECHE, Etapa.VACA, 3),
                GrupoInventario(Proposito.CARNE, Etapa.NOVILLO, 2),
            ),
            indicadores.inventario,
        )
    }

    @Test
    fun cuentaLosPesajesPendientesSinResolverYDaElMasAntiguo() = runTest {
        val masAntiguo = pendiente(timestamp = 10 * DIA)
        pesajePendienteRepository.set(pendiente(timestamp = 40 * DIA))
        pesajePendienteRepository.set(masAntiguo)
        val descartado = Conciliacion(ResolucionPesajePendiente.DESCARTADO, "usuario", fecha = 50 * DIA)
        pesajePendienteRepository.set(pendiente(timestamp = 5 * DIA, conciliacion = descartado))

        val indicadores = checkNotNull(ultimoEstado().indicadores)

        assertEquals(2, indicadores.pesajesPendientes)
        assertEquals(masAntiguo, indicadores.pesajePendienteMasAntiguo)
    }

    @Test
    fun laCompletitudDaLosAnimalesSinChapetaActivaYLosSinPesajeEnLosUltimos30Dias() = runTest {
        val completo = animal(Etapa.VACA, Proposito.LECHE)
        val sinChapetaYPesajeViejo = animal(Etapa.VACA, Proposito.LECHE)
        val nuncaPesado = animal(Etapa.TORO, Proposito.CARNE)
        animalRepository.activos.value = listOf(completo, sinChapetaYPesajeViejo, nuncaPesado)
        chapetaRepository.set(chapeta(completo))
        chapetaRepository.set(chapeta(sinChapetaYPesajeViejo, fechaDesasociacion = 50 * DIA))
        chapetaRepository.set(chapeta(nuncaPesado))
        registroRepository.set(completo.idAnimal, listOf(registro(completo, timestamp = AHORA - 10 * DIA)))
        registroRepository.set(
            sinChapetaYPesajeViejo.idAnimal,
            listOf(registro(sinChapetaYPesajeViejo, timestamp = AHORA - 40 * DIA)),
        )

        val indicadores = checkNotNull(ultimoEstado().indicadores)

        assertEquals(listOf(sinChapetaYPesajeViejo), indicadores.animalesSinChapetaActiva)
        assertEquals(listOf(sinChapetaYPesajeViejo, nuncaPesado), indicadores.animalesSinPesajeReciente)
    }

    @Test
    fun unAnimalDadoDeBaja_noApareceEnNingunIndicador() = runTest {
        val activo = animal(Etapa.VACA, Proposito.LECHE)
        // El repositorio solo da los activos: el dado de baja tiene datos, pero no está entre ellos.
        val dadoDeBaja = animal(Etapa.TORO, Proposito.CARNE).copy(fechaBaja = 60 * DIA)
        animalRepository.activos.value = listOf(activo)
        chapetaRepository.set(chapeta(activo))
        registroRepository.set(activo.idAnimal, listOf(registro(activo, timestamp = AHORA - DIA)))
        registroRepository.set(dadoDeBaja.idAnimal, listOf(registro(dadoDeBaja, timestamp = 10 * DIA)))

        val indicadores = checkNotNull(ultimoEstado().indicadores)

        assertEquals(listOf(GrupoInventario(Proposito.LECHE, Etapa.VACA, 1)), indicadores.inventario)
        assertEquals(emptyList<Animal>(), indicadores.animalesSinChapetaActiva)
        assertEquals(emptyList<Animal>(), indicadores.animalesSinPesajeReciente)
    }

    @Test
    fun sinAnimalesNiPendientes_hayIndicadoresVaciosEnLugarDeQuedarseCargando() = runTest {
        val indicadores = checkNotNull(ultimoEstado().indicadores)

        assertEquals(emptyList<GrupoInventario>(), indicadores.inventario)
        assertEquals(0, indicadores.pesajesPendientes)
        assertNull(indicadores.pesajePendienteMasAntiguo)
    }

    @Test
    fun con400kgElDia1Y430kgElDia31_laGananciaDiariaEsDeUnKiloPorDia() = runTest {
        val vaca = animal(Etapa.VACA, Proposito.LECHE)
        animalRepository.activos.value = listOf(vaca)
        registroRepository.set(
            vaca.idAnimal,
            listOf(registro(vaca, timestamp = 31 * DIA).copy(peso = 430f), registro(vaca, timestamp = DIA)),
        )

        val evolucion = checkNotNull(ultimoEstado(idAnimalElegido = vaca.idAnimal).evolucionPeso)

        assertEquals(listOf(400f, 430f), evolucion.registros.map { it.peso })
        assertEquals(1.0, checkNotNull(evolucion.gananciaDiariaKg), 1e-9)
    }

    @Test
    fun conUnSoloPesaje_noHayGananciaDiaria() = runTest {
        val vaca = animal(Etapa.VACA, Proposito.LECHE)
        animalRepository.activos.value = listOf(vaca)
        registroRepository.set(vaca.idAnimal, listOf(registro(vaca, timestamp = DIA)))

        val evolucion = checkNotNull(ultimoEstado(idAnimalElegido = vaca.idAnimal).evolucionPeso)

        assertEquals(1, evolucion.registros.size)
        assertNull(evolucion.gananciaDiariaKg)
    }

    @Test
    fun conDosPesajesAMenosDeUnDia_noHayGananciaDiaria() = runTest {
        val vaca = animal(Etapa.VACA, Proposito.LECHE)
        animalRepository.activos.value = listOf(vaca)
        val dosHoras = 2L * 60 * 60 * 1000
        registroRepository.set(
            vaca.idAnimal,
            listOf(registro(vaca, timestamp = DIA + dosHoras).copy(peso = 405f), registro(vaca, timestamp = DIA)),
        )

        val evolucion = checkNotNull(ultimoEstado(idAnimalElegido = vaca.idAnimal).evolucionPeso)

        assertEquals(2, evolucion.registros.size)
        assertNull(evolucion.gananciaDiariaKg)
    }

    @Test
    fun siElAnimalElegidoSeDaDeBaja_dejaDeMostrarseSuEvolucionDePeso() = runTest {
        val vaca = animal(Etapa.VACA, Proposito.LECHE)
        animalRepository.activos.value = listOf(vaca)
        registroRepository.set(vaca.idAnimal, listOf(registro(vaca, timestamp = DIA)))
        val viewModel = crearViewModel()
        val estados = mutableListOf<IndicadoresUiState>()
        val job = launch { viewModel.estado.collect { estados.add(it) } }
        viewModel.onAnimalElegido(vaca.idAnimal)
        advanceUntilIdle()

        animalRepository.activos.value = emptyList()
        advanceUntilIdle()

        assertNull(estados.last().idAnimalElegido)
        assertNull(estados.last().evolucionPeso)
        job.cancel()
    }

    @Test
    fun losAnimalesParaElegirLlevanSuCodigoDeChapetaActiva() = runTest {
        val conChapeta = animal(Etapa.VACA, Proposito.LECHE)
        val sinChapeta = animal(Etapa.TORO, Proposito.CARNE)
        animalRepository.activos.value = listOf(conChapeta, sinChapeta)
        val activa = chapeta(conChapeta)
        chapetaRepository.set(activa)

        val elegibles = ultimoEstado().animalesElegibles

        assertEquals(
            listOf(AnimalConCodigo(conChapeta, activa.codigo), AnimalConCodigo(sinChapeta, null)),
            elegibles,
        )
    }

    @Test
    fun muestraLosCambiosSinSubirYLaFechaDeLaUltimaSincronizacion() = runTest {
        sincronizacionRepository.estado.value = EstadoSincronizacion(cambiosSinSubir = 4, ultimaSincronizacion = 7 * DIA)

        val estado = ultimoEstado()

        assertEquals(4, estado.cambiosSinSubir)
        assertEquals(7 * DIA, estado.ultimaSincronizacion)
    }

    private fun evento(animal: Animal, tipo: String, fecha: Long, fechaBaja: Long? = null) = Evento(
        idEvento = UUID.randomUUID().toString(),
        idAnimal = animal.idAnimal,
        idUsuario = "usuario",
        tipoEvento = tipo,
        fecha = fecha,
        detalle = "",
        fechaBaja = fechaBaja,
    )

    private fun fecha(anio: Int, mes: Int, dia: Int): Long =
        Calendar.getInstance().apply { clear(); set(anio, mes - 1, dia, 12, 0) }.timeInMillis

    @Test
    fun cuentaLosEventosPorMesYPorTipo_sinLosDadosDeBajaNiLosDeAnimalesDadosDeBaja() = runTest {
        val vaca = animal(Etapa.VACA, Proposito.LECHE)
        val dadoDeBaja = animal(Etapa.TORO, Proposito.CARNE).copy(fechaBaja = fecha(2026, 10, 1))
        animalRepository.activos.value = listOf(vaca)
        eventoRepository.set(
            vaca.idAnimal,
            listOf(
                evento(vaca, "Vacunación", fecha(2026, 10, 20)),
                evento(vaca, " vacunacion", fecha(2026, 10, 5)),
                evento(vaca, "BVD", fecha(2026, 10, 3)),
                evento(vaca, "Tratamiento", fecha(2026, 10, 7)),
                evento(vaca, "Vacunación", fecha(2026, 9, 15)),
                evento(vaca, "bvd ", fecha(2026, 9, 10)),
                evento(vaca, "BVD", fecha(2026, 9, 2)),
                evento(vaca, "Vacunación", fecha(2026, 10, 8), fechaBaja = fecha(2026, 10, 9)),
            ),
        )
        eventoRepository.set(dadoDeBaja.idAnimal, listOf(evento(dadoDeBaja, "Tratamiento", fecha(2026, 9, 1))))

        val indicadores = checkNotNull(ultimoEstado().indicadores)

        assertEquals(
            listOf(
                EventosDelMes(AnioMes(2026, 10), mapOf("Vacunación" to 2, "Tratamiento" to 1, "BVD" to 1)),
                EventosDelMes(AnioMes(2026, 9), mapOf("Vacunación" to 1, "BVD" to 2)),
            ),
            indicadores.eventosPorMes,
        )
    }

    private companion object {
        const val DIA = 24L * 60 * 60 * 1000
        const val AHORA = 100 * DIA
    }
}
