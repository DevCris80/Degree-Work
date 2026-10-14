package com.devcris80.prototipo.ui.nav

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devcris80.prototipo.domain.repository.CuentaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Decide la pantalla inicial: Crear cuenta si no hay PerfilFinca todavía, Lista si ya hay uno. */
class InicioViewModel(cuentaRepository: CuentaRepository) : ViewModel() {
    val tienePerfil: StateFlow<Boolean?> = cuentaRepository.observePerfilFinca()
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
}
