package com.devcris80.prototipo.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devcris80.prototipo.domain.usecase.IndicadoresHato
import com.devcris80.prototipo.domain.usecase.ObservarIndicadoresHato
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class IndicadoresUiState(
    /** Nulos mientras se calculan por primera vez. */
    val indicadores: IndicadoresHato? = null,
    /** Nula mientras no exista la sincronización (#83): la pantalla dice "nunca". */
    val ultimaSincronizacion: Long? = null,
)

/** Estado de Indicadores: los del hato, calculados sobre la base local. */
class IndicadoresViewModel(
    observarIndicadoresHato: ObservarIndicadoresHato,
) : ViewModel() {
    val estado: StateFlow<IndicadoresUiState> = observarIndicadoresHato()
        .map { IndicadoresUiState(indicadores = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), IndicadoresUiState())
}
