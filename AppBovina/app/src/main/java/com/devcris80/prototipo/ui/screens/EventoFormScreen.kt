package com.devcris80.prototipo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import com.devcris80.prototipo.data.local.entity.EventoEntity
import java.util.UUID

@Composable
fun EventoFormScreen(
    idAnimal: String,
    onGuardar: (EventoEntity) -> Unit,
) {
    var tipoEvento by remember { mutableStateOf("") }
    var detalle by remember { mutableStateOf("") }

    val puedeGuardar = tipoEvento.isNotBlank() && detalle.isNotBlank()

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Registro de evento")
            OutlinedTextField(
                value = tipoEvento,
                onValueChange = { tipoEvento = it },
                label = { Text("Tipo (vacuna, tratamiento, nacimiento, inseminacion)") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = detalle,
                onValueChange = { detalle = it },
                label = { Text("Detalle") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                enabled = puedeGuardar,
                onClick = {
                    onGuardar(
                        EventoEntity(
                            idEvento = UUID.randomUUID().toString(),
                            idAnimal = idAnimal,
                            tipoEvento = tipoEvento,
                            fecha = System.currentTimeMillis(),
                            detalle = detalle,
                        ),
                    )
                },
            ) {
                Text("Guardar")
            }
        }
    }
}
