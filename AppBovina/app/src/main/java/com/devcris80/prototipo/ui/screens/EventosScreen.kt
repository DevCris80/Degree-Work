package com.devcris80.prototipo.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devcris80.prototipo.ui.components.BovinaCard
import com.devcris80.prototipo.ui.components.BovinaTopBar
import com.devcris80.prototipo.ui.theme.BovinaForest
import com.devcris80.prototipo.ui.theme.BovinaScreenGutter
import com.devcris80.prototipo.ui.theme.Prototipo1Theme

/** Evento ya registrado, tal como lo muestra la lista. */
data class EventoItemUi(
    val tipo: String,
    val detalle: String,
    val fecha: String,
)

/**
 * Lista de eventos registrados. Por ahora es solo la capa visual: la pantalla todavía no tiene
 * ViewModel ni fuente de datos, así que la app la muestra vacía.
 */
@Composable
fun EventosScreen(
    onProgramarClick: () -> Unit,
) {
    EventosContent(eventos = emptyList(), onProgramarClick = onProgramarClick)
}

@Composable
private fun EventosContent(
    eventos: List<EventoItemUi>,
    onProgramarClick: () -> Unit,
) {
    Scaffold(
        topBar = { BovinaTopBar(titulo = "Eventos", mostrarLogo = true) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onProgramarClick,
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(text = "  Programar evento", style = MaterialTheme.typography.labelLarge)
            }
        },
    ) { innerPadding ->
        if (eventos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Aún no hay eventos registrados.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = BovinaScreenGutter),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = BovinaScreenGutter),
            ) {
                items(eventos) { evento -> TarjetaEvento(evento) }
            }
        }
    }
}

@Composable
private fun TarjetaEvento(evento: EventoItemUi) {
    BovinaCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.EventAvailable,
                    contentDescription = null,
                    tint = BovinaForest,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = evento.tipo, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = evento.detalle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = evento.fecha,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EventosConDatosPreview() {
    Prototipo1Theme {
        EventosContent(
            eventos = listOf(
                EventoItemUi("vacuna", "Aftosa, dosis de refuerzo", "10/05/2026 08:30"),
                EventoItemUi("tratamiento", "Desparasitación", "02/05/2026 16:10"),
            ),
            onProgramarClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EventosVacioPreview() {
    Prototipo1Theme { EventosContent(eventos = emptyList(), onProgramarClick = {}) }
}
