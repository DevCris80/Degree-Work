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

sealed interface NuevoPesajeError {
    data object PesoFueraDeRango : NuevoPesajeError
    data object FechaFutura : NuevoPesajeError
    data object AnimalNoDisponible : NuevoPesajeError
}

data class NuevoPesajeUiState(
    /** El peso como se digita; acepta coma o punto decimal. */
    val peso: String = "",
    /** Fecha y hora de la medición. */
    val fecha: Long,
    val error: NuevoPesajeError? = null,
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

    fun onPesoCambiado(valor: String) = _estado.update { it.copy(peso = valor, error = null) }
    fun onFechaCambiada(fecha: Long) = _estado.update { it.copy(fecha = fecha, error = null) }

    fun onGuardar() {
        val actual = _estado.value
        if (!actual.puedeGuardar) return
        // Un texto que no es un número llega como NaN y el caso de uso lo rechaza por rango.
        val peso = actual.peso.trim().replace(',', '.').toFloatOrNull() ?: Float.NaN
        viewModelScope.launch {
            when (registrarPesajeManual(idAnimal, peso, actual.fecha)) {
                RegistrarPesajeManual.Resultado.Registrado -> _eventos.send(NuevoPesajeEvento.Guardado)
                RegistrarPesajeManual.Resultado.PesoFueraDeRango ->
                    _estado.update { it.copy(error = NuevoPesajeError.PesoFueraDeRango) }
                RegistrarPesajeManual.Resultado.FechaFutura ->
                    _estado.update { it.copy(error = NuevoPesajeError.FechaFutura) }
                RegistrarPesajeManual.Resultado.AnimalNoDisponible ->
                    _estado.update { it.copy(error = NuevoPesajeError.AnimalNoDisponible) }
            }
        }
    }
}
