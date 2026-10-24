package com.devcris80.prototipo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.createSavedStateHandle
import com.devcris80.prototipo.domain.model.EntidadConRevisiones
import com.devcris80.prototipo.ui.components.BannerConflicto
import com.devcris80.prototipo.ui.components.BovinaCard
import com.devcris80.prototipo.ui.components.BovinaPrimaryButton
import com.devcris80.prototipo.ui.components.BovinaSecondaryButton
import com.devcris80.prototipo.ui.components.BovinaTopBar
import com.devcris80.prototipo.ui.components.FotoAnimal
import com.devcris80.prototipo.ui.components.MarcaEnConflicto
import com.devcris80.prototipo.ui.components.PillBadge
import com.devcris80.prototipo.ui.theme.BovinaForest
import com.devcris80.prototipo.ui.theme.BovinaScreenGutter
import com.devcris80.prototipo.ui.util.aEtiqueta
import com.devcris80.prototipo.ui.util.appViewModel
import com.devcris80.prototipo.ui.util.formatearEdad
import com.devcris80.prototipo.ui.util.nombreConBaja
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun DetalleAnimalScreen(
    onVolver: () -> Unit,
    onNuevoEventoClick: () -> Unit,
    onNuevoPesajeClick: () -> Unit,
    onEditarClick: () -> Unit = {},
    onAnimalClick: (idAnimal: String) -> Unit = {},
    onResolverConflictoClick: (entidad: EntidadConRevisiones, idFila: String) -> Unit = { _, _ -> },
) {
    val viewModel = appViewModel { container ->
        val idAnimal = createSavedStateHandle().get<String>("idAnimal").orEmpty()
        DetalleAnimalViewModel(
            idAnimal,
            container.animalRepository,
            container.eventoRepository,
            container.registroRepository,
            container.chapetaRepository,
            container.darDeBajaAnimal,
            container.liberarChapeta,
            container.conflictoRepository,
        )
    }
    val estado by viewModel.estado.collectAsState()
    val animal = estado.animal ?: return

    val listaEventos = estado.eventos
    val listaRegistros = estado.registros
    val chapeta = estado.chapetaActiva
    val formatoFecha = remember(animal.idAnimal) { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    var tabSeleccionada by remember { mutableIntStateOf(0) }
    var confirmarBaja by remember { mutableStateOf(false) }
    var confirmarLiberacion by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { BovinaTopBar(titulo = "Detalle Animal", onVolver = onVolver) },
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(MaterialTheme.shapes.extraLarge),
                ) {
                    FotoAnimal(
                        fotoUri = animal.fotoUri,
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(0.dp),
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(130.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color.Transparent, MaterialTheme.colorScheme.scrim.copy(alpha = 0.65f)),
                                ),
                            ),
                    )
                    chapeta?.let { chapetaActual ->
                        PillBadge(
                            texto = "#${chapetaActual.codigo}",
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(16.dp),
                        )
                    }
                    IconButton(
                        onClick = onEditarClick,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest),
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = "Editar")
                    }
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "FICHA BOVINA",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                            )
                            Text(
                                text = animal.nombre,
                                style = MaterialTheme.typography.displayMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                        PillBadge(texto = animal.proposito.aEtiqueta())
                    }
                }
            }
            if (estado.animalEnConflicto) {
                item {
                    BannerConflicto(
                        texto = "Dos personas editaron este animal a la vez. Elija qué datos quedan.",
                        onResolver = { onResolverConflictoClick(EntidadConRevisiones.ANIMAL, animal.idAnimal) },
                    )
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DatoTile("Raza", animal.raza, Modifier.weight(1f))
                        DatoTile("Sexo", animal.sexo.aEtiqueta(), Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DatoTile("Etapa", animal.etapa.aEtiqueta(), Modifier.weight(1f))
                        DatoTile(
                            "Edad",
                            formatearEdad(animal.fechaNacimiento) +
                                if (animal.fechaNacimientoEsEstimada) " (estimada)" else "",
                            Modifier.weight(1f),
                        )
                    }
                    DatoTile("Destino", animal.proposito.aEtiqueta(), Modifier.fillMaxWidth())
                }
            }
            if (estado.padre != null || estado.madre != null) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        estado.padre?.let { padre ->
                            DatoTile(
                                "Padre",
                                "${padre.nombreConBaja()} ›",
                                Modifier.weight(1f),
                                onClick = { onAnimalClick(padre.idAnimal) },
                            )
                        }
                        estado.madre?.let { madre ->
                            DatoTile(
                                "Madre",
                                "${madre.nombreConBaja()} ›",
                                Modifier.weight(1f),
                                onClick = { onAnimalClick(madre.idAnimal) },
                            )
                        }
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Último peso registrado",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = ultimoPeso?.let { "${it.peso} kg" } ?: "Sin pesajes registrados",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (delta != null) {
                            val signo = if (delta >= 0) "+" else ""
                            Text(
                                text = "$signo${"%.1f".format(delta)} kg desde el pesaje anterior",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    if (ultimoPeso != null) {
                        val dias = TimeUnit.MILLISECONDS
                            .toDays(System.currentTimeMillis() - ultimoPeso.timestamp)
                            .coerceAtLeast(0)
                        Text(
                            text = "Hace $dias días",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(6.dp),
                ) {
                    PestanaPildora(
                        texto = "Pesajes (${listaRegistros.size})",
                        seleccionada = tabSeleccionada == 0,
                        onClick = { tabSeleccionada = 0 },
                        modifier = Modifier.weight(1f),
                    )
                    PestanaPildora(
                        texto = "Eventos (${listaEventos.size})",
                        seleccionada = tabSeleccionada == 1,
                        onClick = { tabSeleccionada = 1 },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (tabSeleccionada == 0) {
                items(listaRegistros, key = { it.idRegistro }) { registro ->
                    val indice = listaRegistros.indexOf(registro)
                    val anterior = listaRegistros.getOrNull(indice + 1)
                    val delta = anterior?.let { registro.peso - it.peso }
                    FilaDetalle(
                        icono = Icons.Filled.MonitorWeight,
                        destacada = indice == 0,
                        titulo = formatoFecha.format(Date(registro.timestamp)),
                        subtitulo = null,
                        valor = "${registro.peso} kg",
                        valorSecundario = if (delta != null) {
                            val signo = if (delta >= 0) "+" else ""
                            "$signo${"%.1f".format(delta)} kg"
                        } else {
                            "Base inicial"
                        },
                        enConflicto = registro.idRegistro in estado.registrosEnConflicto,
                        onClick = { onResolverConflictoClick(EntidadConRevisiones.REGISTRO, registro.idRegistro) },
                    )
                }
            } else {
                items(listaEventos, key = { it.idEvento }) { evento ->
                    FilaDetalle(
                        icono = Icons.Filled.EventAvailable,
                        destacada = false,
                        titulo = evento.tipoEvento,
                        subtitulo = evento.detalle,
                        valor = formatoFecha.format(Date(evento.fecha)),
                        valorSecundario = null,
                        enConflicto = evento.idEvento in estado.eventosEnConflicto,
                        onClick = { onResolverConflictoClick(EntidadConRevisiones.EVENTO, evento.idEvento) },
                    )
                }
            }
            if (animal.activo) {
                item {
                    BovinaPrimaryButton(
                        texto = "Registrar pesaje",
                        onClick = onNuevoPesajeClick,
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Filled.MonitorWeight,
                    )
                }
            }
            item {
                BovinaSecondaryButton(
                    texto = "Registrar evento",
                    onClick = onNuevoEventoClick,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Filled.Add,
                )
            }
            item {
                chapeta?.let { chapetaActual ->
                    BovinaSecondaryButton(
                        texto = "Liberar chapeta (${chapetaActual.codigo})",
                        onClick = { confirmarLiberacion = true },
                        modifier = Modifier.fillMaxWidth(),
                    )
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
                    BovinaSecondaryButton(
                        texto = "Dar de baja",
                        onClick = { confirmarBaja = true },
                        modifier = Modifier.fillMaxWidth(),
                    )
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
                        viewModel.onLiberarChapeta(chapetaActual.idChapeta)
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
                    viewModel.onDarDeBaja()
                }) { Text("Dar de baja") }
            },
            dismissButton = {
                TextButton(onClick = { confirmarBaja = false }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun DatoTile(
    etiqueta: String,
    valor: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .then(if (onClick != null) Modifier.clickable(onClickLabel = "Ver ficha", onClick = onClick) else Modifier)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(text = valor, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun PestanaPildora(
    texto: String,
    seleccionada: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (seleccionada) MaterialTheme.colorScheme.surfaceContainerLowest else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = texto,
            style = MaterialTheme.typography.labelLarge,
            color = if (seleccionada) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FilaDetalle(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    destacada: Boolean,
    titulo: String,
    subtitulo: String?,
    valor: String,
    valorSecundario: String?,
    enConflicto: Boolean,
    onClick: () -> Unit,
) {
    // Solo una fila en Conflicto abre la pantalla de resolver; las demás no se tocan.
    BovinaCard(modifier = Modifier.fillMaxWidth(), onClick = if (enConflicto) onClick else null) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(
                        if (destacada) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icono,
                    contentDescription = null,
                    tint = BovinaForest,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = titulo, style = MaterialTheme.typography.titleSmall)
                if (enConflicto) MarcaEnConflicto()
                if (subtitulo != null) {
                    Text(
                        text = subtitulo,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = valor, style = MaterialTheme.typography.titleMedium)
                if (valorSecundario != null) {
                    Text(
                        text = valorSecundario,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
