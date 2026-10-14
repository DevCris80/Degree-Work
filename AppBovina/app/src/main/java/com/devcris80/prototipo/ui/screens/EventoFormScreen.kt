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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.createSavedStateHandle
import com.devcris80.prototipo.ui.util.appViewModel

@Composable
fun EventoFormScreen(
    onGuardado: () -> Unit,
) {
    val viewModel = appViewModel { container ->
        val idAnimal = createSavedStateHandle().get<String>("idAnimal").orEmpty()
        NuevoEventoViewModel(idAnimal, container.eventoRepository, container.cuentaRepository)
    }
    val estado by viewModel.estado.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.eventos.collect { evento ->
            when (evento) {
                NuevoEventoEvento.Guardado -> onGuardado()
            }
        }
    }

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
                value = estado.tipoEvento,
                onValueChange = viewModel::onTipoEventoCambiado,
                label = { Text("Tipo (vacuna, tratamiento, nacimiento, inseminacion)") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = estado.detalle,
                onValueChange = viewModel::onDetalleCambiado,
                label = { Text("Detalle") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                enabled = estado.puedeGuardar,
                onClick = viewModel::onGuardar,
            ) {
                Text("Guardar")
            }
        }
    }
}
