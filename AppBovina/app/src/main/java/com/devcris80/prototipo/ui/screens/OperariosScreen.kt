package com.devcris80.prototipo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devcris80.prototipo.domain.repository.OperarioDeLaFinca
import com.devcris80.prototipo.ui.components.BovinaCard
import com.devcris80.prototipo.ui.components.BovinaSecondaryButton
import com.devcris80.prototipo.ui.components.BovinaTopBar
import com.devcris80.prototipo.ui.theme.BovinaScreenGutter
import com.devcris80.prototipo.ui.util.appViewModel

/**
 * Los Operarios de la finca y la opción de quitarlos (CU-05, RF-37). Solo el Ganadero llega aquí, desde el menú de
 * Configuración. Quitar pide confirmación explicando el efecto (RNF-16).
 */
@Composable
fun OperariosScreen(onVolver: () -> Unit) {
    val viewModel = appViewModel { container -> OperariosViewModel(container.operarioRepository, container.quitarOperario) }
    val estado by viewModel.estado.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var porQuitar by remember { mutableStateOf<OperarioDeLaFinca?>(null) }

    LaunchedEffect(estado.aviso) {
        estado.aviso?.let {
            viewModel.onAvisoMostrado()
            snackbar.showSnackbar(it)
        }
    }

    Scaffold(
        topBar = { BovinaTopBar(titulo = "Operarios de la finca", onVolver = onVolver) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(
                horizontal = BovinaScreenGutter,
                vertical = 4.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = "Quitar a un Operario le cierra el acceso a la finca. Lo que registró se conserva a su nombre.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (estado.cargando) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            estado.error?.let { error ->
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                        if (estado.operarios == null) {
                            BovinaSecondaryButton(texto = "Reintentar", onClick = viewModel::onReintentar)
                        }
                    }
                }
            }
            val operarios = estado.operarios
            if (operarios != null && operarios.isEmpty() && !estado.cargando) {
                item {
                    Text(
                        text = "La finca no tiene Operarios. Invita a uno desde Configuración.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(operarios.orEmpty(), key = { it.idUsuario }) { operario ->
                BovinaCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = operario.nombre,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { porQuitar = operario }, enabled = !estado.quitando) {
                            Text(text = "Quitar", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    porQuitar?.let { operario ->
        AlertDialog(
            onDismissRequest = { porQuitar = null },
            title = { Text("¿Quitar a ${operario.nombre} de la finca?") },
            text = {
                Text(
                    "${operario.nombre} dejará de ver y de escribir datos de la finca, y su sesión se cerrará cuando su " +
                        "celular sincronice. Lo que capturó sin señal antes de eso no llegará al servidor. Los pesajes y " +
                        "eventos que ya registró se conservan a su nombre. Esto no se puede deshacer desde la app.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    porQuitar = null
                    viewModel.onQuitar(operario)
                }) { Text("Quitar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { porQuitar = null }) { Text("Cancelar") } },
        )
    }
}
