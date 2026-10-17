package com.devcris80.prototipo.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devcris80.prototipo.ui.theme.BovinaCardPadding
import com.devcris80.prototipo.ui.theme.BovinaForest
import com.devcris80.prototipo.ui.theme.Prototipo1Theme

/** Tarjeta de contenido: superficie blanca de 24dp con borde fino y elevación suave. */
@Composable
fun BovinaCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLowest,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colores = CardDefaults.cardColors(containerColor = containerColor)
    val borde = BorderStroke(1.dp, BovinaForest.copy(alpha = 0.05f))
    val elevacion = CardDefaults.cardElevation(defaultElevation = 2.dp)
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = MaterialTheme.shapes.medium,
            colors = colores,
            border = borde,
            elevation = elevacion,
        ) {
            Column(modifier = Modifier.padding(BovinaCardPadding), content = content)
        }
    } else {
        Card(
            modifier = modifier,
            shape = MaterialTheme.shapes.medium,
            colors = colores,
            border = borde,
            elevation = elevacion,
        ) {
            Column(modifier = Modifier.padding(BovinaCardPadding), content = content)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TarjetaPreview() {
    Prototipo1Theme {
        BovinaCard(modifier = Modifier.padding(16.dp)) {
            PillBadge(texto = "LECHE")
            Text(text = "Nombre del animal", style = MaterialTheme.typography.headlineSmall)
            Text(text = "Holstein · Adulta", style = MaterialTheme.typography.bodyMedium)
        }
    }
}
