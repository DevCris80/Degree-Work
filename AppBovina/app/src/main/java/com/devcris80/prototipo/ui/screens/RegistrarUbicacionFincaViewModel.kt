package com.devcris80.prototipo.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devcris80.prototipo.domain.repository.CuentaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegistrarUbicacionFincaUiState(
    val nombreFinca: String = "",
    val latitud: String = "",
    val longitud: String = "",
    val buscandoUbicacion: Boolean = false,
    val cargando: Boolean = false,
    val error: String? = null,
    val guardado: Boolean = false,
)

/**
 * Pantalla de Registrar ubicación de la finca (CU-23, RF-14): muestra la finca actual, permite editar
 * latitud y longitud (a mano o desde el GPS si se concede el permiso) y guarda en el perfil de la finca.
 */
class RegistrarUbicacionFincaViewModel(private val cuentaRepository: CuentaRepository) : ViewModel() {

    private val _estado = MutableStateFlow(RegistrarUbicacionFincaUiState())
    val estado: StateFlow<RegistrarUbicacionFincaUiState> = _estado.asStateFlow()

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

    fun onBuscandoUbicacion() = _estado.update { it.copy(buscandoUbicacion = true, error = null) }

    fun onUbicacionDelGps(latitud: Double, longitud: Double) = _estado.update {
        it.copy(
            latitud = latitud.toString(),
            longitud = longitud.toString(),
            buscandoUbicacion = false,
            error = null,
        )
    }

    fun onPermisoNegado() = _estado.update {
        it.copy(buscandoUbicacion = false, error = "Sin permiso de ubicación. Ingresa las coordenadas a mano.")
    }

    fun onUbicacionNoDisponible() = _estado.update {
        it.copy(
            buscandoUbicacion = false,
            error = "No se pudo obtener la posición del GPS. Actívalo e intenta de nuevo, o ingresa las coordenadas a mano.",
        )
    }

    fun onGuardarUbicacion() {
        val actual = _estado.value
        if (actual.latitud.isBlank() || actual.longitud.isBlank()) {
            _estado.update { it.copy(error = "Ingresa la latitud y la longitud.") }
            return
        }
        val latitud = actual.latitud.comoCoordenada()
        val longitud = actual.longitud.comoCoordenada()
        if (latitud == null || longitud == null) {
            _estado.update { it.copy(error = "Las coordenadas deben ser números, por ejemplo 4.6097 y -74.0817.") }
            return
        }
        if (latitud !in -90.0..90.0) {
            _estado.update { it.copy(error = "La latitud debe estar entre -90 y 90.") }
            return
        }
        if (longitud !in -180.0..180.0) {
            _estado.update { it.copy(error = "La longitud debe estar entre -180 y 180.") }
            return
        }
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            val perfilActual = cuentaRepository.getPerfilFinca()
            if (perfilActual == null) {
                _estado.update { it.copy(cargando = false, error = "No hay un perfil de finca guardado.") }
                return@launch
            }
            cuentaRepository.guardarPerfilFinca(perfilActual.copy(latitud = latitud, longitud = longitud))
            _estado.update { it.copy(cargando = false, guardado = true) }
        }
    }
}

// Acepta la coma decimal del teclado en español; NaN e infinito no son coordenadas.
private fun String.comoCoordenada(): Double? =
    trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
