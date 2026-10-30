package com.devcris80.prototipo.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devcris80.prototipo.domain.usecase.RegistrarPesajeManual
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** El peso como se digita en un formulario de pesaje: solo dígitos y un punto decimal, sin separador de miles. */
internal val FORMATO_PESO = Regex("\\d*\\.?\\d*")

sealed interface PesajeError {
    data object PesoFueraDeRango : PesajeError
    data object FechaFutura : PesajeError
    data object AnimalNoDisponible : PesajeError

    /** Solo al editar: el pesaje no existe o ya está dado de baja. */
    data object PesajeNoDisponible : PesajeError
}

data class NuevoPesajeUiState(
    /** El peso como se digita: solo dígitos y un punto decimal, sin separador de miles. */
    val peso: String = "",
    /** Fecha y hora del pesaje. */
    val fecha: Long,
    val error: PesajeError? = null,
    /** Verdadero desde que se pide guardar hasta que falla: evita guardar dos veces con un doble toque. */
    val guardando: Boolean = false,
) {
    val puedeGuardar: Boolean get() = peso.isNotBlank()
}

sealed interface NuevoPesajeEvento {
    data object Guardado : NuevoPesajeEvento
}

/** Estado del formulario de un pesaje manual de un Animal; la fecha empieza en ahora. */
class NuevoPesajeViewModel(
    private val idAnimal: String,
    private val registrarPesajeManual: RegistrarPesajeManual,
    clock: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {
    private val _estado = MutableStateFlow(NuevoPesajeUiState(fecha = clock()))
    val estado: StateFlow<NuevoPesajeUiState> = _estado.asStateFlow()

    private val _eventos = Channel<NuevoPesajeEvento>(Channel.BUFFERED)
    val eventos: Flow<NuevoPesajeEvento> = _eventos.receiveAsFlow()

    fun onPesoCambiado(valor: String) {
        if (!FORMATO_PESO.matches(valor)) return
        _estado.update { it.copy(peso = valor, error = null) }
    }

    fun onFechaCambiada(fecha: Long) = _estado.update { it.copy(fecha = fecha, error = null) }

    fun onGuardar() {
        val actual = _estado.value
        if (!actual.puedeGuardar || actual.guardando) return
        _estado.update { it.copy(guardando = true) }
        // Un "." solo no es un número: llega como NaN y el caso de uso lo rechaza por rango.
        val peso = actual.peso.toFloatOrNull() ?: Float.NaN
        viewModelScope.launch {
            val error = when (registrarPesajeManual(idAnimal, peso, actual.fecha)) {
                RegistrarPesajeManual.Resultado.Registrado -> null
                RegistrarPesajeManual.Resultado.PesoFueraDeRango -> PesajeError.PesoFueraDeRango
                RegistrarPesajeManual.Resultado.FechaFutura -> PesajeError.FechaFutura
                RegistrarPesajeManual.Resultado.AnimalNoDisponible -> PesajeError.AnimalNoDisponible
            }
            if (error == null) {
                _eventos.send(NuevoPesajeEvento.Guardado)
            } else {
                _estado.update { it.copy(error = error, guardando = false) }
            }
        }
    }
}
