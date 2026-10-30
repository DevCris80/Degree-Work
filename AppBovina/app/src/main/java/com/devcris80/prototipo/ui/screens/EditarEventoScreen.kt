package com.devcris80.prototipo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.createSavedStateHandle
import com.devcris80.prototipo.ui.components.BannerInformativo
import com.devcris80.prototipo.ui.components.BovinaCard
import com.devcris80.prototipo.ui.components.BovinaPrimaryButton
import com.devcris80.prototipo.ui.components.BovinaSecondaryButton
import com.devcris80.prototipo.ui.components.BovinaTextField
import com.devcris80.prototipo.ui.components.BovinaTopBar
import com.devcris80.prototipo.ui.components.CampoSelector
import com.devcris80.prototipo.ui.components.DialogoFecha
import com.devcris80.prototipo.ui.theme.BovinaScreenGutter
import com.devcris80.prototipo.ui.util.appViewModel
import java.util.Date

/**
 * Editar un evento del historial (CU-18) y, para el Ganadero, darlo de baja (CU-19). El tipo, el animal y la cría de
 * un parto se ven pero no se cambian. La baja pide confirmación explicando el efecto (RNF-16).
 */
@Composable
fun EditarEventoScreen(
    onTerminado: () -> Unit,
    onVolver: () -> Unit,
) {
    val viewModel = appViewModel { container ->
        val idEvento = createSavedStateHandle().get<String>("idEvento").orEmpty()
        EditarEventoViewModel(
            idEvento,
            container.eventoRepository,
            container.animalRepository,
            container.cuentaRepository,
            container.actualizarEvento,
            container.darDeBajaEvento,
        )
    }
    val estado by viewModel.estado.collectAsState()
    var confirmarBaja by remember { mutableStateOf(false) }
    var mostrarSelectorFecha by remember { mutableStateOf(false) }
    var mostrarSelectorHora by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.eventos.collect { evento ->
            when (evento) {
                EditarEventoEvento.Guardado, EditarEventoEvento.DadoDeBaja -> onTerminado()
            }
        }
    }

    Scaffold(
        topBar = { BovinaTopBar(titulo = "Editar evento", onVolver = onVolver) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(BovinaScreenGutter),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (estado.cargado) {
                BannerInformativo(
                    texto = if (estado.programado) {
                        "La fecha todavía no llega: el evento queda como programado hasta entonces."
                    } else {
                        "Corrija lo que quedó mal. El tipo y el animal no se cambian."
                    },
                    icon = Icons.Filled.EventAvailable,
                )
                BovinaCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        CampoConEtiqueta("Tipo de evento") {
                            Text(text = estado.tipo, style = MaterialTheme.typography.bodyLarge)
                        }
                        estado.nombreAnimal?.let { nombre ->
                            CampoConEtiqueta("Aplicado a") {
                                Text(text = nombre, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                        CampoConEtiqueta("Título o nombre del evento") {
                            BovinaTextField(
                                value = estado.titulo,
                                onValueChange = viewModel::onTituloCambiado,
                                placeholder = "Ej: Vacunación del ciclo I",
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CampoConEtiqueta("Fecha del evento", Modifier.weight(1f)) {
                                CampoSelector(
                                    valor = formatoDia.format(Date(estado.fecha)),
                                    marcador = "dd/mm/aaaa",
                                    onClick = { mostrarSelectorFecha = true },
                                    leadingIcon = Icons.Filled.CalendarMonth,
                                    trailingIcon = Icons.Filled.CalendarMonth,
                                )
                            }
                            CampoConEtiqueta("Hora", Modifier.weight(1f)) {
                                CampoSelector(
                                    valor = formatoHora.format(Date(estado.fecha)),
                                    marcador = "hh:mm",
                                    onClick = { mostrarSelectorHora = true },
                                    leadingIcon = Icons.Filled.AccessTime,
                                    trailingIcon = Icons.Filled.AccessTime,
                                )
                            }
                        }
                        if (estado.esVacunacion) {
                            CamposDeVacunacion(
                                biologico = estado.biologico,
                                dosis = estado.dosis,
                                via = estado.via,
                                onBiologicoCambiado = viewModel::onBiologicoCambiado,
                                onDosisCambiada = viewModel::onDosisCambiada,
                                onViaSeleccionada = viewModel::onViaSeleccionada,
                            )
                        }
                        estado.nombreCria?.let { cria ->
                            CuadroDeTipo(titulo = "Cría resultante", icono = Icons.Filled.Pets) {
                                Text(text = cria, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }

            estado.error?.let { error ->
                Text(
                    text = when (error) {
                        EditarEventoError.FaltanDatos -> "Falta el título del evento o el biológico."
                        EditarEventoError.EventoNoDisponible -> "Este evento ya no está disponible."
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
            if (estado.puedeDarDeBaja && estado.cargado) {
                BovinaSecondaryButton(
                    texto = "Dar de baja este evento",
                    onClick = { confirmarBaja = true },
                    enabled = !estado.guardando,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            TextButton(onClick = onVolver, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Descartar y volver",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

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
    if (confirmarBaja) {
        AlertDialog(
            onDismissRequest = { confirmarBaja = false },
            title = { Text("¿Dar de baja este evento?") },
            text = {
                Text(
                    "El evento dejará de aparecer en el historial del animal, en la pestaña Eventos y en los " +
                        "indicadores. Se conserva en la base de datos, pero la baja no se puede deshacer.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmarBaja = false
                    viewModel.onDarDeBaja()
                }) { Text("Dar de baja") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarBaja = false }) { Text("Cancelar") }
            },
        )
    }
}
