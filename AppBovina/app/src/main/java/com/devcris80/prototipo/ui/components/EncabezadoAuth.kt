package com.devcris80.prototipo.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devcris80.prototipo.R
import com.devcris80.prototipo.ui.theme.BovinaForest
import com.devcris80.prototipo.ui.theme.BovinaGradientEnd
import com.devcris80.prototipo.ui.theme.BovinaGradientStart
import com.devcris80.prototipo.ui.theme.Prototipo1Theme

/**
 * Cabecera de Registro y Login: logo en círculo, etiqueta, título y subtítulo. `oscuro` usa el
 * degradado botánico con texto blanco (Login); si no, una tarjeta clara (Registro).
 */
@Composable
fun EncabezadoAuth(
    etiqueta: String,
    titulo: String,
    subtitulo: String,
    modifier: Modifier = Modifier,
    oscuro: Boolean = false,
) {
    val forma = if (oscuro) {
        RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp)
    } else {
        RoundedCornerShape(32.dp)
    }
    val fondo = if (oscuro) {
        Modifier.background(Brush.linearGradient(listOf(BovinaGradientStart, BovinaGradientEnd)), forma)
    } else {
        Modifier.background(MaterialTheme.colorScheme.surfaceContainer, forma)
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(fondo)
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.logo_miganado),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(88.dp)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape),
        )
        PillBadge(
            texto = etiqueta,
            containerColor = if (oscuro) {
                MaterialTheme.colorScheme.surfaceContainerLowest
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
            contentColor = if (oscuro) MaterialTheme.colorScheme.primary else BovinaForest,
        )
        Text(
            text = titulo,
            style = MaterialTheme.typography.displayMedium,
            color = if (oscuro) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = subtitulo,
            style = MaterialTheme.typography.bodyLarge,
            color = if (oscuro) {
                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EncabezadoAuthPreview() {
    Prototipo1Theme {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            EncabezadoAuth(
                etiqueta = "MIGANADO",
                titulo = "Iniciar sesión",
                subtitulo = "Ingrese con su cuenta para continuar.",
                oscuro = true,
            )
            EncabezadoAuth(
                etiqueta = "MIGANADO",
                titulo = "Crear cuenta",
                subtitulo = "Regístrese para empezar.",
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}
