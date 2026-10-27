package com.devcris80.prototipo.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devcris80.prototipo.domain.model.PerfilFinca
import com.devcris80.prototipo.domain.repository.CuentaRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegistrarUbicacionFincaUiState(
    val nombreFinca: String = "",
    val latitud: String = "",
    val longitud: String = "",
    val cargando: Boolean = false,
    val error: String? = null,
    val guardado: Boolean = false,
)

sealed interface RegistrarUbicacionFincaEvento {
    data object UbicacionGuardada : RegistrarUbicacionFincaEvento
}

/**
 * Pantalla de Registrar ubicación de la finca (CU-23, RF-14): muestra la finca actual, permite editar
 * latitud y longitud (a mano o desde el GPS si se concede el permiso) y guarda en el perfil de la finca.
 */
class RegistrarUbicacionFincaViewModel(private val cuentaRepository: CuentaRepository) : ViewModel() {

    private val _estado = MutableStateFlow(RegistrarUbicacionFincaUiState())
    val estado: StateFlow<RegistrarUbicacionFincaUiState> = _estado.asStateFlow()

    private val _eventos = Channel<RegistrarUbicacionFincaEvento>(Channel.BUFFERED)
    val eventos: Flow<RegistrarUbicacionFincaEvento> = _eventos.receiveAsFlow()

    init {
        viewModelScope.launch {
            val perfil = cuentaRepository.getPerfilFinca()
            if (perfil != null) {
                _estado.update {
                    it.copy(
                        nombreFinca = perfil.nombreFinca,
                        latitud = perfil.latitud?.toString().orEmpty(),
                        longitud = perfil.longitud?.toString().orEmpty(),
                    )
                }
            }
        }
    }

    fun onLatitudCambiada(valor: String) = _estado.update { it.copy(latitud = valor, error = null) }
    fun onLongitudCambiada(valor: String) = _estado.update { it.copy(longitud = valor, error = null) }

    fun onUbicacionDelGps(latitud: Double, longitud: Double) = _estado.update {
        it.copy(latitud = latitud.toString(), longitud = longitud.toString(), error = null)
    }

    fun onGuardarUbicacion() {
        val actual = _estado.value
        if (actual.latitud.isBlank() && actual.longitud.isBlank()) {
            _estado.update { it.copy(error = "Ingresa al menos latitud o longitud.") }
            return
        }
        val latitud = actual.latitud.toDoubleOrNull()
        val longitud = actual.longitud.toDoubleOrNull()
        if ((actual.latitud.isNotBlank() && latitud == null) ||
            (actual.longitud.isNotBlank() && longitud == null)
        ) {
            _estado.update { it.copy(error = "Coordenadas inválidas.") }
            return
        }
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            val perfilActual = cuentaRepository.getPerfilFinca()
            if (perfilActual == null) {
                _estado.update { it.copy(cargando = false, error = "No hay un perfil de finca guardado.") }
                return@launch
            }
            val perfil = perfilActual.copy(latitud = latitud, longitud = longitud)
            cuentaRepository.guardarPerfilFinca(perfil)
            _estado.update { it.copy(cargando = false, guardado = true) }
            _eventos.send(RegistrarUbicacionFincaEvento.UbicacionGuardada)
        }
    }
}
