package com.devcris80.prototipo.ui.util

import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.usecase.GrupoInventario

/** Una etapa dentro de un propósito del gráfico de inventario. */
data class BarraEtapa(
    val etapa: Etapa,
    val cantidad: Int,
    /** De 0 a 1, parte del propósito al que pertenece. */
    val proporcion: Float,
)

/** Un propósito del gráfico de inventario, con sus etapas. */
data class BarraProposito(
    val proposito: Proposito,
    val cantidad: Int,
    /** De 0 a 1, parte del total de Animales activos. */
    val proporcion: Float,
    val etapas: List<BarraEtapa>,
)

/**
 * Las barras del gráfico de inventario: un bloque por propósito con su parte del hato, y dentro sus etapas con su
 * parte del propósito. Los grupos sin Animales no aparecen.
 */
fun barrasDeInventario(inventario: List<GrupoInventario>): List<BarraProposito> {
    val activos = inventario.filter { it.cantidad > 0 }
    val totalHato = activos.sumOf { it.cantidad }
    return activos.groupBy { it.proposito }.map { (proposito, grupos) ->
        val totalProposito = grupos.sumOf { it.cantidad }
        BarraProposito(
            proposito = proposito,
            cantidad = totalProposito,
            proporcion = totalProposito.toFloat() / totalHato,
            etapas = grupos.map { BarraEtapa(it.etapa, it.cantidad, it.cantidad.toFloat() / totalProposito) },
        )
    }
}
