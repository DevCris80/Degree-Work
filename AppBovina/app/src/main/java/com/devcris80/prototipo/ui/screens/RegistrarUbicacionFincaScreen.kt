package com.devcris80.prototipo.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.devcris80.prototipo.ui.util.appViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

private const val ESPERA_GPS_MS = 20_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrarUbicacionFincaScreen(
    onVolver: () -> Unit,
    onUbicacionGuardada: () -> Unit,
) {
    val context = LocalContext.current
    val alcance = rememberCoroutineScope()
    val viewModel = appViewModel { container ->
        RegistrarUbicacionFincaViewModel(container.cuentaRepository)
    }
    val estado by viewModel.estado.collectAsState()

    fun buscarUbicacion() {
        viewModel.onBuscandoUbicacion()
        alcance.launch {
            val posicion = posicionActual(context)
            if (posicion != null) {
                viewModel.onUbicacionDelGps(posicion.latitude, posicion.longitude)
            } else {
                viewModel.onUbicacionNoDisponible()
            }
        }
    }

    // Desde Android 12 el usuario puede conceder solo la ubicación aproximada: con cualquiera de las dos sirve.
    val solicitarUbicacion = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permisos ->
        if (permisos.values.any { it }) buscarUbicacion() else viewModel.onPermisoNegado()
    }

    LaunchedEffect(estado.guardado) {
        if (estado.guardado) onUbicacionGuardada()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ubicación de la finca") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
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
                if (estado.buscandoUbicacion) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text(
                        text = "Buscando la posición del GPS…",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                } else {
                    IconButton(
                        onClick = {
                            solicitarUbicacion.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION,
                                ),
                            )
                        },
                        enabled = !estado.cargando,
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Usar GPS",
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = estado.latitud,
                    onValueChange = { viewModel.onLatitudCambiada(it) },
                    label = { Text("Latitud") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = estado.longitud,
                    onValueChange = { viewModel.onLongitudCambiada(it) },
                    label = { Text("Longitud") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
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
                    enabled = !estado.cargando && !estado.buscandoUbicacion,
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

/**
 * Pide una posición nueva al GPS, que funciona sin señal de datos; si el GPS está apagado, a la red. La
 * última posición en caché puede ser vieja o no existir, así que no se usa. Devuelve null si no hay
 * proveedor activo o no responde en [ESPERA_GPS_MS].
 */
@SuppressLint("MissingPermission") // Solo se llama con el permiso ya concedido.
private suspend fun posicionActual(context: Context): Location? {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val proveedor = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .firstOrNull { manager.isProviderEnabled(it) }
        ?: return null
    return withTimeoutOrNull(ESPERA_GPS_MS) {
        suspendCancellableCoroutine { continuacion ->
            val senal = CancellationSignal()
            continuacion.invokeOnCancellation { senal.cancel() }
            LocationManagerCompat.getCurrentLocation(
                manager,
                proveedor,
                senal,
                ContextCompat.getMainExecutor(context),
            ) { continuacion.resume(it) }
        }
    }
}
