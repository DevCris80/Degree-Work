package com.devcris80.prototipo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.ui.components.FotoAnimal
import com.devcris80.prototipo.ui.components.PillBadge
import com.devcris80.prototipo.ui.theme.BovinaScreenGutter
import com.devcris80.prototipo.ui.util.aEtiqueta
import com.devcris80.prototipo.ui.util.appViewModel
import com.devcris80.prototipo.ui.util.formatearEdad
import java.util.concurrent.TimeUnit

private val FILTROS_PROPOSITO: List<Proposito?> = listOf(null) + Proposito.entries

@Composable
fun ListaAnimalesScreen(
    onAnimalClick: (idAnimal: String) -> Unit,
    onNuevoAnimalClick: () -> Unit,
    onPesajesPendientesClick: () -> Unit,
) {
    val viewModel = appViewModel { container ->
        ListaAnimalesViewModel(
            container.cuentaRepository,
            container.animalRepository,
            container.chapetaRepository,
            container.registroRepository,
            container.observarPesajesPendientes,
        )
    }
    val estado by viewModel.estado.collectAsState()

    var busqueda by remember { mutableStateOf("") }

    val listaFiltrada = estado.animales.filter { item ->
        busqueda.isBlank() ||
            item.animal.nombre.contains(busqueda, ignoreCase = true) ||
            item.codigoChapeta?.contains(busqueda, ignoreCase = true) == true
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onNuevoAnimalClick) {
                Icon(Icons.Filled.Add, contentDescription = "Registrar animal")
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = BovinaScreenGutter),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = BovinaScreenGutter),
        ) {
            item {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Home,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "  FINCA ${estado.nombreFinca}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(text = "Animales", style = MaterialTheme.typography.headlineLarge)
                }
            }
            if (estado.pesajesPendientes > 0) {
                item {
                    Card(
                        onClick = onPesajesPendientesClick,
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                                Text(
                                    text = "${estado.pesajesPendientes} pesajes pendientes",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            )
                        }
                    }
                }
            }
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column {
                                Text(
                                    text = "Buenos días, ${estado.nombreUsuario}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Text(
                                    text = estado.nombreFinca,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                            PillBadge(
                                texto = "${estado.animales.size} en finca",
                                containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                contentColor = MaterialTheme.colorScheme.primaryContainer,
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 10.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = "Todos los datos sincronizados sin internet",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                }
            }
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = busqueda,
                        onValueChange = { busqueda = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Buscar por chapeta o nombre...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                    )
                    IconButton(onClick = {}) {
                        Icon(Icons.Filled.FilterList, contentDescription = "Filtros")
                    }
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(FILTROS_PROPOSITO) { proposito ->
                        FilterChip(
                            selected = estado.filtro == proposito,
                            onClick = { viewModel.onFiltroSeleccionado(proposito) },
                            label = { Text(proposito?.aEtiqueta() ?: "Todos") },
                        )
                    }
                }
            }
            items(listaFiltrada, key = { it.animal.idAnimal }) { item ->
                TarjetaAnimal(
                    item = item,
                    onClick = { onAnimalClick(item.animal.idAnimal) },
                )
            }
        }
    }
}

@Composable
private fun TarjetaAnimal(
    item: AnimalConResumen,
    onClick: () -> Unit,
) {
    val animal = item.animal
    val registro = item.ultimoRegistro

    Card(onClick = onClick, shape = RoundedCornerShape(16.dp)) {
        Row(modifier = Modifier.padding(12.dp)) {
            FotoAnimal(fotoUri = animal.fotoUri, modifier = Modifier.size(72.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                PillBadge(texto = animal.proposito.aEtiqueta())
                Text(
                    text = animal.nombre,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    text = "${animal.raza} · ${animal.etapa.aEtiqueta()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Edad: ${formatearEdad(animal.fechaNacimiento)}" +
                        if (animal.fechaNacimientoEsEstimada) " (estimada)" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = registro?.let { "Último peso: ${it.peso} kg" } ?: "Sin pesajes registrados",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = registro?.let { "Pesaje: hace ${diasDesde(it.timestamp)} días" }.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(contentAlignment = Alignment.BottomEnd) {
                Text(
                    text = "Ficha →",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private fun diasDesde(timestamp: Long): Long =
    TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - timestamp).coerceAtLeast(0)
