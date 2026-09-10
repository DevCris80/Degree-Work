package com.devcris80.prototipo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import com.devcris80.prototipo.data.Animal
import com.devcris80.prototipo.data.Evento
import com.devcris80.prototipo.data.Registro
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AnimalDetailScreen(
    animal: Animal,
    eventos: Flow<List<Evento>>,
    registros: Flow<List<Registro>>,
    onNuevoEventoClick: () -> Unit,
) {
    val listaEventos by eventos.collectAsState(initial = emptyList())
    val listaRegistros by registros.collectAsState(initial = emptyList())
    val formatoFecha = remember(animal.idAnimal) { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onNuevoEventoClick) {
                Icon(Icons.Filled.Add, contentDescription = "Registrar evento")
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Text(animal.nombre, style = MaterialTheme.typography.headlineSmall)
                Text("Raza: ${animal.raza}")
                Text("Sexo: ${animal.sexo}")
                Text("Edad: ${animal.edad}")
                Text("Propósito: ${animal.proposito}")
            }
            item {
                Text("Pesajes", style = MaterialTheme.typography.titleMedium)
            }
            items(listaRegistros, key = { it.idRegistro }) { registro ->
                Card {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("${registro.peso} kg")
                        Text(formatoFecha.format(Date(registro.timestamp)))
                    }
                }
            }
            item {
                Text("Eventos", style = MaterialTheme.typography.titleMedium)
            }
            items(listaEventos, key = { it.idEvento }) { evento ->
                Card {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(evento.tipoEvento)
                        Text(evento.detalle)
                        Text(formatoFecha.format(Date(evento.fecha)))
                    }
                }
            }
        }
    }
}
