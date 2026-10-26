package com.devcris80.prototipo.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Chapeta
import com.devcris80.prototipo.domain.model.EntidadConRevisiones
import com.devcris80.prototipo.domain.model.Evento
import com.devcris80.prototipo.domain.model.Registro
import com.devcris80.prototipo.domain.model.puedeAdministrar
import com.devcris80.prototipo.domain.repository.AnimalRepository
import com.devcris80.prototipo.domain.repository.ChapetaRepository
import com.devcris80.prototipo.domain.repository.ConflictoRepository
import com.devcris80.prototipo.domain.repository.CuentaRepository
import com.devcris80.prototipo.domain.repository.EventoRepository
import com.devcris80.prototipo.domain.repository.RegistroRepository
import com.devcris80.prototipo.domain.usecase.DarDeBajaAnimal
import com.devcris80.prototipo.domain.usecase.LiberarChapeta
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DetalleAnimalUiState(
    val animal: Animal? = null,
    val eventos: List<Evento> = emptyList(),
    val registros: List<Registro> = emptyList(),
    val chapetaActiva: Chapeta? = null,
    val padre: Animal? = null,
    val madre: Animal? = null,
    /** Si el Animal tiene algún Sibling sin resolver (RN-17). */
    val animalEnConflicto: Boolean = false,
    /** Los Eventos y Registros de su historial que están en Conflicto. */
    val eventosEnConflicto: Set<String> = emptySet(),
    val registrosEnConflicto: Set<String> = emptySet(),
    /** Dar de baja y liberar la Chapeta son del Ganadero; al Operario no se le ofrecen. */
    val puedeAdministrar: Boolean = false,
)

private data class FilasEnConflicto(val animal: Boolean, val eventos: Set<String>, val registros: Set<String>)

/** Estado del Detalle de Animal: el animal, su historial, su Chapeta activa y sus padres, si los tiene. */
@OptIn(ExperimentalCoroutinesApi::class)
class DetalleAnimalViewModel(
    private val idAnimal: String,
    private val animalRepository: AnimalRepository,
    eventoRepository: EventoRepository,
    registroRepository: RegistroRepository,
    chapetaRepository: ChapetaRepository,
    private val darDeBajaAnimal: DarDeBajaAnimal,
    private val liberarChapeta: LiberarChapeta,
    conflictoRepository: ConflictoRepository,
    cuentaRepository: CuentaRepository,
) : ViewModel() {
    private val enConflicto: Flow<FilasEnConflicto> = combine(
        conflictoRepository.observeEnConflicto(EntidadConRevisiones.ANIMAL, idAnimal),
        conflictoRepository.observeFilasEnConflicto(EntidadConRevisiones.EVENTO),
        conflictoRepository.observeFilasEnConflicto(EntidadConRevisiones.REGISTRO),
    ) { animal, eventos, registros -> FilasEnConflicto(animal, eventos, registros) }

    // El padre y la madre se vuelven a observar cada vez que cambia el Animal.
    private val animalConPadres: Flow<AnimalConPadres> = animalRepository.observeById(idAnimal).flatMapLatest { actual ->
        combine(observarSiHay(actual?.idPadre), observarSiHay(actual?.idMadre)) { padre, madre ->
            AnimalConPadres(actual, padre, madre)
        }
    }

    private val detalle: Flow<DetalleAnimalUiState> = combine(
        animalConPadres,
        eventoRepository.observeByAnimal(idAnimal),
        registroRepository.observeByAnimal(idAnimal),
        chapetaRepository.observeActivaByAnimal(idAnimal),
        enConflicto,
    ) { (animal, padre, madre), eventos, registros, chapeta, conflictos ->
        DetalleAnimalUiState(
            animal = animal,
            eventos = eventos,
            registros = registros,
            chapetaActiva = chapeta,
            padre = padre,
            madre = madre,
            animalEnConflicto = conflictos.animal,
            eventosEnConflicto = conflictos.eventos,
            registrosEnConflicto = conflictos.registros,
        )
    }

    val estado: StateFlow<DetalleAnimalUiState> = combine(
        detalle,
        cuentaRepository.observeUsuario(),
    ) { detalle, usuario -> detalle.copy(puedeAdministrar = usuario.puedeAdministrar) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetalleAnimalUiState())

    private fun observarSiHay(idAnimal: String?): Flow<Animal?> =
        idAnimal?.let(animalRepository::observeById) ?: flowOf(null)

    private data class AnimalConPadres(val animal: Animal?, val padre: Animal?, val madre: Animal?)

    fun onDarDeBaja() {
        viewModelScope.launch { darDeBajaAnimal(idAnimal) }
    }

    fun onLiberarChapeta(idChapeta: String) {
        viewModelScope.launch { liberarChapeta(idChapeta) }
    }
}
