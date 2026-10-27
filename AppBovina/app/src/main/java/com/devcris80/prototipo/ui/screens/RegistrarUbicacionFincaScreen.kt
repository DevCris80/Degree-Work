package com.devcris80.prototipo.ui.screens

import android.Manifest
import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.devcris80.prototipo.ui.util.appViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrarUbicacionFincaScreen(
    onVolver: () -> Unit,
    onUbicacionGuardada: () -> Unit,
) {
    val context = LocalContext.current
    val viewModel = appViewModel { container ->
        RegistrarUbicacionFincaViewModel(container.cuentaRepository)
    }
    val estado by viewModel.estado.collectAsState()

    fun obtenerUltimaUbicacion(ctx: Context) {
        val manager = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val ubicacion = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        if (ubicacion != null) {
            viewModel.onUbicacionDelGps(ubicacion.latitude, ubicacion.longitude)
        }
    }

    val solicitarUbicacion = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) {
            obtenerUltimaUbicacion(context)
        }
    }

    var permisoSolicitado by remember { mutableStateOf(false) }

    LaunchedEffect(estado.guardado) {
        if (estado.guardado) onUbicacionGuardada()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ubicación de la finca") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Filled.Save, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
        ) {
            Text(
                text = "Finca: ${estado.nombreFinca}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                IconButton(
                    onClick = {
                        permisoSolicitado = true
                        solicitarUbicacion.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    },
                    enabled = !estado.cargando,
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Usar GPS",
                    )
                }
                if (permisoSolicitado) {
                    Text(
                        text = "Si el GPS no responde, ingresa las coordenadas a mano.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                androidx.compose.material3.OutlinedTextField(
                    value = estado.latitud,
                    onValueChange = { viewModel.onLatitudCambiada(it) },
                    label = { Text("Latitud") },
                    modifier = Modifier.weight(1f),
                )
                androidx.compose.material3.OutlinedTextField(
                    value = estado.longitud,
                    onValueChange = { viewModel.onLongitudCambiada(it) },
                    label = { Text("Longitud") },
                    modifier = Modifier.weight(1f),
                )
            }

            estado.error?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onVolver) {
                    Text("Cancelar")
                }
                Button(
                    onClick = { viewModel.onGuardarUbicacion() },
                    enabled = !estado.cargando,
                ) {
                    if (estado.cargando) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Text(text = "Guardar")
                    }
                }
            }
        }
    }
}
