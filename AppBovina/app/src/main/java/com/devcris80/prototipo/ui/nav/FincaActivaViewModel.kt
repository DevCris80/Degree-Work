package com.devcris80.prototipo.ui.nav

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devcris80.prototipo.domain.repository.CuentaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * La finca de la cuenta con la sesión abierta, para mostrarla en la barra de navegación (ADR-0006): con varias cuentas
 * en el celular, y los pesajes del ESP32 yendo a la abierta, quien trabaja debe ver siempre en cuál está.
 */
class FincaActivaViewModel(cuentaRepository: CuentaRepository) : ViewModel() {
    val nombreFinca: StateFlow<String?> = cuentaRepository.observePerfilFinca()
        .map { it?.nombreFinca }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
