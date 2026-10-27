package com.devcris80.prototipo.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.tooling.preview.Preview
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.createSavedStateHandle
import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.model.Sexo
import com.devcris80.prototipo.ui.components.BannerInformativo
import com.devcris80.prototipo.ui.components.BovinaFilterChip
import com.devcris80.prototipo.ui.components.BovinaPrimaryButton
import com.devcris80.prototipo.ui.components.DialogoFechaHastaHoy
import com.devcris80.prototipo.ui.components.BovinaSecondaryButton
import com.devcris80.prototipo.ui.components.BovinaTopBar
import com.devcris80.prototipo.ui.components.CampoConIcono
import com.devcris80.prototipo.ui.components.FotoAnimal
import com.devcris80.prototipo.ui.theme.BovinaForest
import com.devcris80.prototipo.ui.theme.BovinaScreenGutter
import com.devcris80.prototipo.ui.theme.Prototipo1Theme
import com.devcris80.prototipo.ui.util.aEtiqueta
import com.devcris80.prototipo.ui.util.appViewModel
import com.devcris80.prototipo.ui.util.copiarFotoElegida
import com.devcris80.prototipo.ui.util.crearArchivoFotoUri
import com.devcris80.prototipo.ui.util.nombreConBaja
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val RAZAS = listOf(
    "Holstein", "Jersey", "Normando", "Pardo Suizo", "Brahman", "Cebú",
    "Angus", "Simmental", "Criollo", "Otra",
)

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
            savedState.get<String>("idMadre"),
            container.cuentaRepository,
            container.animalRepository,
            container.registrarAnimal,
            container.asignarPesajesPendientes,
        )
    }
    val estado by viewModel.estado.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

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

    var fotoTempUri by remember { mutableStateOf<Uri?>(null) }
    val tomarFotoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { exito ->
        if (exito) viewModel.onFotoCapturada(fotoTempUri?.toString())
    }
    val elegirDeGaleriaLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) scope.launch { copiarFotoElegida(context, uri)?.let { viewModel.onFotoCapturada(it.toString()) } }
    }

    RegistrarAnimalContent(
        estado = estado,
        snackbarHostState = snackbarHostState,
        onCancelar = onCancelar,
        onTomarFoto = {
            val uri = crearArchivoFotoUri(context)
            fotoTempUri = uri
            tomarFotoLauncher.launch(uri)
        },
        onElegirDeGaleria = { elegirDeGaleriaLauncher.launch("image/*") },
        onNfcClick = {
            scope.launch {
                snackbarHostState.showSnackbar(
                    "Escaneo NFC disponible cuando haya tags físicos (Fase 3).",
                )
            }
        },
        onNombreCambiado = viewModel::onNombreCambiado,
        onCodigoCambiado = viewModel::onCodigoCambiado,
        onRazaCambiada = viewModel::onRazaCambiada,
        onSexoSeleccionado = viewModel::onSexoSeleccionado,
        onEtapaSeleccionada = viewModel::onEtapaSeleccionada,
        onFechaNacimientoCambiada = viewModel::onFechaNacimientoCambiada,
        onFechaEstimadaCambiada = viewModel::onFechaEstimadaCambiada,
        onPropositoSeleccionado = viewModel::onPropositoSeleccionado,
        onPadreSeleccionado = viewModel::onPadreSeleccionado,
        onMadreSeleccionada = viewModel::onMadreSeleccionada,
        onGuardar = viewModel::onGuardar,
        onOfertaPesajesPendientesRespondida = viewModel::onOfertaPesajesPendientesRespondida,
    )
}

/** Editar Animal: el mismo formulario que Registrar, con los datos guardados y sin el campo de chapeta. */
@Composable
fun EditarAnimalScreen(
    onGuardado: () -> Unit,
    onCancelar: () -> Unit,
) {
    val viewModel = appViewModel { container ->
        val savedState = createSavedStateHandle()
        EditarAnimalViewModel(
            checkNotNull(savedState.get<String>("idAnimal")),
            container.animalRepository,
            container.actualizarAnimal,
        )
    }
    val estado by viewModel.estado.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.eventos.collect { evento ->
            when (evento) {
                EditarAnimalEvento.Guardado -> onGuardado()
            }
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
    ) { uri ->
        if (uri != null) scope.launch { copiarFotoElegida(context, uri)?.let { viewModel.onFotoCapturada(it.toString()) } }
    }

    RegistrarAnimalContent(
        estado = estado,
        esEdicion = true,
        snackbarHostState = snackbarHostState,
        onCancelar = onCancelar,
        onTomarFoto = {
            val uri = crearArchivoFotoUri(context)
            fotoTempUri = uri
            tomarFotoLauncher.launch(uri)
        },
        onElegirDeGaleria = { elegirDeGaleriaLauncher.launch("image/*") },
        onNfcClick = {},
        onNombreCambiado = viewModel::onNombreCambiado,
        onCodigoCambiado = {},
        onRazaCambiada = viewModel::onRazaCambiada,
        onSexoSeleccionado = viewModel::onSexoSeleccionado,
        onEtapaSeleccionada = viewModel::onEtapaSeleccionada,
        onFechaNacimientoCambiada = viewModel::onFechaNacimientoCambiada,
        onFechaEstimadaCambiada = viewModel::onFechaEstimadaCambiada,
        onPropositoSeleccionado = viewModel::onPropositoSeleccionado,
        onPadreSeleccionado = viewModel::onPadreSeleccionado,
        onMadreSeleccionada = viewModel::onMadreSeleccionada,
        onGuardar = viewModel::onGuardar,
        onOfertaPesajesPendientesRespondida = {},
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegistrarAnimalContent(
    estado: RegistrarAnimalUiState,
    esEdicion: Boolean = false,
    snackbarHostState: SnackbarHostState,
    onCancelar: () -> Unit,
    onTomarFoto: () -> Unit,
    onElegirDeGaleria: () -> Unit,
    onNfcClick: () -> Unit,
    onNombreCambiado: (String) -> Unit,
    onCodigoCambiado: (String) -> Unit,
    onRazaCambiada: (String) -> Unit,
    onSexoSeleccionado: (Sexo) -> Unit,
    onEtapaSeleccionada: (Etapa) -> Unit,
    onFechaNacimientoCambiada: (Long) -> Unit,
    onFechaEstimadaCambiada: (Boolean) -> Unit,
    onPropositoSeleccionado: (Proposito) -> Unit,
    onPadreSeleccionado: (String?) -> Unit,
    onMadreSeleccionada: (String?) -> Unit,
    onGuardar: () -> Unit,
    onOfertaPesajesPendientesRespondida: (Boolean) -> Unit,
) {
    var razaExpandida by remember { mutableStateOf(false) }
    var mostrarSelectorFecha by remember { mutableStateOf(false) }
    val formatoFecha = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    val nfcAction: @Composable () -> Unit = {
        IconButton(onClick = onNfcClick) {
            Icon(Icons.Filled.Nfc, contentDescription = "Escanear con NFC")
        }
    }

    Scaffold(
        topBar = {
            BovinaTopBar(titulo = if (esEdicion) "Editar Animal" else "Registrar Animal", onVolver = onCancelar)
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
                texto = when {
                    estado.noEditable -> "Este animal está dado de baja y no se puede editar."
                    esEdicion -> "Cambie los datos que necesite. La chapeta no se modifica desde aquí."
                    else -> "Complete los datos del animal. Podrá editarlos más adelante desde su ficha."
                },
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
                BovinaSecondaryButton(
                    texto = "Tomar foto",
                    onClick = onTomarFoto,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.CameraAlt,
                )
                BovinaSecondaryButton(
                    texto = "De galería",
                    onClick = onElegirDeGaleria,
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.PhotoLibrary,
                )
            }

            CampoConIcono(
                label = "Nombre o Apodo",
                value = estado.nombre,
                onValueChange = onNombreCambiado,
                icon = Icons.Filled.Person,
            )

            if (!esEdicion) {
                CampoConIcono(
                    label = "Número de chapeta / arete (opcional)",
                    value = estado.codigo,
                    onValueChange = onCodigoCambiado,
                    icon = Icons.Filled.Numbers,
                    readOnly = estado.codigoSoloLectura,
                    trailingAction = if (estado.codigoSoloLectura) null else nfcAction,
                )
            }

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
                                onRazaCambiada(opcion)
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
                        onClick = { onSexoSeleccionado(opcion) },
                        modifier = Modifier.weight(1f),
                        habilitada = !estado.sexoBloqueado,
                    )
                }
            }
            if (estado.sexoBloqueado) {
                Text(
                    "Este animal es padre o madre de otros animales, también de los dados de baja, por eso su sexo no se puede cambiar.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (estado.etapasDisponibles.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    estado.etapasDisponibles.forEach { opcion ->
                        BovinaFilterChip(
                            texto = opcion.aEtiqueta(),
                            seleccionado = estado.etapa == opcion,
                            onClick = { onEtapaSeleccionada(opcion) },
                        )
                    }
                }
            }

            Text("Fecha de nacimiento", style = MaterialTheme.typography.labelLarge)
            BovinaSecondaryButton(
                texto = estado.fechaNacimiento?.let { formatoFecha.format(Date(it)) } ?: "Seleccionar fecha",
                onClick = { mostrarSelectorFecha = true },
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Filled.CalendarMonth,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = estado.fechaNacimientoEsEstimada,
                    onCheckedChange = onFechaEstimadaCambiada,
                )
                Text("Fecha estimada", style = MaterialTheme.typography.bodyMedium)
            }

            if (mostrarSelectorFecha) {
                DialogoFechaHastaHoy(
                    fecha = estado.fechaNacimiento ?: System.currentTimeMillis(),
                    onAceptar = onFechaNacimientoCambiada,
                    onCerrar = { mostrarSelectorFecha = false },
                )
            }

            Text("Propósito productivo", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Proposito.entries.forEach { opcion ->
                    TarjetaSeleccionable(
                        texto = opcion.aEtiqueta(),
                        seleccionada = estado.proposito == opcion,
                        onClick = { onPropositoSeleccionado(opcion) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            SelectorProgenitor(
                label = "Padre (opcional)",
                candidatos = estado.candidatosPadre,
                idSeleccionado = estado.idPadre,
                onSeleccionado = onPadreSeleccionado,
            )
            SelectorProgenitor(
                label = "Madre (opcional)",
                candidatos = estado.candidatosMadre,
                idSeleccionado = estado.idMadre,
                onSeleccionado = onMadreSeleccionada,
            )

            BovinaPrimaryButton(
                texto = if (esEdicion) "Guardar cambios" else "Guardar animal",
                onClick = onGuardar,
                enabled = estado.puedeGuardar && !estado.noEditable,
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Filled.CheckCircle,
            )
            TextButton(onClick = onCancelar, modifier = Modifier.fillMaxWidth()) {
                Text("Cancelar y volver")
            }
        }
    }

    estado.ofertaPesajesPendientes?.let { oferta ->
        AlertDialog(
            onDismissRequest = { onOfertaPesajesPendientesRespondida(false) },
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
                TextButton(onClick = { onOfertaPesajesPendientesRespondida(true) }) { Text("Sí") }
            },
            dismissButton = {
                TextButton(onClick = { onOfertaPesajesPendientesRespondida(false) }) { Text("No") }
            },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RegistrarAnimalPreview() {
    Prototipo1Theme {
        RegistrarAnimalContent(
            estado = RegistrarAnimalUiState(
                nombre = "Lucero",
                codigo = "CH-0123",
                raza = "Holstein",
                sexo = Sexo.entries.first(),
                aviso = "Esta chapeta aún no está asociada a ningún animal.",
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onCancelar = {},
            onTomarFoto = {},
            onElegirDeGaleria = {},
            onNfcClick = {},
            onNombreCambiado = {},
            onCodigoCambiado = {},
            onRazaCambiada = {},
            onSexoSeleccionado = {},
            onEtapaSeleccionada = {},
            onFechaNacimientoCambiada = {},
            onFechaEstimadaCambiada = {},
            onPropositoSeleccionado = {},
            onPadreSeleccionado = {},
            onMadreSeleccionada = {},
            onGuardar = {},
            onOfertaPesajesPendientesRespondida = {},
        )
    }
}

/** Elige el padre o la madre entre los candidatos, o ninguno. Los dados de baja también se pueden elegir. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorProgenitor(
    label: String,
    candidatos: List<Animal>,
    idSeleccionado: String?,
    onSeleccionado: (String?) -> Unit,
) {
    var expandido by remember { mutableStateOf(false) }
    val seleccionado = candidatos.firstOrNull { it.idAnimal == idSeleccionado }
    ExposedDropdownMenuBox(expanded = expandido, onExpandedChange = { expandido = it }) {
        CampoConIcono(
            label = label,
            value = seleccionado?.nombreConBaja() ?: "Sin registrar",
            onValueChange = {},
            icon = Icons.Filled.Pets,
            readOnly = true,
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
        )
        ExposedDropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            DropdownMenuItem(
                text = { Text("Sin registrar") },
                onClick = {
                    onSeleccionado(null)
                    expandido = false
                },
            )
            candidatos.forEach { candidato ->
                DropdownMenuItem(
                    text = { Text(candidato.nombreConBaja()) },
                    onClick = {
                        onSeleccionado(candidato.idAnimal)
                        expandido = false
                    },
                )
            }
        }
    }
}

@Composable
private fun TarjetaSeleccionable(
    texto: String,
    seleccionada: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    habilitada: Boolean = true,
) {
    val fondo = if (seleccionada) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest
    }
    Card(
        onClick = onClick,
        modifier = modifier,
        enabled = habilitada,
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(
            containerColor = fondo,
            // Deshabilitada conserva el fondo: la selección sigue viéndose, solo se atenúa.
            disabledContainerColor = fondo,
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
            color = if (habilitada) BovinaForest else BovinaForest.copy(alpha = 0.6f),
            fontWeight = if (seleccionada) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(16.dp),
        )
    }
}
