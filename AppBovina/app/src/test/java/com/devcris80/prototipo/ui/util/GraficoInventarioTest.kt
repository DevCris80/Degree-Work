package com.devcris80.prototipo.ui.util

import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.usecase.GrupoInventario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GraficoInventarioTest {
    private val propositos = Proposito.entries
    private val etapas = Etapa.entries

    private fun grupo(proposito: Proposito, etapa: Etapa, cantidad: Int) = GrupoInventario(proposito, etapa, cantidad)

    @Test
    fun sinAnimalesNoHayBarras() {
        assertTrue(barrasDeInventario(emptyList()).isEmpty())
    }

    @Test
    fun losGruposEnCeroNoAparecen() {
        val barras = barrasDeInventario(listOf(grupo(propositos[0], etapas[0], 0)))

        assertTrue(barras.isEmpty())
    }

    @Test
    fun unPropositoSumaSusEtapasYSuParteDelHato() {
        val barras = barrasDeInventario(
            listOf(
                grupo(propositos[0], etapas[0], 3),
                grupo(propositos[0], etapas[1], 1),
                grupo(propositos[1], etapas[0], 4),
            ),
        )

        assertEquals(2, barras.size)
        assertEquals(4, barras[0].cantidad)
        assertEquals(0.5f, barras[0].proporcion, 0.0001f)
        assertEquals(0.5f, barras[1].proporcion, 0.0001f)
    }

    @Test
    fun lasEtapasSonParteDeSuPropositoNoDelHato() {
        val barras = barrasDeInventario(
            listOf(
                grupo(propositos[0], etapas[0], 3),
                grupo(propositos[0], etapas[1], 1),
                grupo(propositos[1], etapas[0], 4),
            ),
        )

        assertEquals(listOf(0.75f, 0.25f), barras[0].etapas.map { it.proporcion })
    }

    @Test
    fun lasProporcionesDelHatoSumanUno() {
        val barras = barrasDeInventario(
            listOf(
                grupo(propositos[0], etapas[0], 1),
                grupo(propositos[1], etapas[0], 2),
                grupo(propositos[1], etapas[1], 4),
            ),
        )

        assertEquals(1f, barras.sumOf { it.proporcion.toDouble() }.toFloat(), 0.0001f)
    }
}
