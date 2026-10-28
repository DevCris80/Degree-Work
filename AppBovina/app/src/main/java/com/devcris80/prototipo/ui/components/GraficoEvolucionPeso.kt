package com.devcris80.prototipo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devcris80.prototipo.domain.model.Registro
import com.devcris80.prototipo.ui.theme.Prototipo1Theme
import com.devcris80.prototipo.ui.util.DatosGrafico
import com.devcris80.prototipo.ui.util.datosDelGrafico
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val LINEAS_GUIA = 3
private val ALTO_GRAFICO = 160.dp

/**
 * Evolución del peso de un Animal: peso contra fecha, a partir de sus Registros. Con menos de dos Registros
 * activos no hay línea que dibujar y muestra un aviso en su lugar.
 */
@Composable
fun GraficoEvolucionPeso(
    registros: List<Registro>,
    modifier: Modifier = Modifier,
) {
    val datos = remember(registros) { datosDelGrafico(registros) }

    BovinaCard(modifier = modifier.fillMaxWidth()) {
        Text(text = "Evolución de peso", style = MaterialTheme.typography.titleMedium)
        if (datos == null) {
            Text(
                text = "Se necesitan al menos dos pesajes para ver la evolución.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        } else {
            GraficoPesoConEjes(datos = datos, modifier = Modifier.padding(top = 12.dp))
        }
    }
}

/** La línea de peso con sus ejes, sin tarjeta alrededor, para usarla dentro de otra tarjeta. */
@Composable
fun GraficoPesoConEjes(
    datos: DatosGrafico,
    modifier: Modifier = Modifier,
) {
    val formatoFecha = remember { SimpleDateFormat("dd/MM/yy", Locale.getDefault()) }
    val pesoMaximo = "${formatoPeso(datos.pesoMaximo)} kg"
    val pesoMinimo = "${formatoPeso(datos.pesoMinimo)} kg"
    val primeraFecha = formatoFecha.format(Date(datos.primeraFecha))
    val ultimaFecha = formatoFecha.format(Date(datos.ultimaFecha))
    val resumen = "Peso de $pesoMinimo a $pesoMaximo entre el $primeraFecha y el $ultimaFecha"
    Column(
        // Un solo resumen para el lector de pantalla: sin esto leería además cada etiqueta de los ejes.
        modifier = modifier.clearAndSetSemantics { contentDescription = resumen },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(
                modifier = Modifier.height(ALTO_GRAFICO),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                EtiquetaEje(pesoMaximo)
                EtiquetaEje(pesoMinimo)
            }
            LineaDeEvolucion(datos = datos, modifier = Modifier.weight(1f).height(ALTO_GRAFICO))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            EtiquetaEje(primeraFecha)
            EtiquetaEje(ultimaFecha)
        }
    }
}

@Composable
private fun EtiquetaEje(texto: String) {
    Text(text = texto, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun LineaDeEvolucion(datos: DatosGrafico, modifier: Modifier = Modifier) {
    val colorLinea = MaterialTheme.colorScheme.primary
    val colorGuia = MaterialTheme.colorScheme.outlineVariant
    val colorCentro = MaterialTheme.colorScheme.surfaceContainerLowest

    Canvas(modifier = modifier) {
        val radio = 5.dp.toPx()
        // Un margen igual al radio deja enteros los puntos de los extremos.
        val ancho = size.width - 2 * radio
        val alto = size.height - 2 * radio
        fun punto(x: Float, y: Float) = Offset(radio + ancho * x, radio + alto * (1f - y))

        repeat(LINEAS_GUIA) { i ->
            val y = radio + alto * i / (LINEAS_GUIA - 1)
            drawLine(colorGuia, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
        }

        val trazo = Path()
        datos.puntos.forEachIndexed { i, p ->
            val destino = punto(p.x, p.y)
            if (i == 0) trazo.moveTo(destino.x, destino.y) else trazo.lineTo(destino.x, destino.y)
        }
        drawPath(
            path = trazo,
            color = colorLinea,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        datos.puntos.forEach { p ->
            val centro = punto(p.x, p.y)
            drawCircle(colorLinea, radius = radio, center = centro)
            drawCircle(colorCentro, radius = radio / 2f, center = centro)
        }
    }
}

private fun formatoPeso(peso: Float): String = "%.1f".format(Locale.getDefault(), peso)

private fun registroDePrueba(id: String, peso: Float, dia: Int) = Registro(
    idRegistro = id,
    idAnimal = "animal-1",
    idUsuario = "usuario",
    peso = peso,
    timestamp = 1_700_000_000_000 + dia * 86_400_000L,
)

@Preview(showBackground = true)
@Composable
private fun GraficoEvolucionPesoPreview() {
    Prototipo1Theme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GraficoEvolucionPeso(
                registros = listOf(
                    registroDePrueba("r4", 372f, 90),
                    registroDePrueba("r3", 351.5f, 60),
                    registroDePrueba("r2", 338f, 30),
                    registroDePrueba("r1", 320f, 0),
                ),
            )
            GraficoEvolucionPeso(registros = listOf(registroDePrueba("r1", 320f, 0)))
        }
    }
}
