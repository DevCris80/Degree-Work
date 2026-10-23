package com.devcris80.prototipo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.createSavedStateHandle
import com.devcris80.prototipo.domain.model.Pesaje
import com.devcris80.prototipo.ui.components.BovinaPrimaryButton
import com.devcris80.prototipo.ui.components.BovinaSecondaryButton
import com.devcris80.prototipo.ui.components.BovinaTopBar
import com.devcris80.prototipo.ui.components.CampoConIcono
import com.devcris80.prototipo.ui.theme.BovinaScreenGutter
import com.devcris80.prototipo.ui.util.appViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Formulario del pesaje manual de un Animal (CU-16): peso, y fecha y hora de la medición. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PesajeFormScreen(
    onGuardado: () -> Unit,
    onVolver: () -> Unit,
) {
    val viewModel = appViewModel { container ->
        val idAnimal = createSavedStateHandle().get<String>("idAnimal").orEmpty()
        NuevoPesajeViewModel(idAnimal, container.registrarPesajeManual)
    }
    val estado by viewModel.estado.collectAsState()
    val formatoFecha = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val formatoHora = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    var mostrarSelectorFecha by remember { mutableStateOf(false) }
    var mostrarSelectorHora by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.eventos.collect { evento ->
            when (evento) {
                NuevoPesajeEvento.Guardado -> onGuardado()
            }
        }
    }

    Scaffold(
        topBar = { BovinaTopBar(titulo = "Registrar pesaje", onVolver = onVolver) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(BovinaScreenGutter),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CampoConIcono(
                label = "Peso (kg)",
                value = estado.peso,
                onValueChange = viewModel::onPesoCambiado,
                icon = Icons.Filled.MonitorWeight,
                keyboardType = KeyboardType.Decimal,
            )
            Text("Fecha y hora de la medición", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BovinaSecondaryButton(
                    texto = formatoFecha.format(Date(estado.fecha)),
                    onClick = { mostrarSelectorFecha = true },
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.CalendarMonth,
                )
                BovinaSecondaryButton(
                    texto = formatoHora.format(Date(estado.fecha)),
                    onClick = { mostrarSelectorHora = true },
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Schedule,
                )
            }
            estado.error?.let { error ->
                Text(
                    text = when (error) {
                        NuevoPesajeError.PesoFueraDeRango ->
                            "El peso debe ser mayor que 0 y de hasta ${Pesaje.PESO_MAXIMO_KG.toInt()} kg."
                        NuevoPesajeError.FechaFutura -> "La fecha y hora de la medición no pueden ser futuras."
                        NuevoPesajeError.AnimalNoDisponible -> "Este animal está dado de baja."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            BovinaPrimaryButton(
                texto = "Guardar",
                onClick = viewModel::onGuardar,
                enabled = estado.puedeGuardar,
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Filled.CheckCircle,
            )
        }
    }

    if (mostrarSelectorFecha) {
        val selectorState = rememberDatePickerState(
            initialSelectedDateMillis = diaEnUtc(estado.fecha),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcTimeMillis <= System.currentTimeMillis()
            },
        )
        DatePickerDialog(
            onDismissRequest = { mostrarSelectorFecha = false },
            confirmButton = {
                TextButton(onClick = {
                    selectorState.selectedDateMillis?.let { viewModel.onFechaCambiada(conDia(estado.fecha, it)) }
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

    if (mostrarSelectorHora) {
        val actual = Calendar.getInstance().apply { timeInMillis = estado.fecha }
        val selectorState = rememberTimePickerState(
            initialHour = actual.get(Calendar.HOUR_OF_DAY),
            initialMinute = actual.get(Calendar.MINUTE),
            is24Hour = true,
        )
        AlertDialog(
            onDismissRequest = { mostrarSelectorHora = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onFechaCambiada(conHora(estado.fecha, selectorState.hour, selectorState.minute))
                    mostrarSelectorHora = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarSelectorHora = false }) { Text("Cancelar") }
            },
            text = { TimePicker(state = selectorState) },
        )
    }
}

// El DatePicker trabaja con la medianoche UTC del día; la fecha del pesaje está en la hora local.
private fun diaEnUtc(fecha: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = fecha }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

private fun conDia(fecha: Long, diaUtc: Long): Long {
    val dia = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = diaUtc }
    return Calendar.getInstance().apply {
        timeInMillis = fecha
        set(dia.get(Calendar.YEAR), dia.get(Calendar.MONTH), dia.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

private fun conHora(fecha: Long, hora: Int, minuto: Int): Long = Calendar.getInstance().apply {
    timeInMillis = fecha
    set(Calendar.HOUR_OF_DAY, hora)
    set(Calendar.MINUTE, minuto)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis
