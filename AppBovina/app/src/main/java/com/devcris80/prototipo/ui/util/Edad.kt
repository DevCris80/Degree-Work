package com.devcris80.prototipo.ui.util

import java.util.Calendar

fun formatearEdad(fechaNacimiento: Long, hoy: Long = System.currentTimeMillis()): String {
    val nacimiento = Calendar.getInstance().apply { timeInMillis = fechaNacimiento }
    val ahora = Calendar.getInstance().apply { timeInMillis = hoy }
    var meses = (ahora.get(Calendar.YEAR) - nacimiento.get(Calendar.YEAR)) * 12 +
        (ahora.get(Calendar.MONTH) - nacimiento.get(Calendar.MONTH))
    if (ahora.get(Calendar.DAY_OF_MONTH) < nacimiento.get(Calendar.DAY_OF_MONTH)) meses--
    meses = meses.coerceAtLeast(0)
    return "${meses / 12} años, ${meses % 12} meses"
}
