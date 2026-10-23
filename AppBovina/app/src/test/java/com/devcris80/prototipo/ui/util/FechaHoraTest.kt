package com.devcris80.prototipo.ui.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class FechaHoraTest {
    private val zonaOriginal = TimeZone.getDefault()
    private val utc = TimeZone.getTimeZone("UTC")

    @Before
    fun setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Bogota"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(zonaOriginal)
    }

    private fun local(anio: Int, mes: Int, dia: Int, hora: Int, minuto: Int, segundo: Int = 0): Long =
        Calendar.getInstance().apply { clear(); set(anio, mes - 1, dia, hora, minuto, segundo) }.timeInMillis

    private fun medianocheUtc(anio: Int, mes: Int, dia: Int): Long =
        Calendar.getInstance(utc).apply { clear(); set(anio, mes - 1, dia) }.timeInMillis

    @Test
    fun aLas9DeLaNoche_elDiaUtcSigueSiendoElDiaLocal() {
        // A las 21:30 en Bogotá ya es el día siguiente en UTC.
        assertEquals(medianocheUtc(2026, 10, 23), diaEnUtc(local(2026, 10, 23, 21, 30)))
    }

    @Test
    fun conDia_cambiaElDiaYConservaLaHoraLocal() {
        assertEquals(local(2026, 10, 20, 8, 15), conDia(local(2026, 10, 23, 8, 15), medianocheUtc(2026, 10, 20)))
    }

    @Test
    fun conHora_cambiaHoraYMinutoYDejaElSegundoEnCero() {
        assertEquals(local(2026, 10, 23, 17, 5), conHora(local(2026, 10, 23, 8, 15, 42), 17, 5))
    }
}
