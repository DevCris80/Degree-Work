package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.Registro
import com.devcris80.prototipo.testing.FakeRegistroRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ObservarEvolucionPesoTest {
    private val registroRepository = FakeRegistroRepository()
    private val observarEvolucionPeso = ObservarEvolucionPeso(registroRepository)

    private fun registro(id: String, peso: Float, timestamp: Long) =
        Registro(id, "animal-1", "usuario-1", peso = peso, timestamp = timestamp)

    private val dia = 24L * 60 * 60 * 1000

    @Test
    fun ordenaLosRegistrosDelMasAntiguoAlMasRecienteYCalculaLosKgPorDia() = runTest {
        registroRepository.set(
            "animal-1",
            listOf(registro("r3", 340f, 10 * dia), registro("r1", 300f, 0), registro("r2", 320f, 5 * dia)),
        )

        val evolucion = observarEvolucionPeso("animal-1").first()

        assertEquals(listOf("r1", "r2", "r3"), evolucion.registros.map { it.idRegistro })
        assertEquals(4.0, evolucion.gananciaDiariaKg!!, 0.0001)
    }

    @Test
    fun conMenosDeDosRegistrosNoHayGanancia() = runTest {
        registroRepository.set("animal-1", listOf(registro("r1", 300f, 0)))

        assertNull(observarEvolucionPeso("animal-1").first().gananciaDiariaKg)
    }

    @Test
    fun conMenosDeUnDiaEntreElPrimeroYElUltimoNoHayGanancia() = runTest {
        registroRepository.set("animal-1", listOf(registro("r1", 300f, 0), registro("r2", 305f, dia - 1)))

        assertNull(observarEvolucionPeso("animal-1").first().gananciaDiariaKg)
    }

    @Test
    fun conExactamenteUnDiaYaHayGananciaYPuedeSerNegativa() = runTest {
        registroRepository.set("animal-1", listOf(registro("r1", 300f, 0), registro("r2", 296f, dia)))

        assertEquals(-4.0, observarEvolucionPeso("animal-1").first().gananciaDiariaKg!!, 0.0001)
    }
}
