package com.devcris80.prototipo.ui.util

import com.devcris80.prototipo.domain.usecase.AnioMes
import com.devcris80.prototipo.domain.usecase.EventosDelMes

/** Una barra del gráfico de eventos por mes. */
data class BarraMes(
    val mes: AnioMes,
    val total: Int,
    /** De 0 a 1, relativa al mes con más eventos de las barras que se muestran. */
    val altura: Float,
)

const val MESES_EN_EL_GRAFICO = 12

/**
 * Las barras del gráfico de eventos por mes, del mes más antiguo al más reciente. Un mes sin eventos entre dos con
 * eventos aparece en cero, para que el hueco se vea. Si hay más de [maximoMeses], quedan los más recientes.
 */
fun barrasPorMes(eventosPorMes: List<EventosDelMes>, maximoMeses: Int = MESES_EN_EL_GRAFICO): List<BarraMes> {
    if (eventosPorMes.isEmpty()) return emptyList()
    val totalPorMes = eventosPorMes.associate { it.mes to it.porTipo.values.sum() }
    val primero = totalPorMes.keys.min()
    val ultimo = totalPorMes.keys.max()

    val meses = generateSequence(primero) { it.siguiente() }.takeWhile { it <= ultimo }.toList().takeLast(maximoMeses)
    val masEventos = meses.maxOf { totalPorMes[it] ?: 0 }
    return meses.map { mes ->
        val total = totalPorMes[mes] ?: 0
        BarraMes(mes = mes, total = total, altura = if (masEventos == 0) 0f else total.toFloat() / masEventos)
    }
}

private fun AnioMes.siguiente() = if (mes == 12) AnioMes(anio + 1, 1) else AnioMes(anio, mes + 1)
