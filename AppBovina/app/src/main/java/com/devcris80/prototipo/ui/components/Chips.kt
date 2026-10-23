package com.devcris80.prototipo.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devcris80.prototipo.ui.theme.BovinaForest
import com.devcris80.prototipo.ui.theme.Prototipo1Theme

/** Chip de filtro en píldora: blanco con borde fino al estar suelto, verde bosque al seleccionarse. */
@Composable
fun BovinaFilterChip(
    texto: String,
    seleccionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    val contenido = if (seleccionado) MaterialTheme.colorScheme.onPrimary else BovinaForest
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        color = if (seleccionado) BovinaForest else MaterialTheme.colorScheme.surfaceContainerLowest,
        border = if (seleccionado) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (seleccionado) MaterialTheme.colorScheme.inversePrimary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(text = texto, style = MaterialTheme.typography.labelLarge, color = contenido)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChipsPreview() {
    Prototipo1Theme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BovinaFilterChip(texto = "Todos (32)", seleccionado = true, onClick = {}, icon = Icons.Filled.Pets)
            BovinaFilterChip(texto = "Leche (14)", seleccionado = false, onClick = {})
        }
    }
}
