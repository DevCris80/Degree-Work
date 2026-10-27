package com.devcris80.prototipo.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EventoTest {
    private fun evento(fecha: Long = 10L, biologico: String? = null, dosis: String? = null, via: String? = null) =
        Evento("e1", "a1", "u1", "Vacunación", fecha, "Ciclo I", biologico = biologico, dosis = dosis, via = via)

    @Test
    fun unEventoConFechaPosteriorAAhoraEstaProgramado() {
        assertTrue(evento(fecha = 11L).estaProgramado(ahora = 10L))
        assertFalse("la fecha de ahora ya llegó", evento(fecha = 10L).estaProgramado(ahora = 10L))
        assertFalse(evento(fecha = 9L).estaProgramado(ahora = 10L))
    }

    @Test
    fun elResumenDeLaVacunacionJuntaBiologicoDosisYViaLosQueTenga() {
        assertEquals("Aftosa · 2 ml · Subcutánea", evento(biologico = "Aftosa", dosis = "2 ml", via = "Subcutánea").resumenDeVacunacion)
        assertEquals("Aftosa · Subcutánea", evento(biologico = "Aftosa", via = "Subcutánea").resumenDeVacunacion)
    }

    @Test
    fun unEventoSinCamposDeVacunacionNoTieneResumen() {
        assertNull(evento().resumenDeVacunacion)
    }
}
