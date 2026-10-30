package com.devcris80.prototipo.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.devcris80.prototipo.domain.model.Rol
import com.devcris80.prototipo.ui.theme.BovinaScreenGutter
import com.devcris80.prototipo.ui.util.aEtiqueta

/**
 * Menú de Configuración de la pantalla de Animales (#139): los datos de la cuenta y la finca, las acciones del
 * Ganadero y Cerrar sesión. Una acción en `null` no se muestra, porque el rol no la permite.
 */
@Composable
fun PanelConfiguracion(
    visible: Boolean,
    nombreUsuario: String,
    rol: Rol?,
    nombreFinca: String,
    onInvitarClick: (() -> Unit)?,
    onRegistrarUbicacionClick: (() -> Unit)?,
    onCerrarSesionClick: () -> Unit,
    onCerrarPanel: () -> Unit,
) {
    // Elegir una opción cierra el panel y luego hace lo suyo.
    fun alElegir(accion: () -> Unit): () -> Unit = {
        onCerrarPanel()
        accion()
    }
    PanelLateralDerecho(visible = visible, onCerrar = onCerrarPanel) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = BovinaScreenGutter, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Configuración",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onCerrarPanel) {
                Icon(Icons.Filled.Close, contentDescription = "Cerrar el menú")
            }
        }
        DatosDeLaCuenta(nombreUsuario = nombreUsuario, rol = rol, nombreFinca = nombreFinca)
        HorizontalDivider()
        if (onInvitarClick != null) {
            OpcionDelPanel(
                icono = Icons.Filled.PersonAdd,
                texto = "Invitar a un Operario",
                onClick = alElegir(onInvitarClick),
            )
        }
        if (onRegistrarUbicacionClick != null) {
            OpcionDelPanel(
                icono = Icons.Filled.LocationOn,
                texto = "Registrar ubicación de la finca",
                onClick = alElegir(onRegistrarUbicacionClick),
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        HorizontalDivider()
        OpcionDelPanel(
            icono = Icons.AutoMirrored.Filled.Logout,
            texto = "Cerrar sesión",
            color = MaterialTheme.colorScheme.error,
            onClick = alElegir(onCerrarSesionClick),
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun DatosDeLaCuenta(nombreUsuario: String, rol: Rol?, nombreFinca: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = BovinaScreenGutter, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(text = nombreUsuario, style = MaterialTheme.typography.titleMedium)
        rol?.let {
            Text(
                text = it.aEtiqueta(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = nombreFinca,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun OpcionDelPanel(
    icono: ImageVector,
    texto: String,
    onClick: () -> Unit,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = BovinaScreenGutter, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(imageVector = icono, contentDescription = null, tint = color)
        Text(text = texto, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}
