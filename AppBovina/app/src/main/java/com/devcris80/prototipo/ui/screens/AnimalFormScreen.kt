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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devcris80.prototipo.data.Animal
import com.devcris80.prototipo.data.PERFIL_FINCA_ID
import com.devcris80.prototipo.ui.components.BannerInformativo
import com.devcris80.prototipo.ui.components.CampoConIcono
import com.devcris80.prototipo.ui.components.FotoAnimal
import com.devcris80.prototipo.ui.theme.BovinaMinFieldHeight
import com.devcris80.prototipo.ui.theme.BovinaScreenGutter
import com.devcris80.prototipo.ui.util.crearArchivoFotoUri
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private val RAZAS = listOf(
    "Holstein", "Jersey", "Normando", "Pardo Suizo", "Brahman", "Cebú",
    "Angus", "Simmental", "Criollo", "Otra",
)
private val ETAPAS_MACHO = listOf("Toro", "Novillo")
private val ETAPAS_HEMBRA = listOf("Vaca", "Ternera")
private val PROPOSITOS = listOf("Leche", "Carne", "Doble Propósito")


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrarAnimalScreen(
    onGuardar: (animal: Animal, idChip: String?) -> Unit,
    onCancelar: () -> Unit,
    codigoPrecargado: String? = null,
    avisoAnterior: String? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var fotoUri by remember { mutableStateOf<String?>(null) }
    var fotoTempUri by remember { mutableStateOf<Uri?>(null) }
    var nombre by remember { mutableStateOf("") }
    var idChip by remember { mutableStateOf(codigoPrecargado.orEmpty()) }
    var raza by remember { mutableStateOf("") }
    var razaExpandida by remember { mutableStateOf(false) }
    var sexo by remember { mutableStateOf("") }
    var etapa by remember { mutableStateOf("") }
    var fechaNacimiento by remember { mutableStateOf<Long?>(null) }
    var fechaNacimientoEsEstimada by remember { mutableStateOf(false) }
    var mostrarSelectorFecha by remember { mutableStateOf(false) }
    var proposito by remember { mutableStateOf("") }
    val formatoFecha = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

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

    val tomarFotoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { exito ->
        if (exito) fotoUri = fotoTempUri?.toString()
    }
    val elegirDeGaleriaLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri -> if (uri != null) fotoUri = uri.toString() }

    val etapasDisponibles = when (sexo) {
        "Macho" -> ETAPAS_MACHO
        "Hembra" -> ETAPAS_HEMBRA
        else -> emptyList()
    }

    val puedeGuardar = nombre.isNotBlank() && raza.isNotBlank() &&
        sexo.isNotBlank() && etapa.isNotBlank() && fechaNacimiento != null && proposito.isNotBlank()

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
            avisoAnterior?.let { aviso ->
                BannerInformativo(texto = aviso, icon = Icons.Filled.Info)
            }

            FotoAnimal(
                fotoUri = fotoUri,
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
                value = nombre,
                onValueChange = { nombre = it },
                icon = Icons.Filled.Person,
            )

            CampoConIcono(
                label = "Número de chapeta / arete (opcional)",
                value = idChip,
                onValueChange = { idChip = it },
                icon = Icons.Filled.Numbers,
                readOnly = codigoPrecargado != null,
                trailingAction = if (codigoPrecargado != null) null else nfcAction,
            )

            ExposedDropdownMenuBox(
                expanded = razaExpandida,
                onExpandedChange = { razaExpandida = it },
            ) {
                CampoConIcono(
                    label = "Raza principal",
                    value = raza,
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
                                raza = opcion
                                razaExpandida = false
                            },
                        )
                    }
                }
            }

            Text("Sexo del ejemplar", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("Macho", "Hembra").forEach { opcion ->
                    TarjetaSeleccionable(
                        texto = opcion,
                        seleccionada = sexo == opcion,
                        onClick = {
                            sexo = opcion
                            etapa = ""
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (etapasDisponibles.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    etapasDisponibles.forEach { opcion ->
                        FilterChip(
                            selected = etapa == opcion,
                            onClick = { etapa = opcion },
                            label = { Text(opcion) },
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
                    text = fechaNacimiento?.let { formatoFecha.format(Date(it)) } ?: "Seleccionar fecha",
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = fechaNacimientoEsEstimada,
                    onCheckedChange = { fechaNacimientoEsEstimada = it },
                )
                Text("Fecha estimada", style = MaterialTheme.typography.bodyMedium)
            }

            if (mostrarSelectorFecha) {
                val selectorState = rememberDatePickerState(
                    initialSelectedDateMillis = fechaNacimiento ?: System.currentTimeMillis(),
                    selectableDates = object : SelectableDates {
                        override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                            utcTimeMillis <= System.currentTimeMillis()
                    },
                )
                DatePickerDialog(
                    onDismissRequest = { mostrarSelectorFecha = false },
                    confirmButton = {
                        TextButton(onClick = {
                            fechaNacimiento = selectorState.selectedDateMillis
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
                PROPOSITOS.forEach { opcion ->
                    TarjetaSeleccionable(
                        texto = opcion,
                        seleccionada = proposito == opcion,
                        onClick = { proposito = opcion },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Button(
                enabled = puedeGuardar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BovinaMinFieldHeight),
                shape = RoundedCornerShape(12.dp),
                onClick = {
                    val fecha = fechaNacimiento ?: return@Button
                    onGuardar(
                        Animal(
                            idAnimal = UUID.randomUUID().toString(),
                            idPerfilFinca = PERFIL_FINCA_ID,
                            nombre = nombre,
                            raza = raza,
                            sexo = sexo,
                            etapa = etapa,
                            fechaNacimiento = fecha,
                            fechaNacimientoEsEstimada = fechaNacimientoEsEstimada,
                            proposito = proposito,
                            fotoUri = fotoUri,
                        ),
                        idChip.takeIf { it.isNotBlank() },
                    )
                },
            ) {
                Text("Guardar animal", style = MaterialTheme.typography.labelLarge)
            }
            TextButton(onClick = onCancelar, modifier = Modifier.fillMaxWidth()) {
                Text("Cancelar y volver")
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

