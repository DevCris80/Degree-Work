package com.devcris80.prototipo.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devcris80.prototipo.domain.model.Conflicto
import com.devcris80.prototipo.domain.model.Eleccion
import com.devcris80.prototipo.domain.model.EntidadConRevisiones
import com.devcris80.prototipo.domain.repository.ConflictoRepository
import com.devcris80.prototipo.domain.usecase.ResolverConflicto
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ResolverConflictoUiState(
    /** El Conflicto como estaba al abrir la pantalla: es lo que la persona ve y lo que se resuelve. */
    val conflicto: Conflicto? = null,
    val eleccion: Eleccion? = null,
    /** Llegaron Siblings después de abrir: no se resuelven con esta elección. */
    val hayNuevos: Boolean = false,
    /** La fila o la edición elegida desaparecieron antes de resolver: no se escribió nada. */
    val yaNoExiste: Boolean = false,
) {
    val puedeResolver: Boolean get() = conflicto != null && eleccion != null && !yaNoExiste
}

sealed interface ResolverConflictoEvento {
    data object Resuelto : ResolverConflictoEvento
}

/**
 * Estado de la pantalla Resolver Conflicto. Guarda el Conflicto de la primera vez que lo ve y resuelve solo esos
 * Siblings: uno que llegue después queda sin resolver (ADR-0004, regla 5).
 */
class ResolverConflictoViewModel(
    entidad: EntidadConRevisiones,
    idFila: String,
    conflictoRepository: ConflictoRepository,
    private val resolverConflicto: ResolverConflicto,
) : ViewModel() {
    private val _estado = MutableStateFlow(ResolverConflictoUiState())
    val estado: StateFlow<ResolverConflictoUiState> = _estado.asStateFlow()

    private val _eventos = Channel<ResolverConflictoEvento>(Channel.BUFFERED)
    val eventos: Flow<ResolverConflictoEvento> = _eventos.receiveAsFlow()

    init {
        viewModelScope.launch {
            conflictoRepository.observeConflicto(entidad, idFila).collect { actual ->
                _estado.update { estado ->
                    val abierto = estado.conflicto ?: actual
                    val vistos = abierto?.siblings.orEmpty().map { it.sibling.idVersion }.toSet()
                    estado.copy(
                        conflicto = abierto,
                        hayNuevos = actual?.siblings.orEmpty().any { it.sibling.idVersion !in vistos },
                    )
                }
            }
        }
    }

    fun onEleccionCambiada(eleccion: Eleccion) = _estado.update { it.copy(eleccion = eleccion) }

    fun onResolver() {
        val conflicto = _estado.value.conflicto ?: return
        val eleccion = _estado.value.eleccion ?: return
        viewModelScope.launch {
            when (resolverConflicto(conflicto, eleccion)) {
                ResolverConflicto.Resultado.Resuelto -> _eventos.send(ResolverConflictoEvento.Resuelto)
                ResolverConflicto.Resultado.YaNoExiste -> _estado.update { it.copy(yaNoExiste = true) }
            }
        }
    }
}
