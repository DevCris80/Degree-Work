package com.devcris80.prototipo.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devcris80.prototipo.domain.usecase.EvolucionPeso
import com.devcris80.prototipo.domain.usecase.IndicadoresHato
import com.devcris80.prototipo.domain.usecase.ObservarEvolucionPeso
import com.devcris80.prototipo.domain.usecase.ObservarIndicadoresHato
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

data class IndicadoresUiState(
    /** Nulos mientras se calculan por primera vez. */
    val indicadores: IndicadoresHato? = null,
    /** El Animal cuya evolución de peso se muestra; nulo hasta que se elige uno. */
    val idAnimalElegido: String? = null,
    val evolucionPeso: EvolucionPeso? = null,
    /** Nula mientras no exista la sincronización (#83): la pantalla dice "nunca". */
    val ultimaSincronizacion: Long? = null,
)

/** Estado de Indicadores: los del hato y la evolución de peso del Animal elegido. */
@OptIn(ExperimentalCoroutinesApi::class)
class IndicadoresViewModel(
    observarIndicadoresHato: ObservarIndicadoresHato,
    observarEvolucionPeso: ObservarEvolucionPeso,
) : ViewModel() {
    private val idAnimalElegido = MutableStateFlow<String?>(null)

    private val evolucionPeso = idAnimalElegido.flatMapLatest { idAnimal ->
        if (idAnimal == null) flowOf(null) else observarEvolucionPeso(idAnimal)
    }

    val estado: StateFlow<IndicadoresUiState> = combine(
        observarIndicadoresHato(),
        idAnimalElegido,
        evolucionPeso,
    ) { indicadores, idAnimal, evolucion ->
        IndicadoresUiState(indicadores = indicadores, idAnimalElegido = idAnimal, evolucionPeso = evolucion)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), IndicadoresUiState())

    fun onAnimalElegido(idAnimal: String) {
        idAnimalElegido.value = idAnimal
    }
}
