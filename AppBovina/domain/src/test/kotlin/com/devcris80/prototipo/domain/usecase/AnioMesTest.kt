package com.devcris80.prototipo.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class AnioMesTest {
    private fun fecha(anio: Int, mes: Int, dia: Int): Long =
        Calendar.getInstance().apply { clear(); set(anio, mes - 1, dia, 12, 0) }.timeInMillis

    @Test
    fun deUnaFechaSacaElAnioYElMesDe1a12() {
        assertEquals(AnioMes(2026, 1), AnioMes.de(fecha(2026, 1, 31)))
        assertEquals(AnioMes(2026, 12), AnioMes.de(fecha(2026, 12, 1)))
    }

    @Test
    fun elInicioDelMesEsSuPrimerInstante() {
        val inicio = Calendar.getInstance().apply { timeInMillis = AnioMes(2026, 10).inicio() }

        assertEquals(2026, inicio.get(Calendar.YEAR))
        assertEquals(Calendar.OCTOBER, inicio.get(Calendar.MONTH))
        assertEquals(1, inicio.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, inicio.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, inicio.get(Calendar.MINUTE))
        assertEquals(AnioMes(2026, 10), AnioMes.de(inicio.timeInMillis))
    }

    @Test
    fun seOrdenaPrimeroPorAnioYLuegoPorMes() {
        assertTrue(AnioMes(2025, 12) < AnioMes(2026, 1))
        assertTrue(AnioMes(2026, 9) < AnioMes(2026, 10))
        assertEquals(listOf(AnioMes(2025, 12), AnioMes(2026, 1), AnioMes(2026, 10)),
            listOf(AnioMes(2026, 10), AnioMes(2025, 12), AnioMes(2026, 1)).sorted())
    }
}
