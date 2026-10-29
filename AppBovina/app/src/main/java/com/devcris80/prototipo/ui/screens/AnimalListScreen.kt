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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.ui.components.BovinaCard
import com.devcris80.prototipo.ui.components.BovinaFilterChip
import com.devcris80.prototipo.ui.components.BovinaTextField
import com.devcris80.prototipo.ui.components.BovinaTopBar
import com.devcris80.prototipo.ui.components.FotoAnimal
import com.devcris80.prototipo.ui.components.MarcaEnConflicto
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
    onRegistrarUbicacionClick: () -> Unit,
    onRechazosClick: () -> Unit,
    onInvitarClick: () -> Unit,
) {
    val viewModel = appViewModel { container ->
        ListaAnimalesViewModel(
            container.cuentaRepository,
            container.animalRepository,
            container.chapetaRepository,
            container.registroRepository,
            container.observarPesajesPendientes,
            container.sesionRepository,
            container.sincronizacionRepository,
            container.conflictoRepository,
            container.rechazoRepository,
        )
    }
    val estado by viewModel.estado.collectAsState()
    var confirmarCierre by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(estado.avisoSincronizacion) {
        estado.avisoSincronizacion?.let {
            viewModel.onAvisoMostrado()
            snackbar.showSnackbar(it)
        }
    }

    var busqueda by remember { mutableStateOf("") }

    val listaFiltrada = estado.animales.filter { item ->
        busqueda.isBlank() ||
            item.animal.nombre.contains(busqueda, ignoreCase = true) ||
            item.codigoChapeta?.contains(busqueda, ignoreCase = true) == true
    }

    Scaffold(
        topBar = {
            BovinaTopBar(titulo = "Animales", mostrarLogo = true) {
                IconButton(onClick = viewModel::onSincronizar, enabled = !estado.sincronizando) {
                    if (estado.sincronizando) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Filled.Sync, contentDescription = "Sincronizar ahora")
                    }
                }
                IconButton(onClick = onIndicadoresClick) {
                    Icon(Icons.Filled.Insights, contentDescription = "Indicadores")
                }
                IconButton(onClick = { confirmarCierre = true }) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Cerrar sesión")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
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
            // Un Rechazo espera a una persona (RN-24): va primero.
            if (estado.rechazosAbiertos > 0) {
                item {
                    AvisoEnLista(
                        texto = if (estado.rechazosAbiertos == 1) "1 cambio rechazado" else "${estado.rechazosAbiertos} cambios rechazados",
                        onClick = onRechazosClick,
                        fondo = MaterialTheme.colorScheme.errorContainer,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
            if (estado.pesajesPendientes > 0) {
                item {
                    AvisoEnLista(
                        texto = "${estado.pesajesPendientes} pesajes pendientes",
                        onClick = onPesajesPendientesClick,
                        fondo = MaterialTheme.colorScheme.tertiaryContainer,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
            item {
                ResumenFinca(
                    nombreUsuario = estado.nombreUsuario,
                    nombreFinca = estado.nombreFinca,
                    totalAnimales = estado.animales.size,
                    onRegistrarUbicacionClick = onRegistrarUbicacionClick.takeIf { estado.puedeRegistrarUbicacion },
                    onInvitarClick = onInvitarClick.takeIf { estado.puedeInvitar },
                    rechazosAbiertos = estado.rechazosAbiertos,
                    cambiosSinSubir = estado.cambiosSinSubir,
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
                    enConflicto = item.animal.idAnimal in estado.animalesEnConflicto,
                    onClick = { onAnimalClick(item.animal.idAnimal) },
                )
            }
        }
    }

    if (confirmarCierre) {
        AlertDialog(
            onDismissRequest = { confirmarCierre = false },
            title = { Text("¿Cerrar sesión?") },
            text = { Text("Los datos de la finca quedan en este celular. Para volver a entrar hará falta señal.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarCierre = false
                    viewModel.onCerrarSesion()
                }) { Text("Cerrar sesión") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarCierre = false }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun ResumenFinca(
    nombreUsuario: String,
    nombreFinca: String,
    totalAnimales: Int,
    onRegistrarUbicacionClick: (() -> Unit)?,
    onInvitarClick: (() -> Unit)?,
    rechazosAbiertos: Int,
    cambiosSinSubir: Int,
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
            // Lo que pasa con los datos de este celular: primero lo que espera a una persona.
            val (icono, texto, color) = when {
                rechazosAbiertos > 0 -> Triple(Icons.Filled.Warning, "Hay cambios rechazados por revisar", MaterialTheme.colorScheme.error)
                cambiosSinSubir == 1 -> Triple(Icons.Filled.Sync, "1 cambio guardado, por subir", MaterialTheme.colorScheme.onSurfaceVariant)
                cambiosSinSubir > 1 ->
                    Triple(Icons.Filled.Sync, "$cambiosSinSubir cambios guardados, por subir", MaterialTheme.colorScheme.onSurfaceVariant)
                else -> Triple(Icons.Filled.CheckCircle, "Todos los datos sincronizados", MaterialTheme.colorScheme.primary)
            }
            Icon(
                imageVector = icono,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = texto,
                style = MaterialTheme.typography.labelMedium,
                color = color,
            )
        }
        if (onRegistrarUbicacionClick != null || onInvitarClick != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                if (onInvitarClick != null) {
                    TextButton(onClick = onInvitarClick) { Text(text = "Invitar a un Operario") }
                }
                if (onRegistrarUbicacionClick != null) {
                    TextButton(onClick = onRegistrarUbicacionClick) { Text(text = "Registrar ubicación de la finca") }
                }
            }
        }
    }
}

@Composable
private fun TarjetaAnimal(
    item: AnimalConResumen,
    enConflicto: Boolean,
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
                if (enConflicto) MarcaEnConflicto()
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

/** Un aviso que abre otra pantalla: algo que espera a una persona. */
@Composable
private fun AvisoEnLista(texto: String, onClick: () -> Unit, fondo: Color, color: Color) {
    BovinaCard(onClick = onClick, containerColor = fondo, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(imageVector = Icons.Filled.Warning, contentDescription = null, tint = color)
                Text(text = texto, style = MaterialTheme.typography.titleSmall, color = color)
            }
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = color)
        }
    }
}
