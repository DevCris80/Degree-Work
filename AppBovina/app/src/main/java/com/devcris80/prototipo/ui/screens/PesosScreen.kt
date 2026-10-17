package com.devcris80.prototipo.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.devcris80.prototipo.domain.model.PesajePendiente
import com.devcris80.prototipo.domain.usecase.EstadoCodigo
import com.devcris80.prototipo.domain.usecase.PesajePendientePorConciliar
import com.devcris80.prototipo.service.PesajeHttpForegroundService
import com.devcris80.prototipo.ui.components.BovinaCard
import com.devcris80.prototipo.ui.components.BovinaPrimaryButton
import com.devcris80.prototipo.ui.components.BovinaSecondaryButton
import com.devcris80.prototipo.ui.components.BovinaTextField
import com.devcris80.prototipo.ui.components.BovinaTopBar
import com.devcris80.prototipo.ui.components.PillBadge
import com.devcris80.prototipo.ui.theme.BovinaGradientEnd
import com.devcris80.prototipo.ui.theme.BovinaGradientStart
import com.devcris80.prototipo.ui.theme.BovinaScreenGutter
import com.devcris80.prototipo.ui.util.aEtiqueta
import com.devcris80.prototipo.ui.util.appViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pantalla de Pesos: el servicio que recibe los pesajes del ESP32 (antes ServiceStatusScreen) y la
 * lista de pesajes pendientes por conciliar (antes PesajesPendientesScreen).
 */
@Composable
fun PesosScreen(
    onRegistrarAnimalConCodigo: (codigo: String) -> Unit,
) {
    val pendientesViewModel = appViewModel { container ->
        PesajesPendientesViewModel(
            container.observarPesajesPendientes,
            container.animalRepository,
            container.chapetaRepository,
            container.pesajePendienteRepository,
            container.asignarPesajesPendientes,
            container.descartarPesajePendiente,
        )
    }
    val servicioViewModel = appViewModel { EstadoServicioViewModel() }
    val estado by pendientesViewModel.estado.collectAsState()
    val estadoServicio by servicioViewModel.estado.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val formatoFecha = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    var pendienteParaAsignar by remember { mutableStateOf<PesajePendientePorConciliar?>(null) }
    var busquedaAnimal by remember { mutableStateOf("") }
    var confirmarAsignacion by remember { mutableStateOf<Pair<PesajePendiente, AnimalConCodigo>?>(null) }
    var confirmarDescarte by remember { mutableStateOf<String?>(null) }

    val requestNotificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            PesajeHttpForegroundService.start(context)
        }
    }

    LaunchedEffect(estado.error) {
        val mensaje = when (estado.error) {
            PesajesPendientesError.AnimalNoDisponible -> "Ese animal ya no está disponible."
            PesajesPendientesError.PendienteNoDisponible -> "Ese pesaje pendiente ya se resolvió."
            null -> null
        }
        if (mensaje != null) {
            snackbarHostState.showSnackbar(mensaje)
            pendientesViewModel.onErrorVisto()
        }
    }

    Scaffold(
        topBar = { BovinaTopBar(titulo = "Pesos", mostrarLogo = true) },
        snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = BovinaScreenGutter),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = BovinaScreenGutter),
        ) {
            item {
                TarjetaServicio(
                    activo = estadoServicio.activo,
                    onAlternar = {
                        if (estadoServicio.activo) {
                            PesajeHttpForegroundService.stop(context)
                        } else if (notificationPermissionGranted(context)) {
                            PesajeHttpForegroundService.start(context)
                        } else {
                            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                )
            }
            item {
                Text(
                    text = "Pesajes pendientes (${estado.pendientes.size})",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                )
            }
            if (estado.pendientes.isEmpty()) {
                item {
                    Text(
                        text = "No hay pesajes pendientes.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            } else {
                items(estado.pendientes, key = { it.pesajePendiente.idPesajePendiente }) { item ->
                    FilaPendiente(
                        item = item,
                        formatoFecha = formatoFecha,
                        onAsignarClick = { pendienteParaAsignar = item },
                        onDescartarClick = { confirmarDescarte = item.pesajePendiente.idPesajePendiente },
                        onRegistrarAnimalClick = { onRegistrarAnimalConCodigo(item.pesajePendiente.codigo) },
                    )
                }
            }
        }
    }

    pendienteParaAsignar?.let { item ->
        val sugerido = (item.estadoCodigo as? EstadoCodigo.ChapetaDeAnimalActivo)?.animal?.idAnimal
        val filtrados = estado.animalesActivos
            .sortedByDescending { it.animal.idAnimal == sugerido }
            .filter {
                busquedaAnimal.isBlank() ||
                    it.animal.nombre.contains(busquedaAnimal, ignoreCase = true) ||
                    it.codigoChapeta?.contains(busquedaAnimal, ignoreCase = true) == true
            }
        AlertDialog(
            onDismissRequest = {
                pendienteParaAsignar = null
                busquedaAnimal = ""
            },
            title = { Text("Asignar a un animal") },
            text = {
                Column {
                    BovinaTextField(
                        value = busquedaAnimal,
                        onValueChange = { busquedaAnimal = it },
                        placeholder = "Buscar por nombre o código",
                    )
                    LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                        items(filtrados, key = { it.animal.idAnimal }) { animalConCodigo ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        confirmarAsignacion = item.pesajePendiente to animalConCodigo
                                        pendienteParaAsignar = null
                                        busquedaAnimal = ""
                                    }
                                    .padding(vertical = 12.dp),
                            ) {
                                Text(
                                    text = animalConCodigo.codigoChapeta?.let { "${animalConCodigo.animal.nombre} ($it)" }
                                        ?: animalConCodigo.animal.nombre,
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    pendienteParaAsignar = null
                    busquedaAnimal = ""
                }) { Text("Cancelar") }
            },
        )
    }

    confirmarAsignacion?.let { (pendiente, animalConCodigo) ->
        AlertDialog(
            onDismissRequest = { confirmarAsignacion = null },
            title = { Text("¿Asignar este pesaje a ${animalConCodigo.animal.nombre}?") },
            text = { Text("Es definitivo: crea un registro de pesaje para ese animal.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarAsignacion = null
                    pendientesViewModel.onConfirmarAsignar(pendiente, animalConCodigo.animal.idAnimal)
                }) { Text("Asignar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarAsignacion = null }) { Text("Cancelar") }
            },
        )
    }

    confirmarDescarte?.let { idPesajePendiente ->
        AlertDialog(
            onDismissRequest = { confirmarDescarte = null },
            title = { Text("¿Descartar este pesaje pendiente?") },
            text = { Text("Es definitivo: no se asignará a ningún animal.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarDescarte = null
                    pendientesViewModel.onConfirmarDescartar(idPesajePendiente)
                }) { Text("Descartar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarDescarte = null }) { Text("Cancelar") }
            },
        )
    }

    estado.ofertaRestantes?.let { oferta ->
        AlertDialog(
            onDismissRequest = pendientesViewModel::onAsignarRestantesDescartado,
            title = { Text("¿Asignar también los otros ${oferta.cantidad} pesajes pendientes de ${oferta.codigo}?") },
            confirmButton = {
                TextButton(onClick = pendientesViewModel::onAsignarRestantesConfirmado) { Text("Asignar") }
            },
            dismissButton = {
                TextButton(onClick = pendientesViewModel::onAsignarRestantesDescartado) { Text("No") }
            },
        )
    }
}

/** Tarjeta verde del servicio de captura: estado real y botón para iniciarlo o detenerlo. */
@Composable
private fun TarjetaServicio(
    activo: Boolean,
    onAlternar: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(Brush.linearGradient(listOf(BovinaGradientStart, BovinaGradientEnd)))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PillBadge(
            texto = if (activo) "SERVICIO ACTIVO" else "SERVICIO INACTIVO",
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            contentColor = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "Captura de pesajes",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onPrimary,
        )
        Text(
            text = "Captura automática directa vía ESP32.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
        )
        BovinaSecondaryButton(
            texto = if (activo) "Detener servicio" else "Iniciar servicio",
            onClick = onAlternar,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun FilaPendiente(
    item: PesajePendientePorConciliar,
    formatoFecha: SimpleDateFormat,
    onAsignarClick: () -> Unit,
    onDescartarClick: () -> Unit,
    onRegistrarAnimalClick: () -> Unit,
) {
    val pendiente = item.pesajePendiente
    BovinaCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                PillBadge(texto = pendiente.codigo)
                Text(
                    text = formatoFecha.format(Date(pendiente.timestamp)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Text(
                    text = "Motivo: ${pendiente.motivo.aEtiqueta()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "${pendiente.peso} kg",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        when (val estadoCodigo = item.estadoCodigo) {
            is EstadoCodigo.ChapetaDeAnimalActivo -> Text(
                text = "Sugerido: ${estadoCodigo.animal.nombre}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(12.dp),
            )
            EstadoCodigo.ChapetaDeAnimalDadoDeBaja -> Text(
                text = "La chapeta es de un animal dado de baja",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(12.dp),
            )
            EstadoCodigo.SinChapetaActiva -> Unit
        }
        BovinaPrimaryButton(
            texto = "Asignar",
            onClick = onAsignarClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (item.estadoCodigo is EstadoCodigo.SinChapetaActiva) {
                BovinaSecondaryButton(
                    texto = "Registrar animal",
                    onClick = onRegistrarAnimalClick,
                    modifier = Modifier.weight(1f),
                )
            }
            TextButton(
                onClick = onDescartarClick,
                modifier = Modifier.weight(1f),
            ) { Text("Descartar", color = MaterialTheme.colorScheme.error) }
        }
    }
}

private fun notificationPermissionGranted(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
    ) == PackageManager.PERMISSION_GRANTED
}
