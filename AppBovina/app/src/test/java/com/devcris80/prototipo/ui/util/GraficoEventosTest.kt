package com.devcris80.prototipo.ui.util

import com.devcris80.prototipo.domain.usecase.AnioMes
import com.devcris80.prototipo.domain.usecase.EventosDelMes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GraficoEventosTest {
    private fun delMes(anio: Int, mes: Int, vararg porTipo: Pair<String, Int>) =
        EventosDelMes(AnioMes(anio, mes), porTipo.toMap())

    @Test
    fun sinEventosNoHayBarras() {
        assertTrue(barrasPorMes(emptyList()).isEmpty())
    }

    @Test
    fun lasBarrasVanDelMesMasAntiguoAlMasRecienteAunqueLleguenAlReves() {
        // Llegan del más reciente al más antiguo, como los entrega el caso de uso.
        val barras = barrasPorMes(listOf(delMes(2026, 3, "vacuna" to 2), delMes(2026, 2, "vacuna" to 1)))

        assertEquals(listOf(AnioMes(2026, 2), AnioMes(2026, 3)), barras.map { it.mes })
    }

    @Test
    fun cadaBarraSumaTodosLosTiposDeSuMes() {
        val barras = barrasPorMes(listOf(delMes(2026, 3, "vacuna" to 2, "parto" to 1)))

        assertEquals(3, barras.single().total)
    }

    @Test
    fun unMesSinEventosEntreDosConEventosAparecePeroEnCero() {
        val barras = barrasPorMes(listOf(delMes(2026, 4, "vacuna" to 1), delMes(2026, 1, "vacuna" to 4)))

        assertEquals(
            listOf(AnioMes(2026, 1), AnioMes(2026, 2), AnioMes(2026, 3), AnioMes(2026, 4)),
            barras.map { it.mes },
        )
        assertEquals(listOf(4, 0, 0, 1), barras.map { it.total })
    }

    @Test
    fun losMesesCruzanElCambioDeAnio() {
        val barras = barrasPorMes(listOf(delMes(2026, 1, "vacuna" to 1), delMes(2025, 11, "vacuna" to 1)))

        assertEquals(listOf(AnioMes(2025, 11), AnioMes(2025, 12), AnioMes(2026, 1)), barras.map { it.mes })
    }

    @Test
    fun laAlturaEsRelativaAlMesConMasEventos() {
        val barras = barrasPorMes(listOf(delMes(2026, 2, "vacuna" to 2), delMes(2026, 1, "vacuna" to 4)))

        assertEquals(1f, barras.maxOf { it.altura }, 0.0001f)
        assertEquals(1f, barras.first { it.total == 4 }.altura, 0.0001f)
        assertEquals(0.5f, barras.first { it.total == 2 }.altura, 0.0001f)
    }

    @Test
    fun unMesEnCeroTieneAlturaCero() {
        val barras = barrasPorMes(listOf(delMes(2026, 3, "vacuna" to 1), delMes(2026, 1, "vacuna" to 1)))

        assertEquals(0f, barras[1].altura, 0.0001f)
    }

    @Test
    fun conMasDeLosMesesMaximosSeQuedanLosMasRecientes() {
        val eventos = (1..12).map { delMes(2025, it, "vacuna" to 1) } + delMes(2026, 1, "vacuna" to 1)

        val barras = barrasPorMes(eventos, maximoMeses = 12)

        assertEquals(12, barras.size)
        assertEquals(AnioMes(2025, 2), barras.first().mes)
        assertEquals(AnioMes(2026, 1), barras.last().mes)
    }
}
