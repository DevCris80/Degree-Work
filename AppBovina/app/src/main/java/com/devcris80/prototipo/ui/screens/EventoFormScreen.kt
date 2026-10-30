package com.devcris80.prototipo.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.createSavedStateHandle
import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Sexo
import com.devcris80.prototipo.domain.model.TipoEvento
import com.devcris80.prototipo.ui.components.BannerInformativo
import com.devcris80.prototipo.ui.components.BovinaCard
import com.devcris80.prototipo.ui.components.BovinaPrimaryButton
import com.devcris80.prototipo.ui.components.BovinaSecondaryButton
import com.devcris80.prototipo.ui.components.BovinaTextField
import com.devcris80.prototipo.ui.components.BovinaTopBar
import com.devcris80.prototipo.ui.components.CampoSelector
import com.devcris80.prototipo.ui.components.DialogoFecha
import com.devcris80.prototipo.ui.theme.BovinaForest
import com.devcris80.prototipo.ui.theme.BovinaScreenGutter
import com.devcris80.prototipo.ui.theme.Prototipo1Theme
import com.devcris80.prototipo.ui.util.appViewModel
import com.devcris80.prototipo.ui.util.conHora
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Registrar evento. Con [onRegistrarCria] la persona abre Registrar animal con la madre precargada para dar de alta
 * la cría de un parto; al volver, la cría ya aparece entre las que se pueden elegir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventoFormScreen(
    onGuardado: () -> Unit,
    onVolver: () -> Unit,
    onRegistrarCria: (idMadre: String) -> Unit,
) {
    val viewModel = appViewModel { container ->
        val idAnimal = createSavedStateHandle().get<String>("idAnimal")
        NuevoEventoViewModel(idAnimal, container.animalRepository, container.registrarEvento)
    }
    val estado by viewModel.estado.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.eventos.collect { evento ->
            when (evento) {
                NuevoEventoEvento.Guardado -> onGuardado()
            }
        }
    }

    var mostrarSelectorFecha by remember { mutableStateOf(false) }
    var mostrarSelectorHora by remember { mutableStateOf(false) }

    EventoFormContent(
        estado = estado,
        onVolver = onVolver,
        onTipoSeleccionado = viewModel::onTipoSeleccionado,
        onTituloCambiado = viewModel::onTituloCambiado,
        onFechaClick = { mostrarSelectorFecha = true },
        onHoraClick = { mostrarSelectorHora = true },
        onAnimalSeleccionado = viewModel::onAnimalSeleccionado,
        onBiologicoCambiado = viewModel::onBiologicoCambiado,
        onDosisCambiada = viewModel::onDosisCambiada,
        onViaSeleccionada = viewModel::onViaSeleccionada,
        onCriaSeleccionada = viewModel::onCriaSeleccionada,
        onRegistrarCria = onRegistrarCria,
        onGuardar = viewModel::onGuardar,
    )

    if (mostrarSelectorFecha) {
        DialogoFecha(
            fecha = estado.fecha,
            onAceptar = viewModel::onFechaCambiada,
            onCerrar = { mostrarSelectorFecha = false },
        )
    }

    if (mostrarSelectorHora) {
        DialogoHora(
            fecha = estado.fecha,
            onAceptar = viewModel::onFechaCambiada,
            onCerrar = { mostrarSelectorHora = false },
        )
    }
}

/** Elige la hora de [fecha] sin cambiar su día. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DialogoHora(fecha: Long, onAceptar: (Long) -> Unit, onCerrar: () -> Unit) {
    val actual = Calendar.getInstance().apply { timeInMillis = fecha }
    val selectorState = rememberTimePickerState(
        initialHour = actual.get(Calendar.HOUR_OF_DAY),
        initialMinute = actual.get(Calendar.MINUTE),
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onCerrar,
        confirmButton = {
            TextButton(onClick = {
                onAceptar(conHora(fecha, selectorState.hour, selectorState.minute))
                onCerrar()
            }) { Text("Aceptar") }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) { Text("Cancelar") }
        },
        text = { TimePicker(state = selectorState) },
    )
}

internal val formatoDia = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
internal val formatoHora = SimpleDateFormat("HH:mm", Locale.getDefault())

@Composable
private fun EventoFormContent(
    estado: NuevoEventoUiState,
    onVolver: () -> Unit,
    onTipoSeleccionado: (TipoEvento) -> Unit,
    onTituloCambiado: (String) -> Unit,
    onFechaClick: () -> Unit,
    onHoraClick: () -> Unit,
    onAnimalSeleccionado: (String) -> Unit,
    onBiologicoCambiado: (String) -> Unit,
    onDosisCambiada: (String) -> Unit,
    onViaSeleccionada: (String) -> Unit,
    onCriaSeleccionada: (String?) -> Unit,
    onRegistrarCria: (idMadre: String) -> Unit,
    onGuardar: () -> Unit,
) {
    Scaffold(
        topBar = { BovinaTopBar(titulo = "Registrar evento", onVolver = onVolver) },
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
                texto = if (estado.programado) {
                    "La fecha todavía no llega: el evento queda como programado hasta entonces."
                } else {
                    "Registre lo que ya ocurrió, o ponga una fecha futura para dejarlo programado."
                },
                icon = Icons.Filled.EventAvailable,
            )

            Text(
                text = "Tipo de evento",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 4.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TipoEvento.entries.forEach { tipo ->
                    TarjetaTipoEvento(
                        tipo = tipo,
                        seleccionado = estado.tipo == tipo,
                        onClick = { onTipoSeleccionado(tipo) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            BovinaCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CampoConEtiqueta("Título o nombre del evento") {
                        BovinaTextField(
                            value = estado.titulo,
                            onValueChange = onTituloCambiado,
                            placeholder = "Ej: Vacunación del ciclo I",
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CampoConEtiqueta("Fecha del evento", Modifier.weight(1f)) {
                            CampoSelector(
                                valor = formatoDia.format(Date(estado.fecha)),
                                marcador = "dd/mm/aaaa",
                                onClick = onFechaClick,
                                leadingIcon = Icons.Filled.CalendarMonth,
                                trailingIcon = Icons.Filled.CalendarMonth,
                            )
                        }
                        CampoConEtiqueta("Hora", Modifier.weight(1f)) {
                            CampoSelector(
                                valor = formatoHora.format(Date(estado.fecha)),
                                marcador = "hh:mm",
                                onClick = onHoraClick,
                                leadingIcon = Icons.Filled.AccessTime,
                                trailingIcon = Icons.Filled.AccessTime,
                            )
                        }
                    }
                    CampoConEtiqueta("Aplicar a") {
                        SelectorDeLista(
                            valor = estado.animal?.nombre,
                            marcador = "Seleccionar animal",
                            opciones = estado.animales.map { it.nombre to it.idAnimal },
                            onElegida = onAnimalSeleccionado,
                            leadingIcon = Icons.Filled.Pets,
                        )
                    }
                    when (estado.tipo) {
                        TipoEvento.VACUNACION -> DetallesBiologico(
                            estado = estado,
                            onBiologicoCambiado = onBiologicoCambiado,
                            onDosisCambiada = onDosisCambiada,
                            onViaSeleccionada = onViaSeleccionada,
                        )
                        TipoEvento.PARTO -> DetallesParto(
                            estado = estado,
                            onCriaSeleccionada = onCriaSeleccionada,
                            onRegistrarCria = onRegistrarCria,
                        )
                    }
                }
            }

            estado.error?.let { error ->
                Text(
                    text = when (error) {
                        NuevoEventoError.AnimalNoDisponible -> "Este animal está dado de baja o ya no existe."
                        NuevoEventoError.CriaNoValida -> "La cría elegida ya no está disponible."
                        NuevoEventoError.CriaConOtroProgenitor ->
                            "Esa cría ya tiene otra madre o padre registrado. Corríjalo editando a la cría."
                        NuevoEventoError.FaltanDatos -> "Falta el título del evento o el biológico."
                        NuevoEventoError.SinUsuario -> "No hay una sesión iniciada en este celular."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            BovinaPrimaryButton(
                texto = "Guardar",
                onClick = onGuardar,
                enabled = estado.puedeGuardar,
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Filled.CheckCircle,
            )
            TextButton(onClick = onVolver, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Descartar y volver",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun CampoConEtiqueta(
    etiqueta: String,
    modifier: Modifier = Modifier,
    contenido: @Composable () -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(start = 4.dp),
        )
        contenido()
    }
}

/** Un [CampoSelector] que despliega las [opciones] (texto y valor) y devuelve el valor de la elegida. */
@Composable
internal fun SelectorDeLista(
    valor: String?,
    marcador: String,
    opciones: List<Pair<String, String>>,
    onElegida: (String) -> Unit,
    leadingIcon: ImageVector? = null,
) {
    var abierto by remember { mutableStateOf(false) }
    Box {
        CampoSelector(valor = valor, marcador = marcador, onClick = { abierto = true }, leadingIcon = leadingIcon)
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            opciones.forEach { (texto, id) ->
                DropdownMenuItem(
                    text = { Text(texto) },
                    onClick = {
                        onElegida(id)
                        abierto = false
                    },
                )
            }
        }
    }
}

@Composable
internal fun CuadroDeTipo(titulo: String, icono: ImageVector, contenido: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Text(text = titulo, style = MaterialTheme.typography.titleSmall)
        }
        contenido()
    }
}

@Composable
private fun DetallesBiologico(
    estado: NuevoEventoUiState,
    onBiologicoCambiado: (String) -> Unit,
    onDosisCambiada: (String) -> Unit,
    onViaSeleccionada: (String) -> Unit,
) = CamposDeVacunacion(
    biologico = estado.biologico,
    dosis = estado.dosis,
    via = estado.via,
    onBiologicoCambiado = onBiologicoCambiado,
    onDosisCambiada = onDosisCambiada,
    onViaSeleccionada = onViaSeleccionada,
)

@Composable
internal fun CamposDeVacunacion(
    biologico: String,
    dosis: String,
    via: String?,
    onBiologicoCambiado: (String) -> Unit,
    onDosisCambiada: (String) -> Unit,
    onViaSeleccionada: (String) -> Unit,
) {
    CuadroDeTipo(titulo = "Detalles del biológico y dosis", icono = Icons.Filled.Vaccines) {
        CampoConEtiqueta("Biológico o medicamento") {
            BovinaTextField(
                value = biologico,
                onValueChange = onBiologicoCambiado,
                placeholder = "Nombre del biológico",
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CampoConEtiqueta("Dosis por cabeza", Modifier.weight(1f)) {
                BovinaTextField(
                    value = dosis,
                    onValueChange = onDosisCambiada,
                    placeholder = "Ej: 2 ml",
                )
            }
            CampoConEtiqueta("Vía de aplicación", Modifier.weight(1f)) {
                SelectorDeLista(
                    valor = via,
                    marcador = "Seleccionar",
                    opciones = VIAS_DE_APLICACION.map { it to it },
                    onElegida = onViaSeleccionada,
                )
            }
        }
    }
}

@Composable
private fun DetallesParto(
    estado: NuevoEventoUiState,
    onCriaSeleccionada: (String?) -> Unit,
    onRegistrarCria: (idMadre: String) -> Unit,
) {
    CuadroDeTipo(titulo = "Cría resultante", icono = Icons.Filled.Pets) {
        CampoConEtiqueta("Cría (opcional)") {
            SelectorDeLista(
                valor = estado.candidatasCria.firstOrNull { it.idAnimal == estado.idCria }?.nombre,
                marcador = "Seleccionar la cría",
                opciones = estado.candidatasCria.map { it.nombre to it.idAnimal },
                onElegida = onCriaSeleccionada,
            )
        }
        val madre: Animal? = estado.animal?.takeIf { it.sexo == Sexo.HEMBRA }
        if (madre != null) {
            BovinaSecondaryButton(
                texto = "Registrar cría nueva",
                onClick = { onRegistrarCria(madre.idAnimal) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private val TipoEvento.descripcion: String
    get() = when (this) {
        TipoEvento.VACUNACION -> "Ciclos sanitarios y vacunas"
        TipoEvento.PARTO -> "Parto y seguimiento posparto"
    }

private val TipoEvento.icono: ImageVector
    get() = when (this) {
        TipoEvento.VACUNACION -> Icons.Filled.Vaccines
        TipoEvento.PARTO -> Icons.Filled.Pets
    }

@Composable
private fun TarjetaTipoEvento(
    tipo: TipoEvento,
    seleccionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (seleccionado) {
                MaterialTheme.colorScheme.surfaceContainerLowest
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
        border = if (seleccionado) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        },
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (seleccionado) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = tipo.icono,
                        contentDescription = null,
                        tint = if (seleccionado) MaterialTheme.colorScheme.onPrimary else BovinaForest,
                    )
                }
                if (seleccionado) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Seleccionado",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Text(text = tipo.nombre, style = MaterialTheme.typography.headlineMedium)
            Text(
                text = tipo.descripcion,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun EventoVacunacionPreview() {
    Prototipo1Theme {
        EventoFormContent(
            estado = NuevoEventoUiState(
                titulo = "Vacunación del ciclo I",
                fecha = 1_800_000_000_000L,
                ahora = 1_700_000_000_000L,
                biologico = "Aftosa bivalente",
                dosis = "2 ml",
                via = "Subcutánea",
            ),
            onVolver = {},
            onTipoSeleccionado = {},
            onTituloCambiado = {},
            onFechaClick = {},
            onHoraClick = {},
            onAnimalSeleccionado = {},
            onBiologicoCambiado = {},
            onDosisCambiada = {},
            onViaSeleccionada = {},
            onCriaSeleccionada = {},
            onRegistrarCria = {},
            onGuardar = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun EventoPartoPreview() {
    Prototipo1Theme {
        EventoFormContent(
            estado = NuevoEventoUiState(
                tipo = TipoEvento.PARTO,
                fecha = 1_700_000_000_000L,
                ahora = 1_700_000_000_000L,
            ),
            onVolver = {},
            onTipoSeleccionado = {},
            onTituloCambiado = {},
            onFechaClick = {},
            onHoraClick = {},
            onAnimalSeleccionado = {},
            onBiologicoCambiado = {},
            onDosisCambiada = {},
            onViaSeleccionada = {},
            onCriaSeleccionada = {},
            onRegistrarCria = {},
            onGuardar = {},
        )
    }
}
