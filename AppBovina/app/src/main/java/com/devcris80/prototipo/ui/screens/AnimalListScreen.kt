package com.devcris80.prototipo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devcris80.prototipo.data.Animal
import kotlinx.coroutines.flow.Flow

@Composable
fun AnimalListScreen(
    animales: Flow<List<Animal>>,
    onAnimalClick: (Animal) -> Unit,
    onNuevoAnimalClick: () -> Unit,
) {
    val lista by animales.collectAsState(initial = emptyList())

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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(lista, key = { it.idAnimal }) { animal ->
                Card(onClick = { onAnimalClick(animal) }) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = animal.nombre, style = MaterialTheme.typography.titleMedium)
                        Text(text = animal.raza, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
