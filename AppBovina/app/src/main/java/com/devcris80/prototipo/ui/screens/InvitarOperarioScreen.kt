package com.devcris80.prototipo.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.devcris80.prototipo.domain.repository.Invitacion
import com.devcris80.prototipo.ui.components.BannerInformativo
import com.devcris80.prototipo.ui.components.BovinaCard
import com.devcris80.prototipo.ui.components.BovinaPrimaryButton
import com.devcris80.prototipo.ui.components.BovinaSecondaryButton
import com.devcris80.prototipo.ui.components.BovinaTopBar
import com.devcris80.prototipo.ui.theme.BovinaScreenGutter
import com.devcris80.prototipo.ui.util.appViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Invitar a un Operario (CU-04): el Ganadero genera el código y se lo da a la persona. */
@Composable
fun InvitarOperarioScreen(onVolver: () -> Unit) {
    val viewModel = appViewModel { container -> InvitarOperarioViewModel(container.invitacionRepository) }
    val estado by viewModel.estado.collectAsState()
    val contexto = LocalContext.current
    val formatoFecha = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    Scaffold(
        topBar = { BovinaTopBar(titulo = "Invitar a un Operario", onVolver = onVolver) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = BovinaScreenGutter, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Genera un código y dáselo a la persona. Con él, en Crear cuenta, entra a esta finca como " +
                    "Operario. Cada código sirve una sola vez y vence a los 7 días.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            estado.invitacion?.let { invitacion ->
                val vence = formatoFecha.format(Date(invitacion.venceEnMillis))
                CodigoGenerado(
                    invitacion = invitacion,
                    vence = vence,
                    onCompartir = {
                        val envio = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Tu código para unirte a mi finca en MiGanado: ${invitacion.codigo}. " +
                                    "Vence el $vence.",
                            )
                        }
                        contexto.startActivity(Intent.createChooser(envio, "Compartir código"))
                    },
                )
            }

            estado.error?.let { error ->
                Text(text = error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }

            BannerInformativo(
                texto = "Generar un código necesita señal.",
                icon = Icons.Filled.WifiOff,
            )
            BovinaPrimaryButton(
                texto = when {
                    estado.generando -> "Generando…"
                    estado.invitacion != null -> "Generar otro código"
                    else -> "Generar código"
                },
                onClick = viewModel::onGenerar,
                enabled = !estado.generando,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun CodigoGenerado(invitacion: Invitacion, vence: String, onCompartir: () -> Unit) {
    BovinaCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "Código de invitación", style = MaterialTheme.typography.labelLarge)
            // Seleccionable para copiarlo; monoespaciado para que no se confundan los caracteres.
            SelectionContainer {
                Text(
                    text = invitacion.codigo,
                    style = MaterialTheme.typography.headlineLarge,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = "Vence el $vence",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            BovinaSecondaryButton(
                texto = "Compartir",
                onClick = onCompartir,
                icon = Icons.Filled.Share,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
