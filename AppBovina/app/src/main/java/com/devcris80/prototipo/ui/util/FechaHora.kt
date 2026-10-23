package com.devcris80.prototipo.ui.util

import java.util.Calendar
import java.util.TimeZone

// El DatePicker de Material trabaja con la medianoche UTC de cada día; la app guarda fechas en la
// hora local del celular. Estas funciones pasan de una a otra sin correr el día.

/** La medianoche UTC del día local de [fecha]: lo que espera el DatePicker. */
fun diaEnUtc(fecha: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = fecha }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

/** [fecha] movida al día que eligió el DatePicker ([diaUtc]), con la misma hora local. */
fun conDia(fecha: Long, diaUtc: Long): Long {
    val dia = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = diaUtc }
    return Calendar.getInstance().apply {
        timeInMillis = fecha
        set(dia.get(Calendar.YEAR), dia.get(Calendar.MONTH), dia.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

/** [fecha] con otra hora y minuto locales, en el segundo cero. */
fun conHora(fecha: Long, hora: Int, minuto: Int): Long = Calendar.getInstance().apply {
    timeInMillis = fecha
    set(Calendar.HOUR_OF_DAY, hora)
    set(Calendar.MINUTE, minuto)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis
