package com.devcris80.prototipo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.createSavedStateHandle
import com.devcris80.prototipo.ui.components.BovinaPrimaryButton
import com.devcris80.prototipo.ui.components.BovinaTopBar
import com.devcris80.prototipo.ui.components.CampoConIcono
import com.devcris80.prototipo.ui.theme.BovinaScreenGutter
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

    Scaffold(
        topBar = { BovinaTopBar(titulo = "Registro de evento") },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(BovinaScreenGutter),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CampoConIcono(
                label = "Tipo (vacuna, tratamiento, nacimiento, inseminacion)",
                value = estado.tipoEvento,
                onValueChange = viewModel::onTipoEventoCambiado,
                icon = Icons.Filled.EventAvailable,
            )
            CampoConIcono(
                label = "Detalle",
                value = estado.detalle,
                onValueChange = viewModel::onDetalleCambiado,
                icon = Icons.Filled.Info,
            )
            BovinaPrimaryButton(
                texto = "Guardar",
                onClick = viewModel::onGuardar,
                enabled = estado.puedeGuardar,
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Filled.CheckCircle,
            )
        }
    }
}
