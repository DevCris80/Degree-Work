package com.devcris80.prototipo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devcris80.prototipo.ui.theme.BovinaGradientEnd
import com.devcris80.prototipo.ui.theme.BovinaGradientStart
import com.devcris80.prototipo.ui.theme.BovinaMinTouchTarget
import com.devcris80.prototipo.ui.theme.Prototipo1Theme

/** Botón primario: píldora de 52dp con degradado botánico. */
@Composable
fun BovinaPrimaryButton(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(BovinaMinTouchTarget),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
        ),
        contentPadding = PaddingValues(0.dp),
    ) {
        val fondo = if (enabled) {
            Modifier.background(Brush.horizontalGradient(listOf(BovinaGradientStart, BovinaGradientEnd)))
        } else {
            Modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest)
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .then(fondo)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            ContenidoBoton(
                texto = texto,
                color = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline,
                icon = icon,
                trailingIcon = trailingIcon,
            )
        }
    }
}

/** Botón secundario: píldora de 52dp con fondo suave y texto neutro. */
@Composable
fun BovinaSecondaryButton(
    texto: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(BovinaMinTouchTarget),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            disabledContentColor = MaterialTheme.colorScheme.outline,
        ),
        contentPadding = PaddingValues(horizontal = 24.dp),
    ) {
        ContenidoBoton(
            texto = texto,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
            icon = icon,
            trailingIcon = null,
        )
    }
}

@Composable
private fun ContenidoBoton(
    texto: String,
    color: Color,
    icon: ImageVector?,
    trailingIcon: ImageVector?,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
        Text(text = texto, style = MaterialTheme.typography.labelLarge, color = color)
        if (trailingIcon != null) {
            Icon(imageVector = trailingIcon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BotonesPreview() {
    Prototipo1Theme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BovinaPrimaryButton(
                texto = "Iniciar sesión",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
            )
            BovinaPrimaryButton(
                texto = "Guardar animal",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
            )
            BovinaSecondaryButton(
                texto = "Crear cuenta nueva",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Filled.Add,
            )
        }
    }
}
