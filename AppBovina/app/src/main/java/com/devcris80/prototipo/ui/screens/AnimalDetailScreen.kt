package com.devcris80.prototipo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devcris80.prototipo.data.Animal
import com.devcris80.prototipo.data.Chapeta
import com.devcris80.prototipo.data.Evento
import com.devcris80.prototipo.data.Registro
import com.devcris80.prototipo.ui.components.FotoAnimal
import com.devcris80.prototipo.ui.components.PillBadge
import com.devcris80.prototipo.ui.theme.BovinaScreenGutter
import com.devcris80.prototipo.ui.util.formatearEdad
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleAnimalScreen(
    animal: Animal,
    eventos: Flow<List<Evento>>,
    registros: Flow<List<Registro>>,
    onVolver: () -> Unit,
    onNuevoEventoClick: () -> Unit,
    chapetaActiva: Flow<Chapeta?>,
    onDarDeBaja: () -> Unit,
    onLiberarChapeta: (idChapeta: String) -> Unit,
    onEditarClick: () -> Unit = {},
) {
    val listaEventos by eventos.collectAsState(initial = emptyList())
    val listaRegistros by registros.collectAsState(initial = emptyList())
    val chapeta by chapetaActiva.collectAsState(initial = null)
    val formatoFecha = remember(animal.idAnimal) { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    var tabSeleccionada by remember { mutableIntStateOf(0) }
    var confirmarBaja by remember { mutableStateOf(false) }
    var confirmarLiberacion by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle Animal") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = onEditarClick) {
                        Icon(Icons.Filled.Edit, contentDescription = "Editar")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = BovinaScreenGutter),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = BovinaScreenGutter),
        ) {
            item {
                Box {
                    FotoAnimal(
                        fotoUri = animal.fotoUri,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp),
                    ) {
                        PillBadge(texto = animal.idAnimal.take(6).uppercase())
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = animal.nombre, style = MaterialTheme.typography.headlineMedium)
                    PillBadge(texto = animal.proposito)
                }
            }
            item {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        FilaDato("Raza", animal.raza)
                        FilaDato("Sexo", animal.sexo)
                        FilaDato("Etapa", animal.etapa)
                        FilaDato(
                            "Edad",
                            formatearEdad(animal.fechaNacimiento) +
                                if (animal.fechaNacimientoEsEstimada) " (estimada)" else "",
                        )
                        FilaDato("Destino", animal.proposito)
                    }
                }
            }
            item {
                val ultimoPeso = listaRegistros.firstOrNull()
                val pesoAnterior = listaRegistros.getOrNull(1)
                val delta = if (ultimoPeso != null && pesoAnterior != null) {
                    ultimoPeso.peso - pesoAnterior.peso
                } else {
                    null
                }
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Último peso registrado",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = ultimoPeso?.let { "${it.peso} kg" } ?: "Sin pesajes registrados",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        if (delta != null) {
                            val signo = if (delta >= 0) "+" else ""
                            Text(
                                text = "$signo${"%.1f".format(delta)} kg desde el pesaje anterior",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            }
            item {
                TabRow(selectedTabIndex = tabSeleccionada) {
                    Tab(
                        selected = tabSeleccionada == 0,
                        onClick = { tabSeleccionada = 0 },
                        text = { Text("Pesajes (${listaRegistros.size})") },
                    )
                    Tab(
                        selected = tabSeleccionada == 1,
                        onClick = { tabSeleccionada = 1 },
                        text = { Text("Eventos (${listaEventos.size})") },
                    )
                }
            }
            if (tabSeleccionada == 0) {
                items(listaRegistros, key = { it.idRegistro }) { registro ->
                    val indice = listaRegistros.indexOf(registro)
                    val anterior = listaRegistros.getOrNull(indice + 1)
                    val delta = anterior?.let { registro.peso - it.peso }
                    Card(shape = RoundedCornerShape(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Text("${registro.peso} kg", style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    formatoFecha.format(Date(registro.timestamp)),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (delta != null) {
                                val signo = if (delta >= 0) "+" else ""
                                Text(
                                    "$signo${"%.1f".format(delta)} kg",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }
            } else {
                items(listaEventos, key = { it.idEvento }) { evento ->
                    Card(shape = RoundedCornerShape(12.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(evento.tipoEvento, style = MaterialTheme.typography.bodyLarge)
                            Text(evento.detalle, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                formatoFecha.format(Date(evento.fecha)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = onNuevoEventoClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text(" Registrar Pesaje o Evento")
                }
            }
            item {
                chapeta?.let { chapetaActual ->
                    OutlinedButton(
                        onClick = { confirmarLiberacion = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Liberar chapeta (${chapetaActual.codigo})")
                    }
                }
            }
            item {
                val fechaBaja = animal.fechaBaja
                if (fechaBaja != null) {
                    Text(
                        text = "Dado de baja el ${formatoFecha.format(Date(fechaBaja))}. Su historial se conserva.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    OutlinedButton(
                        onClick = { confirmarBaja = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text("Dar de baja")
                    }
                }
            }
        }
    }

    chapeta?.let { chapetaActual ->
        if (confirmarLiberacion) {
            AlertDialog(
                onDismissRequest = { confirmarLiberacion = false },
                title = { Text("¿Liberar la chapeta de ${animal.nombre}?") },
                text = {
                    Text("La chapeta ${chapetaActual.codigo} dejará de identificar a este animal. Sus pesajes anteriores se conservan.")
                },
                confirmButton = {
                    TextButton(onClick = {
                        confirmarLiberacion = false
                        onLiberarChapeta(chapetaActual.idChapeta)
                    }) { Text("Liberar") }
                },
                dismissButton = {
                    TextButton(onClick = { confirmarLiberacion = false }) { Text("Cancelar") }
                },
            )
        }
    }

    if (confirmarBaja) {
        AlertDialog(
            onDismissRequest = { confirmarBaja = false },
            title = { Text("¿Dar de baja a ${animal.nombre}?") },
            text = {
                Text("El animal dejará de aparecer en la lista. Su historial de pesajes y eventos se conserva.")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmarBaja = false
                    onDarDeBaja()
                }) { Text("Dar de baja") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarBaja = false }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun FilaDato(etiqueta: String, valor: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.bodyLarge)
    }
}
