package com.devcris80.prototipo.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class EdadTest {
    private fun fecha(anio: Int, mes: Int, dia: Int): Long =
        Calendar.getInstance().apply { set(anio, mes, dia, 12, 0, 0) }.timeInMillis

    @Test
    fun cumpleanosNoLlegadoCuentaUnMesMenos() {
        val nacimiento = fecha(2024, Calendar.MARCH, 15)
        val hoy = fecha(2026, Calendar.JULY, 10)
        assertEquals("2 años, 3 meses", formatearEdad(nacimiento, hoy))
    }

    @Test
    fun mismoDiaDelMesCuentaMesCompleto() {
        val nacimiento = fecha(2024, Calendar.MARCH, 15)
        val hoy = fecha(2026, Calendar.JULY, 15)
        assertEquals("2 años, 4 meses", formatearEdad(nacimiento, hoy))
    }

    @Test
    fun fechaFuturaNoDaEdadNegativa() {
        val nacimiento = fecha(2026, Calendar.JULY, 15)
        val hoy = fecha(2026, Calendar.JULY, 1)
        assertEquals("0 años, 0 meses", formatearEdad(nacimiento, hoy))
    }
}
