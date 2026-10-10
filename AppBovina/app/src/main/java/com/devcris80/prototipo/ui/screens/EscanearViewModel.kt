package com.devcris80.prototipo.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devcris80.prototipo.domain.usecase.DestinoEscaneo
import com.devcris80.prototipo.domain.usecase.LiberarChapeta
import com.devcris80.prototipo.domain.usecase.ResolverDestinoEscaneo
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface EscanearUiState {
    data object EsperandoTag : EscanearUiState
    data object Resolviendo : EscanearUiState
    data class AnimalDadoDeBaja(val idChapeta: String, val nombreAnimal: String, val codigo: String) : EscanearUiState
}

sealed interface EscanearEvento {
    data class AbrirAnimal(val idAnimal: String) : EscanearEvento
    data class Registrar(val codigo: String, val aviso: String?) : EscanearEvento
}

/**
 * Estado de la pantalla Escanear. La lectura NFC vive en la pantalla (depende de la Activity);
 * aquí solo se decide qué hacer con el codigo leído (ver #31 y #36).
 */
class EscanearViewModel(
    private val resolverDestinoEscaneo: ResolverDestinoEscaneo,
    private val liberarChapeta: LiberarChapeta,
) : ViewModel() {
    private val _estado = MutableStateFlow<EscanearUiState>(EscanearUiState.EsperandoTag)
    val estado: StateFlow<EscanearUiState> = _estado.asStateFlow()

    private val _eventos = Channel<EscanearEvento>(Channel.BUFFERED)
    val eventos: Flow<EscanearEvento> = _eventos.receiveAsFlow()

    fun onCodigoLeido(codigo: String) {
        _estado.value = EscanearUiState.Resolviendo
        viewModelScope.launch { resolver(codigo) }
    }

    fun onLiberarChapeta() {
        val actual = _estado.value as? EscanearUiState.AnimalDadoDeBaja ?: return
        _estado.value = EscanearUiState.Resolviendo
        viewModelScope.launch {
            liberarChapeta(actual.idChapeta)
            resolver(actual.codigo)
        }
    }

    fun onCancelar() {
        _estado.value = EscanearUiState.EsperandoTag
    }

    private suspend fun resolver(codigo: String) {
        when (val destino = resolverDestinoEscaneo(codigo)) {
            is DestinoEscaneo.AbrirAnimal -> volverAEsperarYEmitir(EscanearEvento.AbrirAnimal(destino.idAnimal))
            is DestinoEscaneo.AnimalDadoDeBaja -> _estado.value = EscanearUiState.AnimalDadoDeBaja(
                destino.idChapeta,
                destino.nombreAnimal,
                destino.codigo,
            )
            is DestinoEscaneo.RegistrarConAviso -> volverAEsperarYEmitir(
                EscanearEvento.Registrar(destino.codigo, "Este tag estuvo asociado a ${destino.nombreAnimalAnterior}"),
            )
            is DestinoEscaneo.RegistrarNuevo -> volverAEsperarYEmitir(EscanearEvento.Registrar(destino.codigo, null))
        }
    }

    private suspend fun volverAEsperarYEmitir(evento: EscanearEvento) {
        _estado.value = EscanearUiState.EsperandoTag
        _eventos.send(evento)
    }
}
