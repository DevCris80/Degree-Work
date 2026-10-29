package com.devcris80.prototipo.ui.util

import com.devcris80.prototipo.domain.model.Registro

/** Un pesaje en el área del gráfico: `x` e `y` van de 0 a 1, con `y` creciente hacia arriba. */
data class PuntoGrafico(val x: Float, val y: Float)

data class DatosGrafico(
    /** De izquierda a derecha, del pesaje más antiguo al más reciente. */
    val puntos: List<PuntoGrafico>,
    val pesoMinimo: Float,
    val pesoMaximo: Float,
    val primeraFecha: Long,
    val ultimaFecha: Long,
)

/** Espacio sobre el peso máximo y bajo el mínimo, para que ningún punto quede pegado al borde. */
private const val MARGEN_RELATIVO = 0.1f
private const val MARGEN_MINIMO_KG = 1f

/**
 * Los puntos del gráfico de evolución de peso de un Animal, o nulo si no hay al menos dos Registros
 * para unir. Los Registros dados de baja no cuentan. Llegan en cualquier orden.
 */
fun datosDelGrafico(registros: List<Registro>): DatosGrafico? {
    val activos = registros.filter { it.fechaBaja == null }.sortedBy { it.timestamp }
    if (activos.size < 2) return null

    val pesoMinimo = activos.minOf { it.peso }
    val pesoMaximo = activos.maxOf { it.peso }
    val primeraFecha = activos.first().timestamp
    val ultimaFecha = activos.last().timestamp
    val duracion = ultimaFecha - primeraFecha

    // Con todos los pesos iguales el margen mínimo evita dividir entre cero y deja los puntos al medio.
    val margen = maxOf((pesoMaximo - pesoMinimo) * MARGEN_RELATIVO, MARGEN_MINIMO_KG)
    val bajo = pesoMinimo - margen
    val alto = pesoMaximo + margen

    return DatosGrafico(
        puntos = activos.map { registro ->
            PuntoGrafico(
                x = if (duracion == 0L) 0.5f else (registro.timestamp - primeraFecha).toFloat() / duracion,
                y = (registro.peso - bajo) / (alto - bajo),
            )
        },
        pesoMinimo = pesoMinimo,
        pesoMaximo = pesoMaximo,
        primeraFecha = primeraFecha,
        ultimaFecha = ultimaFecha,
    )
}
