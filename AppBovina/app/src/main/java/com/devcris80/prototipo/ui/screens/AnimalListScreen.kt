package com.devcris80.prototipo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.ui.components.BovinaCard
import com.devcris80.prototipo.ui.components.BovinaFilterChip
import com.devcris80.prototipo.ui.components.BovinaTextField
import com.devcris80.prototipo.ui.components.BovinaTopBar
import com.devcris80.prototipo.ui.components.FotoAnimal
import com.devcris80.prototipo.ui.components.PillBadge
import com.devcris80.prototipo.ui.theme.BovinaForest
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
    onIndicadoresClick: () -> Unit,
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
        topBar = {
            BovinaTopBar(titulo = "Animales", mostrarLogo = true) {
                IconButton(onClick = onIndicadoresClick) {
                    Icon(Icons.Filled.Insights, contentDescription = "Indicadores")
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNuevoAnimalClick,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(
                    text = "  Registrar",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = BovinaScreenGutter),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 96.dp, top = 4.dp),
        ) {
            if (estado.pesajesPendientes > 0) {
                item {
                    BovinaCard(
                        onClick = onPesajesPendientesClick,
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                    style = MaterialTheme.typography.titleSmall,
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
                ResumenFinca(
                    nombreUsuario = estado.nombreUsuario,
                    nombreFinca = estado.nombreFinca,
                    totalAnimales = estado.animales.size,
                )
            }
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BovinaTextField(
                        value = busqueda,
                        onValueChange = { busqueda = it },
                        modifier = Modifier.weight(1f),
                        placeholder = "Buscar por chapeta o nombre...",
                        leadingIcon = Icons.Filled.Search,
                    )
                    IconButton(onClick = {}) {
                        Icon(Icons.Filled.FilterList, contentDescription = "Filtros")
                    }
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(FILTROS_PROPOSITO) { proposito ->
                        BovinaFilterChip(
                            texto = proposito?.aEtiqueta() ?: "Todos",
                            seleccionado = estado.filtro == proposito,
                            onClick = { viewModel.onFiltroSeleccionado(proposito) },
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
private fun ResumenFinca(
    nombreUsuario: String,
    nombreFinca: String,
    totalAnimales: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Buenos días, $nombreUsuario",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = nombreFinca,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            PillBadge(
                texto = "$totalAnimales en finca",
                containerColor = BovinaForest,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = "Todos los datos sincronizados sin internet",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
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

    BovinaCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.Top) {
            FotoAnimal(fotoUri = animal.fotoUri, modifier = Modifier.size(72.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = animal.nombre,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    item.codigoChapeta?.let { PillBadge(texto = "#$it") }
                }
                Text(
                    text = "${animal.raza} · ${animal.etapa.aEtiqueta()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Edad: ${formatearEdad(animal.fechaNacimiento)}" +
                        if (animal.fechaNacimientoEsEstimada) " (estimada)" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.MonitorWeight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = registro?.let { "Último peso: ${it.peso} kg" } ?: "Sin pesajes registrados",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                registro?.let {
                    Text(
                        text = "Pesaje: hace ${diasDesde(it.timestamp)} días",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(text = "Ficha", style = MaterialTheme.typography.labelLarge)
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        PillBadge(
            texto = animal.proposito.aEtiqueta(),
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

private fun diasDesde(timestamp: Long): Long =
    TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - timestamp).coerceAtLeast(0)
