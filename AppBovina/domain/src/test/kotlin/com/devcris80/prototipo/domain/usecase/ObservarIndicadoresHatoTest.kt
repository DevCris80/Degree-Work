package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Chapeta
import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.Evento
import com.devcris80.prototipo.domain.model.MotivoPesajePendiente
import com.devcris80.prototipo.domain.model.PesajePendiente
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.model.Registro
import com.devcris80.prototipo.domain.model.Sexo
import com.devcris80.prototipo.testing.FakeAnimalRepository
import com.devcris80.prototipo.testing.FakeChapetaRepository
import com.devcris80.prototipo.testing.FakeEventoRepository
import com.devcris80.prototipo.testing.FakePesajePendienteRepository
import com.devcris80.prototipo.testing.FakeRegistroRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ObservarIndicadoresHatoTest {
    private val animalRepository = FakeAnimalRepository()
    private val chapetaRepository = FakeChapetaRepository()
    private val registroRepository = FakeRegistroRepository()
    private val pesajePendienteRepository = FakePesajePendienteRepository()
    private val eventoRepository = FakeEventoRepository()
    private val dia = 24L * 60 * 60 * 1000
    private val ahora = 100 * dia
    private val observarIndicadoresHato = ObservarIndicadoresHato(
        animalRepository,
        chapetaRepository,
        registroRepository,
        pesajePendienteRepository,
        eventoRepository,
        clock = { ahora },
    )

    private fun animal(id: String, proposito: Proposito = Proposito.LECHE, etapa: Etapa = Etapa.VACA) = Animal(
        id, "perfil_local", "Animal $id", "Holstein", etapa.sexo, etapa, 1L, proposito = proposito,
    )

    private fun fecha(anio: Int, mes: Int, dia: Int = 15): Long =
        Calendar.getInstance().apply { clear(); set(anio, mes - 1, dia, 12, 0) }.timeInMillis

    private fun evento(id: String, idAnimal: String, tipo: String, fecha: Long) =
        Evento(id, idAnimal, "usuario-1", tipo, fecha, "detalle")

    @Test
    fun cuentaElInventarioPorPropositoYEtapaSoloConLosAnimalesActivos() = runTest {
        animalRepository.activos.value = listOf(
            animal("a1"),
            animal("a2"),
            animal("a3", Proposito.CARNE, Etapa.TORO),
        )

        val indicadores = observarIndicadoresHato().first()

        assertEquals(
            listOf(
                GrupoInventario(Proposito.LECHE, Etapa.VACA, 2),
                GrupoInventario(Proposito.CARNE, Etapa.TORO, 1),
            ),
            indicadores.inventario,
        )
    }

    @Test
    fun lasAlertasDeChapetaYDePesajeUsanSoloLoActivoYLosUltimos30Dias() = runTest {
        val conTodo = animal("a1")
        val sinChapeta = animal("a2")
        val pesajeViejo = animal("a3")
        animalRepository.activos.value = listOf(conTodo, sinChapeta, pesajeViejo)
        chapetaRepository.set(Chapeta("c1", "AAAA", "a1", 1L))
        chapetaRepository.set(Chapeta("c3", "CCCC", "a3", 1L))
        chapetaRepository.set(Chapeta("c2", "BBBB", "a2", 1L, fechaDesasociacion = 2L))
        registroRepository.setUltimo("a1", Registro("r1", "a1", "u", peso = 300f, timestamp = ahora - 29 * dia))
        registroRepository.setUltimo("a3", Registro("r3", "a3", "u", peso = 300f, timestamp = ahora - 31 * dia))

        val indicadores = observarIndicadoresHato().first()

        assertEquals(listOf(sinChapeta), indicadores.animalesSinChapetaActiva)
        assertEquals(listOf(sinChapeta, pesajeViejo), indicadores.animalesSinPesajeReciente)
    }

    @Test
    fun cuentaLosPesajesPendientesYDestacaElMasAntiguo() = runTest {
        fun pendiente(id: String, timestamp: Long) = PesajePendiente(
            id, "lectura-$id", "AAAA", 100f, timestamp, "u", MotivoPesajePendiente.CODIGO_SIN_CHAPETA_ACTIVA,
        ).also(pesajePendienteRepository::set)
        val masAntiguo = pendiente("p1", timestamp = 10L)
        pendiente("p2", timestamp = 20L)

        val indicadores = observarIndicadoresHato().first()

        assertEquals(2, indicadores.pesajesPendientes)
        assertEquals(masAntiguo, indicadores.pesajePendienteMasAntiguo)
    }

    @Test
    fun sinPesajesPendientesNoHayMasAntiguo() = runTest {
        val indicadores = observarIndicadoresHato().first()

        assertEquals(0, indicadores.pesajesPendientes)
        assertNull(indicadores.pesajePendienteMasAntiguo)
        assertTrue(indicadores.inventario.isEmpty())
    }

    @Test
    fun agrupaLosEventosPorMesDelMasRecienteYUnificaElNombreDelTipoConElMasUsado() = runTest {
        animalRepository.activos.value = listOf(animal("a1"))
        eventoRepository.set(
            "a1",
            listOf(
                evento("e1", "a1", "Vacunación", fecha(2026, 10)),
                evento("e2", "a1", " vacunacion ", fecha(2026, 10)),
                evento("e3", "a1", "Vacunación", fecha(2026, 10)),
                evento("e4", "a1", "BVD", fecha(2026, 9)),
            ),
        )
        // El evento de un Animal que no está activo no cuenta.
        eventoRepository.set("baja", listOf(evento("e5", "baja", "Vacunación", fecha(2026, 10))))

        val indicadores = observarIndicadoresHato().first()

        assertEquals(
            listOf(
                EventosDelMes(AnioMes(2026, 10), mapOf("Vacunación" to 3)),
                EventosDelMes(AnioMes(2026, 9), mapOf("BVD" to 1)),
            ),
            indicadores.eventosPorMes,
        )
    }
}
