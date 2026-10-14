package com.devcris80.prototipo.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devcris80.prototipo.service.PesajeHttpForegroundService
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class EstadoServicioUiState(val activo: Boolean = false)

/** Espeja PesajeHttpForegroundService.isRunning como estado de pantalla. */
class EstadoServicioViewModel : ViewModel() {
    val estado: StateFlow<EstadoServicioUiState> = PesajeHttpForegroundService.isRunning
        .map { EstadoServicioUiState(activo = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoServicioUiState())
}
