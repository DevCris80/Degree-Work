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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.devcris80.prototipo.ui.components.BannerInformativo
import com.devcris80.prototipo.ui.components.BovinaCard
import com.devcris80.prototipo.ui.components.BovinaPrimaryButton
import com.devcris80.prototipo.ui.components.BovinaTextField
import com.devcris80.prototipo.ui.components.BovinaTopBar
import com.devcris80.prototipo.ui.components.CampoSelector
import com.devcris80.prototipo.ui.theme.BovinaForest
import com.devcris80.prototipo.ui.theme.BovinaScreenGutter
import com.devcris80.prototipo.ui.theme.Prototipo1Theme

/** Tipo de evento que se puede programar. */
enum class TipoEventoProgramado(
    val titulo: String,
    val descripcion: String,
    val icono: ImageVector,
) {
    Vacunacion("Vacunación", "Ciclos sanitarios y vacunas", Icons.Filled.Vaccines),
    Parto("Parto", "Parto estimado y seguimiento posparto", Icons.Filled.Pets),
}

/**
 * Estado de la capa visual. Todavía no hay ViewModel: los campos de fecha, hora, animal y vía se
 * muestran como selectores sin acción hasta que se implemente la lógica.
 */
data class ProgramarEventoUiState(
    val tipo: TipoEventoProgramado = TipoEventoProgramado.Vacunacion,
    val titulo: String = "",
    val fecha: String? = null,
    val hora: String? = null,
    val animal: String? = null,
    val biologico: String = "",
    val dosis: String = "",
    val via: String? = null,
)

@Composable
fun ProgramarEventoScreen(
    onVolver: () -> Unit,
) {
    var estado by remember { mutableStateOf(ProgramarEventoUiState()) }
    ProgramarEventoContent(
        estado = estado,
        onVolver = onVolver,
        onTipoSeleccionado = { estado = estado.copy(tipo = it) },
        onTituloCambiado = { estado = estado.copy(titulo = it) },
        onFechaClick = {},
        onHoraClick = {},
        onAnimalClick = {},
        onBiologicoCambiado = { estado = estado.copy(biologico = it) },
        onDosisCambiada = { estado = estado.copy(dosis = it) },
        onViaClick = {},
        onProgramar = {},
    )
}

@Composable
private fun ProgramarEventoContent(
    estado: ProgramarEventoUiState,
    onVolver: () -> Unit,
    onTipoSeleccionado: (TipoEventoProgramado) -> Unit,
    onTituloCambiado: (String) -> Unit,
    onFechaClick: () -> Unit,
    onHoraClick: () -> Unit,
    onAnimalClick: () -> Unit,
    onBiologicoCambiado: (String) -> Unit,
    onDosisCambiada: (String) -> Unit,
    onViaClick: () -> Unit,
    onProgramar: () -> Unit,
) {
    Scaffold(
        topBar = { BovinaTopBar(titulo = "Programar evento", onVolver = onVolver) },
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
                texto = "Agenda sanitaria y reproductiva. Seleccione el tipo de evento a programar " +
                    "en el calendario de la finca.",
                icon = Icons.Filled.EventAvailable,
            )

            Text(
                text = "Tipo de evento",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 4.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TipoEventoProgramado.entries.forEach { tipo ->
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
                        CampoConEtiqueta("Fecha programada", Modifier.weight(1f)) {
                            CampoSelector(
                                valor = estado.fecha,
                                marcador = "dd/mm/aaaa",
                                onClick = onFechaClick,
                                leadingIcon = Icons.Filled.CalendarMonth,
                                trailingIcon = Icons.Filled.CalendarMonth,
                            )
                        }
                        CampoConEtiqueta("Hora", Modifier.weight(1f)) {
                            CampoSelector(
                                valor = estado.hora,
                                marcador = "hh:mm",
                                onClick = onHoraClick,
                                leadingIcon = Icons.Filled.AccessTime,
                                trailingIcon = Icons.Filled.AccessTime,
                            )
                        }
                    }
                    CampoConEtiqueta("Aplicar a") {
                        CampoSelector(
                            valor = estado.animal,
                            marcador = "Seleccionar animal",
                            onClick = onAnimalClick,
                            leadingIcon = Icons.Filled.Pets,
                        )
                    }
                    if (estado.tipo == TipoEventoProgramado.Vacunacion) {
                        DetallesBiologico(
                            estado = estado,
                            onBiologicoCambiado = onBiologicoCambiado,
                            onDosisCambiada = onDosisCambiada,
                            onViaClick = onViaClick,
                        )
                    }
                }
            }

            BovinaPrimaryButton(
                texto = "Programar evento",
                onClick = onProgramar,
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Filled.EventAvailable,
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
private fun CampoConEtiqueta(
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

@Composable
private fun DetallesBiologico(
    estado: ProgramarEventoUiState,
    onBiologicoCambiado: (String) -> Unit,
    onDosisCambiada: (String) -> Unit,
    onViaClick: () -> Unit,
) {
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
                imageVector = Icons.Filled.Vaccines,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "Detalles del biológico y dosis",
                style = MaterialTheme.typography.titleSmall,
            )
        }
        CampoConEtiqueta("Biológico o medicamento") {
            BovinaTextField(
                value = estado.biologico,
                onValueChange = onBiologicoCambiado,
                placeholder = "Nombre del biológico",
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CampoConEtiqueta("Dosis por cabeza", Modifier.weight(1f)) {
                BovinaTextField(
                    value = estado.dosis,
                    onValueChange = onDosisCambiada,
                    placeholder = "Ej: 2 ml",
                )
            }
            CampoConEtiqueta("Vía de aplicación", Modifier.weight(1f)) {
                CampoSelector(
                    valor = estado.via,
                    marcador = "Seleccionar",
                    onClick = onViaClick,
                )
            }
        }
    }
}

@Composable
private fun TarjetaTipoEvento(
    tipo: TipoEventoProgramado,
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
                        tint = if (seleccionado) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            BovinaForest
                        },
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
            Text(text = tipo.titulo, style = MaterialTheme.typography.headlineMedium)
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
private fun ProgramarEventoVacunacionPreview() {
    Prototipo1Theme {
        ProgramarEventoContent(
            estado = ProgramarEventoUiState(
                titulo = "Vacunación del ciclo I",
                fecha = "28/05/2026",
                hora = "07:00",
                animal = "Lucero (091)",
                biologico = "Aftosa bivalente",
                dosis = "2 ml",
                via = "Subcutánea",
            ),
            onVolver = {},
            onTipoSeleccionado = {},
            onTituloCambiado = {},
            onFechaClick = {},
            onHoraClick = {},
            onAnimalClick = {},
            onBiologicoCambiado = {},
            onDosisCambiada = {},
            onViaClick = {},
            onProgramar = {},
        )
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun ProgramarEventoPartoPreview() {
    Prototipo1Theme {
        ProgramarEventoContent(
            estado = ProgramarEventoUiState(tipo = TipoEventoProgramado.Parto),
            onVolver = {},
            onTipoSeleccionado = {},
            onTituloCambiado = {},
            onFechaClick = {},
            onHoraClick = {},
            onAnimalClick = {},
            onBiologicoCambiado = {},
            onDosisCambiada = {},
            onViaClick = {},
            onProgramar = {},
        )
    }
}
