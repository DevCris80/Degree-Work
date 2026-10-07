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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
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
import com.devcris80.prototipo.data.Animal
import com.devcris80.prototipo.data.Chapeta
import com.devcris80.prototipo.data.PerfilFinca
import com.devcris80.prototipo.data.Registro
import com.devcris80.prototipo.data.Usuario
import com.devcris80.prototipo.ui.components.FotoAnimal
import com.devcris80.prototipo.ui.components.PillBadge
import com.devcris80.prototipo.ui.theme.BovinaScreenGutter
import com.devcris80.prototipo.ui.util.formatearEdad
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

private val PROPOSITOS_FILTRO = listOf("Todos", "Leche", "Carne", "Doble Propósito")

@Composable
fun ListaAnimalesScreen(
    perfilFinca: Flow<PerfilFinca?>,
    usuario: Flow<Usuario?>,
    animales: Flow<List<Animal>>,
    chapetasActivas: Flow<List<Chapeta>>,
    obtenerUltimoRegistro: (idAnimal: String) -> Flow<Registro?>,
    onAnimalClick: (Animal) -> Unit,
    onNuevoAnimalClick: () -> Unit,
) {
    val perfil by perfilFinca.collectAsState(initial = null)
    val usuarioActual by usuario.collectAsState(initial = null)
    val lista by animales.collectAsState(initial = emptyList())
    val chapetas by chapetasActivas.collectAsState(initial = emptyList())
    val chipsPorAnimal = remember(chapetas) { chapetas.associateBy({ it.idAnimal }, { it.idChip }) }

    var busqueda by remember { mutableStateOf("") }
    var filtroProposito by remember { mutableStateOf("Todos") }

    val listaFiltrada = lista.filter { animal ->
        val coincideBusqueda = busqueda.isBlank() ||
            animal.nombre.contains(busqueda, ignoreCase = true) ||
            chipsPorAnimal[animal.idAnimal]?.contains(busqueda, ignoreCase = true) == true
        val coincideFiltro = filtroProposito == "Todos" || animal.proposito == filtroProposito
        coincideBusqueda && coincideFiltro
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
                            text = "  FINCA ${perfil?.nombreFinca.orEmpty()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(text = "Animales", style = MaterialTheme.typography.headlineLarge)
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
                                    text = "Buenos días, ${usuarioActual?.nombre.orEmpty()}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                                Text(
                                    text = perfil?.nombreFinca.orEmpty(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                            PillBadge(
                                texto = "${lista.size} en finca",
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
                    items(PROPOSITOS_FILTRO) { proposito ->
                        FilterChip(
                            selected = filtroProposito == proposito,
                            onClick = { filtroProposito = proposito },
                            label = { Text(proposito) },
                        )
                    }
                }
            }
            items(listaFiltrada, key = { it.idAnimal }) { animal ->
                TarjetaAnimal(
                    animal = animal,
                    ultimoRegistro = obtenerUltimoRegistro(animal.idAnimal),
                    onClick = { onAnimalClick(animal) },
                )
            }
        }
    }
}

@Composable
private fun TarjetaAnimal(
    animal: Animal,
    ultimoRegistro: Flow<Registro?>,
    onClick: () -> Unit,
) {
    val registro by ultimoRegistro.collectAsState(initial = null)

    Card(onClick = onClick, shape = RoundedCornerShape(16.dp)) {
        Row(modifier = Modifier.padding(12.dp)) {
            FotoAnimal(fotoUri = animal.fotoUri, modifier = Modifier.size(72.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                PillBadge(texto = animal.proposito)
                Text(
                    text = animal.nombre,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    text = "${animal.raza} · ${animal.etapa}",
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
