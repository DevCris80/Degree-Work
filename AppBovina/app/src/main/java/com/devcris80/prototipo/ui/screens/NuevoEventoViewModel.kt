package com.devcris80.prototipo.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devcris80.prototipo.domain.model.Evento
import com.devcris80.prototipo.domain.repository.CuentaRepository
import com.devcris80.prototipo.domain.repository.EventoRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class NuevoEventoUiState(
    val tipoEvento: String = "",
    val detalle: String = "",
) {
    val puedeGuardar: Boolean get() = tipoEvento.isNotBlank() && detalle.isNotBlank()
}

sealed interface NuevoEventoEvento {
    data object Guardado : NuevoEventoEvento
}

/**
 * Estado del formulario de un Evento nuevo, que queda a nombre del Usuario del dispositivo.
 * Sin caso de uso: no hay regla de negocio (ver #22).
 */
class NuevoEventoViewModel(
    private val idAnimal: String,
    private val eventoRepository: EventoRepository,
    private val cuentaRepository: CuentaRepository,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {
    private val _estado = MutableStateFlow(NuevoEventoUiState())
    val estado: StateFlow<NuevoEventoUiState> = _estado.asStateFlow()

    private val _eventos = Channel<NuevoEventoEvento>(Channel.BUFFERED)
    val eventos: Flow<NuevoEventoEvento> = _eventos.receiveAsFlow()

    fun onTipoEventoCambiado(valor: String) = _estado.update { it.copy(tipoEvento = valor) }
    fun onDetalleCambiado(valor: String) = _estado.update { it.copy(detalle = valor) }

    fun onGuardar() {
        val actual = _estado.value
        if (!actual.puedeGuardar) return
        viewModelScope.launch {
            val idUsuario = cuentaRepository.getUsuario()?.idUsuario ?: return@launch
            eventoRepository.insert(
                Evento(
                    idEvento = UUID.randomUUID().toString(),
                    idAnimal = idAnimal,
                    idUsuario = idUsuario,
                    tipoEvento = actual.tipoEvento,
                    fecha = clock(),
                    detalle = actual.detalle,
                ),
            )
            _eventos.send(NuevoEventoEvento.Guardado)
        }
    }
}
