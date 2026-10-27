package com.devcris80.prototipo.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devcris80.prototipo.domain.model.RechazoEnBandeja
import com.devcris80.prototipo.domain.repository.RechazoRepository
import com.devcris80.prototipo.domain.usecase.ObservarBandejaRechazos
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BandejaRechazosUiState(
    val rechazos: List<RechazoEnBandeja> = emptyList(),
    val cargando: Boolean = true,
)

/**
 * Estado de la Bandeja de rechazos (CU-22): los Rechazos abiertos con su motivo y la acción sugerida. Cerrar uno lo
 * saca de la bandeja y lo conserva; la corrección, si la hay, se hace en el Animal.
 */
class BandejaRechazosViewModel(
    observarBandejaRechazos: ObservarBandejaRechazos,
    private val rechazoRepository: RechazoRepository,
) : ViewModel() {
    val estado: StateFlow<BandejaRechazosUiState> = observarBandejaRechazos()
        .map { BandejaRechazosUiState(rechazos = it, cargando = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BandejaRechazosUiState())

    fun onCerrar(idRechazo: String) {
        viewModelScope.launch { rechazoRepository.cerrar(idRechazo) }
    }
}
