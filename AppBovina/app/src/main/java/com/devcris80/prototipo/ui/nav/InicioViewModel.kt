package com.devcris80.prototipo.ui.nav

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devcris80.prototipo.domain.repository.CuentaRepository
import com.devcris80.prototipo.domain.repository.SesionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take

/**
 * Decide la pantalla de inicio al abrir la app: se entra sin iniciar sesión solo si hay una sesión
 * guardada y el Usuario ya está en el dispositivo. Nulo mientras no se sabe. La decisión no cambia
 * después: iniciar y cerrar sesión navegan por su cuenta.
 */
class InicioViewModel(cuentaRepository: CuentaRepository, private val sesionRepository: SesionRepository) : ViewModel() {
    val puedeEntrar: StateFlow<Boolean?> = combine(
        sesionRepository.observeSesionGuardada(),
        cuentaRepository.observeUsuario(),
    ) { sesion, usuario -> sesion && usuario != null }
        .take(1)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /** Avisa cada vez que la sesión guardada desaparece: al cerrar sesión o si el servidor rechaza renovarla. */
    val sesionPerdida: Flow<Unit> = sesionRepository.observeSesionGuardada()
        .distinctUntilChanged()
        .drop(1)
        .filter { guardada -> !guardada }
        .map { }
}
