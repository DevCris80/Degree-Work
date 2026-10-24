package com.devcris80.prototipo.domain.usecase

import java.util.Calendar

/** Un mes del calendario del celular; [mes] va de 1 a 12. */
data class AnioMes(val anio: Int, val mes: Int) : Comparable<AnioMes> {
    /** El primer instante del mes, para mostrarlo con un formato de fecha. */
    fun inicio(): Long = Calendar.getInstance().apply {
        clear()
        set(anio, mes - 1, 1)
    }.timeInMillis

    override fun compareTo(other: AnioMes): Int = compareValuesBy(this, other, { it.anio }, { it.mes })

    companion object {
        fun de(fecha: Long): AnioMes {
            val calendario = Calendar.getInstance().apply { timeInMillis = fecha }
            return AnioMes(calendario.get(Calendar.YEAR), calendario.get(Calendar.MONTH) + 1)
        }
    }
}
