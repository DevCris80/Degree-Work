package com.devcris80.prototipo.ui.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import com.devcris80.prototipo.ui.util.conDia
import com.devcris80.prototipo.ui.util.diaEnUtc

/**
 * Elige un día. Recibe y devuelve fechas en la hora local del celular, y conserva la hora de [fecha]. Por defecto
 * deja elegir cualquier día, pasado o futuro; [diasElegibles] lo limita.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoFecha(
    fecha: Long,
    onAceptar: (fecha: Long) -> Unit,
    onCerrar: () -> Unit,
    diasElegibles: SelectableDates = DatePickerDefaults.AllDates,
) {
    val selectorState = rememberDatePickerState(
        initialSelectedDateMillis = diaEnUtc(fecha),
        selectableDates = diasElegibles,
    )
    DatePickerDialog(
        onDismissRequest = onCerrar,
        confirmButton = {
            TextButton(onClick = {
                selectorState.selectedDateMillis?.let { onAceptar(conDia(fecha, it)) }
                onCerrar()
            }) { Text("Aceptar") }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) { Text("Cancelar") }
        },
    ) {
        DatePicker(state = selectorState)
    }
}

/** Elige un día de hoy hacia atrás. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoFechaHastaHoy(
    fecha: Long,
    onAceptar: (fecha: Long) -> Unit,
    onCerrar: () -> Unit,
) {
    DialogoFecha(
        fecha = fecha,
        onAceptar = onAceptar,
        onCerrar = onCerrar,
        diasElegibles = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                utcTimeMillis <= diaEnUtc(System.currentTimeMillis())
        },
    )
}
