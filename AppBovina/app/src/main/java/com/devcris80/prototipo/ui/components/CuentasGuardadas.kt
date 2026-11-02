package com.devcris80.prototipo.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devcris80.prototipo.domain.model.CuentaGuardada
import com.devcris80.prototipo.domain.model.Rol
import com.devcris80.prototipo.ui.screens.QuitarCuentaUiState

/**
 * Las cuentas que este celular guarda con la sesión cerrada (ADR-0006). Tocar una rellena el correo; el bote de
 * basura pide quitar sus datos del celular.
 */
@Composable
fun CuentasGuardadasLista(
    cuentas: List<CuentaGuardada>,
    onCuentaClick: (CuentaGuardada) -> Unit,
    onQuitarClick: (CuentaGuardada) -> Unit,
    modifier: Modifier = Modifier,
) {
    BovinaCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Cuentas en este celular",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            cuentas.forEachIndexed { indice, cuenta ->
                if (indice > 0) HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCuentaClick(cuenta) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(cuenta.nombreFinca, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "${cuenta.nombreUsuario} · ${cuenta.rol.etiqueta()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        cuenta.textoPendientes()?.let { pendientes ->
                            Text(
                                text = pendientes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        }
                    }
                    IconButton(onClick = { onQuitarClick(cuenta) }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Quitar ${cuenta.nombreFinca} de este celular",
                        )
                    }
                }
            }
        }
    }
}

/** Quitar una cuenta no se puede deshacer, y con algo por subir no se ofrece (RN-27). */
@Composable
fun QuitarCuentaDialogo(estado: QuitarCuentaUiState, onConfirmar: () -> Unit, onCancelar: () -> Unit) {
    val cuenta = estado.cuenta
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(if (cuenta.tienePendientes) "No se puede quitar todavía" else "¿Quitar de este celular?") },
        text = {
            Text(
                if (cuenta.tienePendientes) {
                    "${cuenta.nombreFinca} tiene ${cuenta.textoPendientes()?.lowercase()}. " +
                        "Inicia sesión con esa cuenta y sincroniza primero; si no, se perderían."
                } else {
                    "Se borran de este celular los datos de ${cuenta.nombreFinca} de ${cuenta.nombreUsuario}. " +
                        "No se pueden recuperar aquí; en el servidor siguen, y volver a entrar con señal los descarga."
                },
            )
        },
        confirmButton = {
            if (cuenta.tienePendientes) {
                TextButton(onClick = onCancelar) { Text("Entendido") }
            } else {
                TextButton(onClick = onConfirmar, enabled = estado.puedeQuitar) {
                    Text(if (estado.quitando) "Quitando…" else "Quitar")
                }
            }
        },
        dismissButton = {
            if (!cuenta.tienePendientes) TextButton(onClick = onCancelar) { Text("Cancelar") }
        },
    )
}

private fun Rol.etiqueta() = when (this) {
    Rol.GANADERO -> "Ganadero"
    Rol.OPERARIO -> "Operario"
}

/** Qué le queda por subir a la cuenta, o nulo si nada. */
private fun CuentaGuardada.textoPendientes(): String? {
    val partes = listOfNotNull(
        cambiosSinSubir.takeIf { it > 0 }?.let { if (it == 1) "1 cambio sin subir" else "$it cambios sin subir" },
        fotosSinSubir.takeIf { it > 0 }?.let { if (it == 1) "1 foto sin subir" else "$it fotos sin subir" },
        rechazosAbiertos.takeIf { it > 0 }?.let { if (it == 1) "1 cambio rechazado" else "$it cambios rechazados" },
    )
    return partes.joinToString(", ").ifEmpty { null }
}
