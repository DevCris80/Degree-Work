package com.devcris80.prototipo.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devcris80.prototipo.domain.model.puedeAdministrar
import com.devcris80.prototipo.domain.repository.CuentaRepository
import com.devcris80.prototipo.domain.repository.RegistroRepository
import com.devcris80.prototipo.domain.usecase.ActualizarRegistro
import com.devcris80.prototipo.domain.usecase.DarDeBajaRegistro
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditarPesajeUiState(
    /** El peso como se digita: solo dígitos y un punto decimal, sin separador de miles. */
    val peso: String = "",
    val fecha: Long = 0L,
    /** El pesaje ya se leyó de la base: hasta entonces no hay nada que guardar. */
    val cargado: Boolean = false,
    val error: NuevoPesajeError? = null,
    /** Verdadero desde que se pide guardar o dar de baja hasta que falla: evita repetirlo con un doble toque. */
    val guardando: Boolean = false,
    /** Dar de baja es del Ganadero; al Operario no se le ofrece (matriz 2.1). */
    val puedeDarDeBaja: Boolean = false,
) {
    val puedeGuardar: Boolean get() = cargado && peso.isNotBlank() && !guardando
}

sealed interface EditarPesajeEvento {
    data object Guardado : EditarPesajeEvento
    data object DadoDeBaja : EditarPesajeEvento
}

/**
 * Estado del formulario Editar pesaje (CU-18, CU-19): parte del peso y la hora guardados, se carga una sola vez para
 * no pisar lo que la persona va escribiendo, guarda con ActualizarRegistro y da de baja con DarDeBajaRegistro.
 */
class EditarPesajeViewModel(
    private val idRegistro: String,
    registroRepository: RegistroRepository,
    cuentaRepository: CuentaRepository,
    private val actualizarRegistro: ActualizarRegistro,
    private val darDeBajaRegistro: DarDeBajaRegistro,
) : ViewModel() {
    private val _estado = MutableStateFlow(EditarPesajeUiState())
    val estado: StateFlow<EditarPesajeUiState> = _estado.asStateFlow()

    private val _eventos = Channel<EditarPesajeEvento>(Channel.BUFFERED)
    val eventos: Flow<EditarPesajeEvento> = _eventos.receiveAsFlow()

    init {
        viewModelScope.launch {
            val registro = registroRepository.findById(idRegistro)
            if (registro == null || registro.fechaBaja != null) {
                _estado.update { it.copy(error = NuevoPesajeError.PesajeNoDisponible) }
            } else {
                _estado.update {
                    it.copy(peso = registro.peso.comoTexto(), fecha = registro.timestamp, cargado = true)
                }
            }
        }
        viewModelScope.launch {
            cuentaRepository.observeUsuario().collect { usuario ->
                _estado.update { it.copy(puedeDarDeBaja = usuario.puedeAdministrar) }
            }
        }
    }

    fun onPesoCambiado(valor: String) {
        if (!FORMATO_PESO.matches(valor)) return
        _estado.update { it.copy(peso = valor, error = null) }
    }

    fun onFechaCambiada(fecha: Long) = _estado.update { it.copy(fecha = fecha, error = null) }

    fun onGuardar() {
        val actual = _estado.value
        if (!actual.puedeGuardar) return
        _estado.update { it.copy(guardando = true) }
        // Un "." solo no es un número: llega como NaN y el caso de uso lo rechaza por rango.
        val peso = actual.peso.toFloatOrNull() ?: Float.NaN
        viewModelScope.launch {
            val error = when (actualizarRegistro(idRegistro, peso, actual.fecha)) {
                ActualizarRegistro.Resultado.Actualizado -> null
                ActualizarRegistro.Resultado.PesoFueraDeRango -> NuevoPesajeError.PesoFueraDeRango
                ActualizarRegistro.Resultado.FechaFutura -> NuevoPesajeError.FechaFutura
                ActualizarRegistro.Resultado.NoDisponible -> NuevoPesajeError.PesajeNoDisponible
            }
            if (error == null) {
                _eventos.send(EditarPesajeEvento.Guardado)
            } else {
                _estado.update { it.copy(error = error, guardando = false) }
            }
        }
    }

    fun onDarDeBaja() {
        val actual = _estado.value
        if (!actual.cargado || actual.guardando || !actual.puedeDarDeBaja) return
        _estado.update { it.copy(guardando = true) }
        viewModelScope.launch {
            when (darDeBajaRegistro(idRegistro)) {
                DarDeBajaRegistro.Resultado.Realizado -> _eventos.send(EditarPesajeEvento.DadoDeBaja)
                DarDeBajaRegistro.Resultado.NoPermitido ->
                    _estado.update { it.copy(guardando = false, puedeDarDeBaja = false) }
                DarDeBajaRegistro.Resultado.NoDisponible ->
                    _estado.update { it.copy(guardando = false, error = NuevoPesajeError.PesajeNoDisponible) }
            }
        }
    }

    private fun Float.comoTexto(): String = if (this % 1f == 0f) toInt().toString() else toString()

    private companion object {
        val FORMATO_PESO = Regex("\\d*\\.?\\d*")
    }
}
