package com.devcris80.prototipo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devcris80.prototipo.ui.theme.BovinaMinFieldHeight
import com.devcris80.prototipo.ui.theme.Prototipo1Theme

/** Campo en píldora que se toca para abrir un selector (fecha, hora, lista); muestra valor o marcador. */
@Composable
fun CampoSelector(
    valor: String?,
    marcador: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector = Icons.Filled.KeyboardArrowDown,
    fondo: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceContainer,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = BovinaMinFieldHeight)
            .clip(CircleShape)
            .background(fondo)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = valor ?: marcador,
            style = MaterialTheme.typography.bodyLarge,
            color = if (valor != null) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = trailingIcon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CampoSelectorPreview() {
    Prototipo1Theme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CampoSelector(
                valor = "28/05/2026",
                marcador = "Fecha",
                onClick = {},
                leadingIcon = Icons.Filled.CalendarMonth,
            )
            CampoSelector(
                valor = null,
                marcador = "Seleccionar animal",
                onClick = {},
                leadingIcon = Icons.Filled.Pets,
            )
        }
    }
}
