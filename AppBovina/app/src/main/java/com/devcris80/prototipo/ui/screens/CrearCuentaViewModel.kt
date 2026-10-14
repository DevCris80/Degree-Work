package com.devcris80.prototipo.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devcris80.prototipo.domain.usecase.CrearCuenta
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CrearCuentaUiState(
    val nombreGanadero: String = "",
    val nombreFinca: String = "",
) {
    val puedeCrear: Boolean get() = nombreGanadero.isNotBlank() && nombreFinca.isNotBlank()
}

sealed interface CrearCuentaEvento {
    data object CuentaCreada : CrearCuentaEvento
}

/** Estado de Crear cuenta: guarda la finca y su primer Usuario con el caso de uso CrearCuenta. */
class CrearCuentaViewModel(private val crearCuenta: CrearCuenta) : ViewModel() {
    private val _estado = MutableStateFlow(CrearCuentaUiState())
    val estado: StateFlow<CrearCuentaUiState> = _estado.asStateFlow()

    private val _eventos = Channel<CrearCuentaEvento>(Channel.BUFFERED)
    val eventos: Flow<CrearCuentaEvento> = _eventos.receiveAsFlow()

    fun onNombreGanaderoCambiado(valor: String) = _estado.update { it.copy(nombreGanadero = valor) }
    fun onNombreFincaCambiado(valor: String) = _estado.update { it.copy(nombreFinca = valor) }

    fun onCrearCuenta() {
        val actual = _estado.value
        if (!actual.puedeCrear) return
        viewModelScope.launch {
            crearCuenta(actual.nombreGanadero, actual.nombreFinca)
            _eventos.send(CrearCuentaEvento.CuentaCreada)
        }
    }
}
