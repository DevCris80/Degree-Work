package com.devcris80.prototipo.ui.screens

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.lifecycle.createSavedStateHandle
import com.devcris80.prototipo.ui.components.BovinaSecondaryButton
import com.devcris80.prototipo.ui.util.appViewModel

/**
 * Editar un pesaje del historial (CU-18) y, para el Ganadero, darlo de baja (CU-19). La baja pide confirmación
 * explicando el efecto (RNF-16).
 */
@Composable
fun EditarPesajeScreen(
    onTerminado: () -> Unit,
    onVolver: () -> Unit,
) {
    val viewModel = appViewModel { container ->
        val idRegistro = createSavedStateHandle().get<String>("idRegistro").orEmpty()
        EditarPesajeViewModel(
            idRegistro,
            container.registroRepository,
            container.cuentaRepository,
            container.actualizarRegistro,
            container.darDeBajaRegistro,
        )
    }
    val estado by viewModel.estado.collectAsState()
    var confirmarBaja by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.eventos.collect { evento ->
            when (evento) {
                EditarPesajeEvento.Guardado, EditarPesajeEvento.DadoDeBaja -> onTerminado()
            }
        }
    }

    FormularioPesaje(
        titulo = "Editar pesaje",
        peso = estado.peso,
        fecha = estado.fecha,
        error = estado.error,
        puedeGuardar = estado.puedeGuardar,
        onPesoCambiado = viewModel::onPesoCambiado,
        onFechaCambiada = viewModel::onFechaCambiada,
        onGuardar = viewModel::onGuardar,
        onVolver = onVolver,
        accionesExtra = {
            if (estado.puedeDarDeBaja && estado.cargado) {
                BovinaSecondaryButton(
                    texto = "Dar de baja este pesaje",
                    onClick = { confirmarBaja = true },
                    enabled = !estado.guardando,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    )

    if (confirmarBaja) {
        AlertDialog(
            onDismissRequest = { confirmarBaja = false },
            title = { Text("¿Dar de baja este pesaje?") },
            text = {
                Text(
                    "El pesaje dejará de aparecer en el historial y en los indicadores del animal. " +
                        "Se conserva en la base de datos, pero la baja no se puede deshacer.",
                )
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
