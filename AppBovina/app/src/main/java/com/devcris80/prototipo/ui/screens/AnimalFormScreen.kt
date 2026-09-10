package com.devcris80.prototipo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.devcris80.prototipo.data.Animal
import java.util.UUID

@Composable
fun AnimalFormScreen(
    onGuardar: (Animal) -> Unit,
) {
    var nombre by remember { mutableStateOf("") }
    var raza by remember { mutableStateOf("") }
    var sexo by remember { mutableStateOf("") }
    var edadTexto by remember { mutableStateOf("") }
    var proposito by remember { mutableStateOf("") }

    val puedeGuardar = nombre.isNotBlank() && raza.isNotBlank() && sexo.isNotBlank() &&
        edadTexto.toIntOrNull() != null && proposito.isNotBlank()

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Registro de animal nuevo")
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = raza,
                onValueChange = { raza = it },
                label = { Text("Raza") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = sexo,
                onValueChange = { sexo = it },
                label = { Text("Sexo") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = edadTexto,
                onValueChange = { edadTexto = it },
                label = { Text("Edad") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = proposito,
                onValueChange = { proposito = it },
                label = { Text("Propósito") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                enabled = puedeGuardar,
                onClick = {
                    onGuardar(
                        Animal(
                            idAnimal = UUID.randomUUID().toString(),
                            nombre = nombre,
                            raza = raza,
                            sexo = sexo,
                            edad = edadTexto.toInt(),
                            proposito = proposito,
                        ),
                    )
                },
            ) {
                Text("Guardar")
            }
        }
    }
}
