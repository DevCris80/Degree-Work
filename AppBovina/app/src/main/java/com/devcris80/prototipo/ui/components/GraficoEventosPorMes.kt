package com.devcris80.prototipo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devcris80.prototipo.domain.usecase.AnioMes
import com.devcris80.prototipo.ui.theme.Prototipo1Theme
import com.devcris80.prototipo.ui.util.BarraMes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ALTO_BARRAS = 120.dp
private val ANCHO_COLUMNA = 48.dp
private val ANCHO_BARRA = 28.dp
private val ALTO_LINEA_BASE = 2.dp

/**
 * Eventos por mes en barras, del mes más antiguo (izquierda) al más reciente (derecha). Tocar una barra elige el mes
 * para ver su detalle. Con muchos meses se desliza, y empieza mostrando los más recientes.
 */
@Composable
fun GraficoEventosPorMes(
    barras: List<BarraMes>,
    mesElegido: AnioMes,
    onMesElegido: (AnioMes) -> Unit,
    modifier: Modifier = Modifier,
) {
    val formatoMes = remember { SimpleDateFormat("MMM yy", Locale.getDefault()) }
    val desplazamiento = rememberScrollState()
    LaunchedEffect(desplazamiento.maxValue) { desplazamiento.scrollTo(desplazamiento.maxValue) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(desplazamiento),
        verticalAlignment = Alignment.Bottom,
    ) {
        barras.forEach { barra ->
            val elegida = barra.mes == mesElegido
            val etiqueta = formatoMes.format(Date(barra.mes.inicio())).replaceFirstChar(Char::uppercase)
            Column(
                modifier = Modifier
                    .width(ANCHO_COLUMNA)
                    .clip(MaterialTheme.shapes.small)
                    .clickable(role = Role.Button) { onMesElegido(barra.mes) }
                    .clearAndSetSemantics {
                        contentDescription = "$etiqueta: ${eventos(barra.total)}"
                        role = Role.Button
                        selected = elegida
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = barra.total.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(modifier = Modifier.height(ALTO_BARRAS), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        modifier = Modifier
                            .width(ANCHO_BARRA)
                            .height(maxOf(ALTO_BARRAS * barra.altura, ALTO_LINEA_BASE))
                            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                            .background(
                                if (elegida) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                },
                            ),
                    )
                }
                Text(
                    text = etiqueta,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (elegida) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                )
            }
        }
    }
}

private fun eventos(cantidad: Int) = if (cantidad == 1) "1 evento" else "$cantidad eventos"

@Preview(showBackground = true)
@Composable
private fun GraficoEventosPorMesPreview() {
    Prototipo1Theme {
        GraficoEventosPorMes(
            barras = listOf(
                BarraMes(AnioMes(2026, 3), total = 2, altura = 0.4f),
                BarraMes(AnioMes(2026, 4), total = 0, altura = 0f),
                BarraMes(AnioMes(2026, 5), total = 5, altura = 1f),
                BarraMes(AnioMes(2026, 6), total = 3, altura = 0.6f),
            ),
            mesElegido = AnioMes(2026, 5),
            onMesElegido = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
