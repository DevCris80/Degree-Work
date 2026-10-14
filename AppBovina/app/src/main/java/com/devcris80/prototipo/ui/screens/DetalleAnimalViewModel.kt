package com.devcris80.prototipo.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Chapeta
import com.devcris80.prototipo.domain.model.Evento
import com.devcris80.prototipo.domain.model.Registro
import com.devcris80.prototipo.domain.repository.AnimalRepository
import com.devcris80.prototipo.domain.repository.ChapetaRepository
import com.devcris80.prototipo.domain.repository.EventoRepository
import com.devcris80.prototipo.domain.repository.RegistroRepository
import com.devcris80.prototipo.domain.usecase.DarDeBajaAnimal
import com.devcris80.prototipo.domain.usecase.LiberarChapeta
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetalleAnimalUiState(
    val animal: Animal? = null,
    val eventos: List<Evento> = emptyList(),
    val registros: List<Registro> = emptyList(),
    val chapetaActiva: Chapeta? = null,
)

/** Estado del Detalle de Animal: el animal, su historial y su Chapeta activa, si tiene. */
class DetalleAnimalViewModel(
    private val idAnimal: String,
    animalRepository: AnimalRepository,
    eventoRepository: EventoRepository,
    registroRepository: RegistroRepository,
    chapetaRepository: ChapetaRepository,
    private val darDeBajaAnimal: DarDeBajaAnimal,
    private val liberarChapeta: LiberarChapeta,
) : ViewModel() {
    val estado: StateFlow<DetalleAnimalUiState> = combine(
        animalRepository.observeById(idAnimal),
        eventoRepository.observeByAnimal(idAnimal),
        registroRepository.observeByAnimal(idAnimal),
        chapetaRepository.observeActivaByAnimal(idAnimal),
    ) { animal, eventos, registros, chapeta ->
        DetalleAnimalUiState(animal, eventos, registros, chapeta)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleAnimalUiState())

    fun onDarDeBaja() {
        viewModelScope.launch { darDeBajaAnimal(idAnimal) }
    }

    fun onLiberarChapeta(idChapeta: String) {
        viewModelScope.launch { liberarChapeta(idChapeta) }
    }
}
