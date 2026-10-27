package com.devcris80.prototipo.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devcris80.prototipo.domain.repository.AnimalRepository
import com.devcris80.prototipo.domain.repository.EventoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Un Evento tal como lo muestra la lista de la pestaña Eventos. */
data class EventoItemUi(
    val idEvento: String,
    val tipo: String,
    val detalle: String,
    val nombreAnimal: String,
    val fecha: Long,
    /** Su fecha todavía no llega. */
    val programado: Boolean,
)

data class EventosUiState(
    val eventos: List<EventoItemUi> = emptyList(),
)

/**
 * Estado de la pestaña Eventos: los Eventos de los Animales activos. Primero los programados, el más próximo
 * antes; después los que ya ocurrieron, del más reciente al más antiguo. Sin caso de uso: no hay regla de negocio.
 */
class EventosViewModel(
    eventoRepository: EventoRepository,
    animalRepository: AnimalRepository,
    clock: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {
    val estado: StateFlow<EventosUiState> = combine(
        eventoRepository.observeActivos(),
        animalRepository.observeActivos(),
    ) { eventos, animales ->
        val ahora = clock()
        val nombres = animales.associate { it.idAnimal to it.nombre }
        val items = eventos.mapNotNull { evento ->
            nombres[evento.idAnimal]?.let { nombre ->
                EventoItemUi(
                    idEvento = evento.idEvento,
                    tipo = evento.tipoEvento,
                    detalle = evento.detalle,
                    nombreAnimal = nombre,
                    fecha = evento.fecha,
                    programado = evento.estaProgramado(ahora),
                )
            }
        }
        val (programados, pasados) = items.partition { it.programado }
        EventosUiState(programados.sortedBy { it.fecha } + pasados.sortedByDescending { it.fecha })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EventosUiState())
}
