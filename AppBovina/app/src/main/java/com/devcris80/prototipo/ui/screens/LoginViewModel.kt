package com.devcris80.prototipo.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devcris80.prototipo.domain.model.Usuario
import com.devcris80.prototipo.domain.repository.CuentaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Estado de Iniciar sesión: el Usuario de este dispositivo, si ya existe una cuenta creada. */
class LoginViewModel(cuentaRepository: CuentaRepository) : ViewModel() {
    val usuario: StateFlow<Usuario?> = cuentaRepository.observeUsuario()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
