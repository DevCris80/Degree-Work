package com.devcris80.prototipo.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.material3.SelectableDates
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.createSavedStateHandle
import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.model.Sexo
import com.devcris80.prototipo.ui.components.BannerInformativo
import com.devcris80.prototipo.ui.components.CampoConIcono
import com.devcris80.prototipo.ui.components.FotoAnimal
import com.devcris80.prototipo.ui.theme.BovinaMinFieldHeight
import com.devcris80.prototipo.ui.theme.BovinaScreenGutter
import com.devcris80.prototipo.ui.util.aEtiqueta
import com.devcris80.prototipo.ui.util.appViewModel
import com.devcris80.prototipo.ui.util.crearArchivoFotoUri
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val RAZAS = listOf(
    "Holstein", "Jersey", "Normando", "Pardo Suizo", "Brahman", "Cebú",
    "Angus", "Simmental", "Criollo", "Otra",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrarAnimalScreen(
    onGuardado: () -> Unit,
    onCancelar: () -> Unit,
) {
    val viewModel = appViewModel { container ->
        val savedState = createSavedStateHandle()
        RegistrarAnimalViewModel(
            savedState.get<String>("codigo"),
            savedState.get<String>("aviso"),
            container.cuentaRepository,
            container.registrarAnimal,
            container.asignarPesajesPendientes,
        )
    }
    val estado by viewModel.estado.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var razaExpandida by remember { mutableStateOf(false) }
    var mostrarSelectorFecha by remember { mutableStateOf(false) }
    val formatoFecha = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    LaunchedEffect(viewModel) {
        viewModel.eventos.collect { evento ->
            when (evento) {
                RegistrarAnimalEvento.Guardado -> onGuardado()
            }
        }
    }
    LaunchedEffect(estado.codigoYaActivo) {
        if (estado.codigoYaActivo) {
            snackbarHostState.showSnackbar("Esta chapeta ya está asociada a un animal activo")
            viewModel.onCodigoYaActivoVisto()
        }
    }
    LaunchedEffect(estado.errorAsignacionPendientes) {
        if (estado.errorAsignacionPendientes) {
            snackbarHostState.showSnackbar("No se pudieron asignar los pesajes pendientes")
            viewModel.onErrorAsignacionPendientesVisto()
        }
    }

    val nfcAction: @Composable () -> Unit = {
        IconButton(onClick = {
            scope.launch {
                snackbarHostState.showSnackbar(
                    "Escaneo NFC disponible cuando haya tags físicos (Fase 3).",
                )
            }
        }) {
            Icon(Icons.Filled.Nfc, contentDescription = "Escanear con NFC")
        }
    }

    var fotoTempUri by remember { mutableStateOf<Uri?>(null) }
    val tomarFotoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { exito ->
        if (exito) viewModel.onFotoCapturada(fotoTempUri?.toString())
    }
    val elegirDeGaleriaLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri -> if (uri != null) viewModel.onFotoCapturada(uri.toString()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrar Animal") },
                navigationIcon = {
                    IconButton(onClick = onCancelar) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(BovinaScreenGutter),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            BannerInformativo(
                texto = "Complete los datos del animal. Podrá editarlos más adelante desde su ficha.",
                icon = Icons.Filled.Info,
            )
            estado.aviso?.let { aviso ->
                BannerInformativo(texto = aviso, icon = Icons.Filled.Info)
            }

            FotoAnimal(
                fotoUri = estado.fotoUri,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        val uri = crearArchivoFotoUri(context)
                        fotoTempUri = uri
                        tomarFotoLauncher.launch(uri)
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null)
                    Text(" Tomar foto")
                }
                OutlinedButton(
                    onClick = { elegirDeGaleriaLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                    Text(" De galería")
                }
            }

            CampoConIcono(
                label = "Nombre o Apodo",
                value = estado.nombre,
                onValueChange = viewModel::onNombreCambiado,
                icon = Icons.Filled.Person,
            )

            CampoConIcono(
                label = "Número de chapeta / arete (opcional)",
                value = estado.codigo,
                onValueChange = viewModel::onCodigoCambiado,
                icon = Icons.Filled.Numbers,
                readOnly = estado.codigoSoloLectura,
                trailingAction = if (estado.codigoSoloLectura) null else nfcAction,
            )

            ExposedDropdownMenuBox(
                expanded = razaExpandida,
                onExpandedChange = { razaExpandida = it },
            ) {
                CampoConIcono(
                    label = "Raza principal",
                    value = estado.raza,
                    onValueChange = {},
                    icon = Icons.Filled.Info,
                    readOnly = true,
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                )
                ExposedDropdownMenu(
                    expanded = razaExpandida,
                    onDismissRequest = { razaExpandida = false },
                ) {
                    RAZAS.forEach { opcion ->
                        DropdownMenuItem(
                            text = { Text(opcion) },
                            onClick = {
                                viewModel.onRazaCambiada(opcion)
                                razaExpandida = false
                            },
                        )
                    }
                }
            }

            Text("Sexo del ejemplar", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Sexo.entries.forEach { opcion ->
                    TarjetaSeleccionable(
                        texto = opcion.aEtiqueta(),
                        seleccionada = estado.sexo == opcion,
                        onClick = { viewModel.onSexoSeleccionado(opcion) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (estado.etapasDisponibles.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    estado.etapasDisponibles.forEach { opcion ->
                        FilterChip(
                            selected = estado.etapa == opcion,
                            onClick = { viewModel.onEtapaSeleccionada(opcion) },
                            label = { Text(opcion.aEtiqueta()) },
                        )
                    }
                }
            }

            Text("Fecha de nacimiento", style = MaterialTheme.typography.labelLarge)
            OutlinedButton(
                onClick = { mostrarSelectorFecha = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BovinaMinFieldHeight),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                Text(
                    text = estado.fechaNacimiento?.let { formatoFecha.format(Date(it)) } ?: "Seleccionar fecha",
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = estado.fechaNacimientoEsEstimada,
                    onCheckedChange = viewModel::onFechaEstimadaCambiada,
                )
                Text("Fecha estimada", style = MaterialTheme.typography.bodyMedium)
            }

            if (mostrarSelectorFecha) {
                val selectorState = rememberDatePickerState(
                    initialSelectedDateMillis = estado.fechaNacimiento ?: System.currentTimeMillis(),
                    selectableDates = object : SelectableDates {
                        override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                            utcTimeMillis <= System.currentTimeMillis()
                    },
                )
                DatePickerDialog(
                    onDismissRequest = { mostrarSelectorFecha = false },
                    confirmButton = {
                        TextButton(onClick = {
                            selectorState.selectedDateMillis?.let(viewModel::onFechaNacimientoCambiada)
                            mostrarSelectorFecha = false
                        }) { Text("Aceptar") }
                    },
                    dismissButton = {
                        TextButton(onClick = { mostrarSelectorFecha = false }) { Text("Cancelar") }
                    },
                ) {
                    DatePicker(state = selectorState)
                }
            }

            Text("Propósito productivo", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Proposito.entries.forEach { opcion ->
                    TarjetaSeleccionable(
                        texto = opcion.aEtiqueta(),
                        seleccionada = estado.proposito == opcion,
                        onClick = { viewModel.onPropositoSeleccionado(opcion) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Button(
                enabled = estado.puedeGuardar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BovinaMinFieldHeight),
                shape = RoundedCornerShape(12.dp),
                onClick = viewModel::onGuardar,
            ) {
                Text("Guardar animal", style = MaterialTheme.typography.labelLarge)
            }
            TextButton(onClick = onCancelar, modifier = Modifier.fillMaxWidth()) {
                Text("Cancelar y volver")
            }
        }
    }

    estado.ofertaPesajesPendientes?.let { oferta ->
        AlertDialog(
            onDismissRequest = { viewModel.onOfertaPesajesPendientesRespondida(false) },
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
            confirmButton = {
                TextButton(onClick = { viewModel.onOfertaPesajesPendientesRespondida(true) }) { Text("Sí") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onOfertaPesajesPendientesRespondida(false) }) { Text("No") }
            },
        )
    }
}

@Composable
private fun TarjetaSeleccionable(
    texto: String,
    seleccionada: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (seleccionada) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
        border = if (seleccionada) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        },
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (seleccionada) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(16.dp),
        )
    }
}
