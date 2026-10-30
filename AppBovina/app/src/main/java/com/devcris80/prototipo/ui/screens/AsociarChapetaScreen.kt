package com.devcris80.prototipo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.createSavedStateHandle
import com.devcris80.prototipo.ui.components.BovinaCard
import com.devcris80.prototipo.ui.components.BovinaPrimaryButton
import com.devcris80.prototipo.ui.components.BovinaTopBar
import com.devcris80.prototipo.ui.components.CampoConIcono
import com.devcris80.prototipo.ui.components.OfertaPesajesPendientesDialog
import com.devcris80.prototipo.ui.theme.BovinaScreenGutter
import com.devcris80.prototipo.ui.util.EstadoNfc
import com.devcris80.prototipo.ui.util.LectorNfc
import com.devcris80.prototipo.ui.util.appViewModel

/**
 * Asociar una Chapeta nueva a un Animal existente (CU-11). Desde el Detalle llega el Animal y se escribe o escanea el
 * codigo; desde Escanear llega el codigo leído y se elige el Animal.
 */
@Composable
fun AsociarChapetaScreen(
    onAsociada: (idAnimal: String) -> Unit,
    onVolver: () -> Unit,
) {
    val viewModel = appViewModel { container ->
        val savedState = createSavedStateHandle()
        AsociarChapetaViewModel(
            savedState.get<String>("idAnimal"),
            savedState.get<String>("codigo"),
            container.animalRepository,
            container.chapetaRepository,
            container.asociarChapeta,
            container.asignarPesajesPendientes,
        )
    }
    val estado by viewModel.estado.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val estadoNfc = LectorNfc { codigo -> if (!estado.codigoSoloLectura) viewModel.onCodigoCambiado(codigo) }

    LaunchedEffect(viewModel) {
        viewModel.eventos.collect { evento ->
            when (evento) {
                is AsociarChapetaEvento.Asociada -> onAsociada(evento.idAnimal)
            }
        }
    }
    LaunchedEffect(estado.errorAsignacionPendientes) {
        if (estado.errorAsignacionPendientes) {
            snackbarHostState.showSnackbar("No se pudieron asignar los pesajes pendientes")
            viewModel.onErrorAsignacionPendientesVisto()
        }
    }

    Scaffold(
        topBar = { BovinaTopBar(titulo = "Asociar chapeta", onVolver = onVolver) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                horizontal = BovinaScreenGutter,
                vertical = 4.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Text(
                    text = if (estado.animalFijo) {
                        "Asocie una chapeta nueva a ${estado.nombreAnimal.orEmpty()}."
                    } else {
                        "Elija el animal que lleva esta chapeta."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                CampoConIcono(
                    label = "Código de la chapeta",
                    value = estado.codigo,
                    onValueChange = viewModel::onCodigoCambiado,
                    icon = Icons.Filled.Numbers,
                    readOnly = estado.codigoSoloLectura,
                    helperText = if (!estado.codigoSoloLectura && estadoNfc == EstadoNfc.LISTO) {
                        "Escríbalo o acerque el tag NFC al celular."
                    } else {
                        null
                    },
                )
            }
            if (!estado.animalFijo) {
                if (estado.candidatos.isEmpty()) {
                    item {
                        Text(
                            text = "No hay animales activos sin chapeta. Libere la chapeta de uno desde su detalle, " +
                                "o registre un animal nuevo.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(estado.candidatos, key = { it.idAnimal }) { animal ->
                    val seleccionado = animal.idAnimal == estado.idAnimal
                    BovinaCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { viewModel.onAnimalSeleccionado(animal.idAnimal) },
                        containerColor = if (seleccionado) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLowest
                        },
                    ) {
                        Text(
                            text = animal.nombre,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
            estado.error?.let { error ->
                item {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            item {
                BovinaPrimaryButton(
                    texto = "Asociar chapeta",
                    onClick = viewModel::onAsociar,
                    enabled = estado.puedeAsociar,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Filled.CheckCircle,
                )
            }
        }
    }

    estado.ofertaPesajesPendientes?.let { oferta ->
        OfertaPesajesPendientesDialog(oferta = oferta, onRespondida = viewModel::onOfertaPesajesPendientesRespondida)
    }
}
