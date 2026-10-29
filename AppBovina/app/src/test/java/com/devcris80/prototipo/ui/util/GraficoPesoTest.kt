package com.devcris80.prototipo.ui.util

import com.devcris80.prototipo.domain.model.Registro
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GraficoPesoTest {
    private fun registro(id: String, peso: Float, timestamp: Long, fechaBaja: Long? = null) =
        Registro(idRegistro = id, idAnimal = "animal-1", idUsuario = "usuario", peso = peso, timestamp = timestamp, fechaBaja = fechaBaja)

    @Test
    fun conCeroRegistrosNoHayGrafico() {
        assertNull(datosDelGrafico(emptyList()))
    }

    @Test
    fun conUnSoloRegistroNoHayGrafico() {
        assertNull(datosDelGrafico(listOf(registro("r1", 300f, 1_000))))
    }

    @Test
    fun conDosRegistrosHayGraficoConLosPuntosDeIzquierdaADerechaPorFecha() {
        // Llegan del más reciente al más antiguo, como los entrega el Detalle.
        val datos = datosDelGrafico(listOf(registro("r2", 330f, 3_000), registro("r1", 300f, 1_000)))!!

        assertEquals(2, datos.puntos.size)
        assertEquals(0f, datos.puntos.first().x, 0.0001f)
        assertEquals(1f, datos.puntos.last().x, 0.0001f)
        assertTrue("el segundo pesaje es más pesado, así que queda más arriba", datos.puntos.last().y > datos.puntos.first().y)
        assertEquals(300f, datos.pesoMinimo, 0.0001f)
        assertEquals(330f, datos.pesoMaximo, 0.0001f)
        assertEquals(1_000L, datos.primeraFecha)
        assertEquals(3_000L, datos.ultimaFecha)
    }

    @Test
    fun losRegistrosDadosDeBajaNoAparecen() {
        val datos = datosDelGrafico(
            listOf(
                registro("r3", 360f, 5_000),
                registro("r2", 900f, 3_000, fechaBaja = 4_000),
                registro("r1", 300f, 1_000),
            ),
        )!!

        assertEquals(2, datos.puntos.size)
        assertEquals(360f, datos.pesoMaximo, 0.0001f)
    }

    @Test
    fun siSoloQuedaUnRegistroActivoNoHayGrafico() {
        assertNull(datosDelGrafico(listOf(registro("r2", 330f, 3_000), registro("r1", 300f, 1_000, fechaBaja = 2_000))))
    }

    @Test
    fun conElMismoPesoEnTodosLosRegistrosLosPuntosQuedanAlMedioYNoSeDivideEntreCero() {
        val datos = datosDelGrafico(listOf(registro("r2", 300f, 3_000), registro("r1", 300f, 1_000)))!!

        datos.puntos.forEach { assertEquals(0.5f, it.y, 0.0001f) }
    }

    @Test
    fun conLaMismaFechaEnTodosLosRegistrosLosPuntosQuedanAlMedioHorizontalmente() {
        val datos = datosDelGrafico(listOf(registro("r2", 330f, 1_000), registro("r1", 300f, 1_000)))!!

        datos.puntos.forEach { assertEquals(0.5f, it.x, 0.0001f) }
    }

    @Test
    fun todosLosPuntosCaenDentroDelAreaDelGrafico() {
        val datos = datosDelGrafico(
            listOf(registro("r3", 420f, 9_000), registro("r2", 310f, 5_000), registro("r1", 250f, 1_000)),
        )
        assertNotNull(datos)

        datos!!.puntos.forEach {
            assertTrue(it.x in 0f..1f)
            assertTrue("y=${it.y}", it.y in 0f..1f)
        }
    }
}
