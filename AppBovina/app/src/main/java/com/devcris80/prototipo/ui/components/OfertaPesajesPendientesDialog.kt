package com.devcris80.prototipo.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.devcris80.prototipo.domain.model.PesajePendiente
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Los Pesajes pendientes de un codigo que se ofrecen asignar al Animal que recibe su Chapeta. */
data class OfertaPesajesPendientes(
    val idAnimal: String,
    val nombreAnimal: String,
    val codigo: String,
    val pesajes: List<PesajePendiente>,
)

/**
 * Ofrece asignar a un Animal los Pesajes pendientes del codigo de su Chapeta, con la hora en que se midió cada uno.
 * Lo usan Registrar animal y Asociar chapeta.
 */
@Composable
fun OfertaPesajesPendientesDialog(
    oferta: OfertaPesajesPendientes,
    onRespondida: (asignar: Boolean) -> Unit,
) {
    val formatoFecha = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    AlertDialog(
        onDismissRequest = { onRespondida(false) },
        title = {
            Text("¿Asignar los ${oferta.pesajes.size} pesajes pendientes de ${oferta.codigo} a ${oferta.nombreAnimal}?")
        },
        text = {
            Column {
                oferta.pesajes.forEach { pendiente ->
                    Text(
                        text = "${pendiente.peso} kg · ${formatoFecha.format(Date(pendiente.timestamp))}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = { onRespondida(true) }) { Text("Sí") } },
        dismissButton = { TextButton(onClick = { onRespondida(false) }) { Text("No") } },
    )
}
