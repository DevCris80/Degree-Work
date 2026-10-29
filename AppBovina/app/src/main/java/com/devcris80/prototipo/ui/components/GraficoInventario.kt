package com.devcris80.prototipo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.model.Sexo
import com.devcris80.prototipo.ui.theme.Prototipo1Theme
import com.devcris80.prototipo.ui.util.BarraEtapa
import com.devcris80.prototipo.ui.util.BarraProposito
import com.devcris80.prototipo.ui.util.aEtiqueta

private val ALTO_BARRA_PROPOSITO = 12.dp
private val ALTO_BARRA_ETAPA = 8.dp
private val ALTO_BARRA_SEXO = 16.dp
private const val ALPHA_ETAPA = 0.55f

/** Hembras y machos en una sola barra partida, cada parte con el ancho de su cantidad. */
@Composable
fun GraficoInventarioPorSexo(
    hembras: Int,
    machos: Int,
    modifier: Modifier = Modifier,
) {
    if (hembras + machos == 0) return
    val resumen = "${Sexo.HEMBRA.aEtiqueta()}: $hembras, ${Sexo.MACHO.aEtiqueta()}: $machos"
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = resumen },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(ALTO_BARRA_SEXO)
                .clip(RoundedCornerShape(50)),
        ) {
            if (hembras > 0) {
                Box(modifier = Modifier.weight(hembras.toFloat()).height(ALTO_BARRA_SEXO).background(MaterialTheme.colorScheme.primary))
            }
            if (machos > 0) {
                Box(modifier = Modifier.weight(machos.toFloat()).height(ALTO_BARRA_SEXO).background(MaterialTheme.colorScheme.tertiary))
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            LeyendaSexo(Sexo.HEMBRA.aEtiqueta(), hembras, MaterialTheme.colorScheme.primary)
            LeyendaSexo(Sexo.MACHO.aEtiqueta(), machos, MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable
private fun LeyendaSexo(etiqueta: String, cantidad: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(color))
        Text(
            text = "$etiqueta: $cantidad",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Inventario por propósito y etapa en barras horizontales: la barra de cada propósito es su parte del hato y, debajo,
 * la de cada etapa es su parte del propósito.
 */
@Composable
fun GraficoInventarioPorProposito(
    barras: List<BarraProposito>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        barras.forEach { barra ->
            val resumen = "${barra.proposito.aEtiqueta()}: ${barra.cantidad}. " +
                barra.etapas.joinToString(", ") { "${it.etapa.aEtiqueta()} ${it.cantidad}" }
            Column(
                modifier = Modifier.clearAndSetSemantics { contentDescription = resumen },
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FilaBarra(
                    etiqueta = barra.proposito.aEtiqueta(),
                    cantidad = barra.cantidad,
                    proporcion = barra.proporcion,
                    alto = ALTO_BARRA_PROPOSITO,
                    color = MaterialTheme.colorScheme.primary,
                    destacada = true,
                )
                barra.etapas.forEach { etapa -> FilaEtapa(etapa) }
            }
        }
    }
}

@Composable
private fun FilaEtapa(etapa: BarraEtapa) {
    FilaBarra(
        etiqueta = etapa.etapa.aEtiqueta(),
        cantidad = etapa.cantidad,
        proporcion = etapa.proporcion,
        alto = ALTO_BARRA_ETAPA,
        color = MaterialTheme.colorScheme.primary.copy(alpha = ALPHA_ETAPA),
        destacada = false,
        modifier = Modifier.padding(start = 12.dp),
    )
}

@Composable
private fun FilaBarra(
    etiqueta: String,
    cantidad: Int,
    proporcion: Float,
    alto: Dp,
    color: Color,
    destacada: Boolean,
    modifier: Modifier = Modifier,
) {
    val estilo = if (destacada) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = etiqueta, style = estilo)
            Text(text = cantidad.toString(), style = estilo)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(alto)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(proporcion.coerceIn(0f, 1f))
                    .height(alto)
                    .clip(RoundedCornerShape(50))
                    .background(color),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GraficoInventarioPreview() {
    Prototipo1Theme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            GraficoInventarioPorSexo(hembras = 14, machos = 6)
            GraficoInventarioPorProposito(
                barras = listOf(
                    BarraProposito(
                        Proposito.LECHE, 12, 0.6f,
                        listOf(BarraEtapa(Etapa.VACA, 9, 0.75f), BarraEtapa(Etapa.TERNERA, 3, 0.25f)),
                    ),
                    BarraProposito(
                        Proposito.CARNE, 8, 0.4f,
                        listOf(BarraEtapa(Etapa.TORO, 2, 0.25f), BarraEtapa(Etapa.NOVILLO, 6, 0.75f)),
                    ),
                ),
            )
        }
    }
}
