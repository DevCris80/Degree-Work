package com.devcris80.prototipo.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.unit.dp
import com.devcris80.prototipo.domain.model.PesajePendiente
import com.devcris80.prototipo.domain.usecase.EstadoCodigo
import com.devcris80.prototipo.domain.usecase.PesajePendientePorConciliar
import com.devcris80.prototipo.ui.theme.BovinaScreenGutter
import com.devcris80.prototipo.ui.util.aEtiqueta
import com.devcris80.prototipo.ui.util.appViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PesajesPendientesScreen(
    onVolver: () -> Unit,
    onRegistrarAnimalConCodigo: (codigo: String) -> Unit,
) {
    val viewModel = appViewModel { container ->
        PesajesPendientesViewModel(
            container.observarPesajesPendientes,
            container.animalRepository,
            container.chapetaRepository,
            container.pesajePendienteRepository,
            container.asignarPesajesPendientes,
            container.descartarPesajePendiente,
        )
    }
    val estado by viewModel.estado.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val formatoFecha = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    var pendienteParaAsignar by remember { mutableStateOf<PesajePendientePorConciliar?>(null) }
    var busquedaAnimal by remember { mutableStateOf("") }
    var confirmarAsignacion by remember { mutableStateOf<Pair<PesajePendiente, AnimalConCodigo>?>(null) }
    var confirmarDescarte by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(estado.error) {
        val mensaje = when (estado.error) {
            PesajesPendientesError.AnimalNoDisponible -> "Ese animal ya no está disponible."
            PesajesPendientesError.PendienteNoDisponible -> "Ese pesaje pendiente ya se resolvió."
            null -> null
        }
        if (mensaje != null) {
            snackbarHostState.showSnackbar(mensaje)
            viewModel.onErrorVisto()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pesajes pendientes") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } },
    ) { innerPadding ->
        if (estado.pendientes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No hay pesajes pendientes.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = BovinaScreenGutter),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = BovinaScreenGutter),
            ) {
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
                    OutlinedTextField(
                        value = busquedaAnimal,
                        onValueChange = { busquedaAnimal = it },
                        placeholder = { Text("Buscar por nombre o código") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
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
                    viewModel.onConfirmarAsignar(pendiente, animalConCodigo.animal.idAnimal)
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
                    viewModel.onConfirmarDescartar(idPesajePendiente)
                }) { Text("Descartar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarDescarte = null }) { Text("Cancelar") }
            },
        )
    }

    estado.ofertaRestantes?.let { oferta ->
        AlertDialog(
            onDismissRequest = viewModel::onAsignarRestantesDescartado,
            title = { Text("¿Asignar también los otros ${oferta.cantidad} pesajes pendientes de ${oferta.codigo}?") },
            confirmButton = {
                TextButton(onClick = viewModel::onAsignarRestantesConfirmado) { Text("Asignar") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onAsignarRestantesDescartado) { Text("No") }
            },
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
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = pendiente.codigo, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${pendiente.peso} kg · ${formatoFecha.format(Date(pendiente.timestamp))}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Motivo: ${pendiente.motivo.aEtiqueta()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            when (val estadoCodigo = item.estadoCodigo) {
                is EstadoCodigo.ChapetaDeAnimalActivo -> Text(
                    text = "Sugerido: ${estadoCodigo.animal.nombre}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                EstadoCodigo.ChapetaDeAnimalDadoDeBaja -> Text(
                    text = "La chapeta es de un animal dado de baja",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                EstadoCodigo.SinChapetaActiva -> Unit
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = onAsignarClick) { Text("Asignar") }
                if (item.estadoCodigo is EstadoCodigo.SinChapetaActiva) {
                    OutlinedButton(onClick = onRegistrarAnimalClick) { Text("Registrar animal") }
                }
                TextButton(onClick = onDescartarClick) { Text("Descartar") }
            }
        }
    }
}
