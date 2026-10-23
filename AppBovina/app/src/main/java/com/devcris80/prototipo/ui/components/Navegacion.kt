package com.devcris80.prototipo.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devcris80.prototipo.R
import com.devcris80.prototipo.ui.theme.Prototipo1Theme

/** Barra superior propia: logo o flecha de regreso, título y acciones opcionales. */
@Composable
fun BovinaTopBar(
    titulo: String,
    modifier: Modifier = Modifier,
    onVolver: (() -> Unit)? = null,
    mostrarLogo: Boolean = false,
    acciones: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onVolver != null) {
            IconButton(onClick = onVolver) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
            }
        } else if (mostrarLogo) {
            Image(
                painter = painterResource(R.drawable.logo_miganado),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(start = 8.dp, end = 4.dp)
                    .size(40.dp)
                    .clip(CircleShape),
            )
        } else {
            Box(modifier = Modifier.size(8.dp))
        }
        Text(
            text = titulo,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        )
        acciones()
    }
}

/** Destino de la barra inferior. */
data class DestinoBarra(
    val etiqueta: String,
    val icono: androidx.compose.ui.graphics.vector.ImageVector,
    val seleccionado: Boolean,
    val onClick: () -> Unit,
)

/** Dock inferior blanco y flotante; la pestaña activa va en píldora verde con ícono y etiqueta. */
@Composable
fun BovinaBottomBar(
    destinos: List<DestinoBarra>,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            destinos.forEach { destino ->
                ItemBarra(destino = destino, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ItemBarra(destino: DestinoBarra, modifier: Modifier = Modifier) {
    val colorContenido = if (destino.seleccionado) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clip(CircleShape)
            .then(
                if (destino.seleccionado) {
                    Modifier.background(MaterialTheme.colorScheme.primary)
                } else {
                    Modifier
                },
            )
            .selectable(selected = destino.seleccionado, role = Role.Tab, onClick = destino.onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = destino.icono,
                contentDescription = null,
                tint = colorContenido,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = destino.etiqueta,
                style = MaterialTheme.typography.labelMedium,
                color = colorContenido,
                maxLines = 1,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NavegacionPreview() {
    Prototipo1Theme {
        Column {
            BovinaTopBar(titulo = "Animales", mostrarLogo = true)
            BovinaTopBar(titulo = "Registrar Animal", onVolver = {})
            BovinaBottomBar(
                destinos = listOf(
                    DestinoBarra("Animales", Icons.Filled.Pets, true) {},
                    DestinoBarra("Escanear", Icons.Filled.Nfc, false) {},
                    DestinoBarra("Eventos", Icons.Filled.EventAvailable, false) {},
                    DestinoBarra("Pesos", Icons.Filled.MonitorWeight, false) {},
                ),
            )
        }
    }
}
