package com.devcris80.prototipo.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class AnioMesTest {
    private fun fecha(anio: Int, mes: Int, dia: Int, hora: Int = 12): Long =
        Calendar.getInstance().apply { clear(); set(anio, mes - 1, dia, hora, 0) }.timeInMillis

    @Test
    fun deUnaFechaSacaElAnioYElMesDe1a12() {
        assertEquals(AnioMes(2026, 1), AnioMes.de(fecha(2026, 1, 31)))
        assertEquals(AnioMes(2026, 12), AnioMes.de(fecha(2026, 12, 1)))
    }

    @Test
    fun elInicioDelMesEsSuPrimerInstante() {
        assertEquals(AnioMes(2026, 10), AnioMes.de(AnioMes(2026, 10).inicio()))
        assertEquals(fecha(2026, 10, 1, hora = 0), AnioMes(2026, 10).inicio())
    }

    @Test
    fun seOrdenaPrimeroPorAnioYLuegoPorMes() {
        assertTrue(AnioMes(2025, 12) < AnioMes(2026, 1))
        assertTrue(AnioMes(2026, 9) < AnioMes(2026, 10))
        assertEquals(listOf(AnioMes(2025, 12), AnioMes(2026, 1), AnioMes(2026, 10)),
            listOf(AnioMes(2026, 10), AnioMes(2025, 12), AnioMes(2026, 1)).sorted())
    }
}
