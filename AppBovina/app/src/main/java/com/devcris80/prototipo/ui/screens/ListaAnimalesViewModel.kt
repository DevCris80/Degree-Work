package com.devcris80.prototipo.ui.screens

import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.Channel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.model.Registro
import com.devcris80.prototipo.domain.repository.AnimalRepository
import com.devcris80.prototipo.domain.repository.ChapetaRepository
import com.devcris80.prototipo.domain.repository.CuentaRepository
import com.devcris80.prototipo.domain.repository.RegistroRepository
import com.devcris80.prototipo.domain.repository.SesionRepository
import com.devcris80.prototipo.domain.usecase.ObservarPesajesPendientes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class AnimalConResumen(
    val animal: Animal,
    val codigoChapeta: String?,
    val ultimoRegistro: Registro?,
)

data class ListaAnimalesUiState(
    val nombreFinca: String = "",
    val nombreUsuario: String = "",
    val animales: List<AnimalConResumen> = emptyList(),
    val filtro: Proposito? = null,
    val pesajesPendientes: Int = 0,
)

sealed interface ListaAnimalesEvento {
    data object SesionCerrada : ListaAnimalesEvento
}

/** Estado de la Lista de Animales: finca, usuario, animales activos con su Chapeta y último Registro. */
@OptIn(ExperimentalCoroutinesApi::class)
class ListaAnimalesViewModel(
    cuentaRepository: CuentaRepository,
    animalRepository: AnimalRepository,
    chapetaRepository: ChapetaRepository,
    registroRepository: RegistroRepository,
    observarPesajesPendientes: ObservarPesajesPendientes,
    private val sesionRepository: SesionRepository,
) : ViewModel() {
    private val _eventos = Channel<ListaAnimalesEvento>(Channel.BUFFERED)
    val eventos: Flow<ListaAnimalesEvento> = _eventos.receiveAsFlow()

    private val filtro = MutableStateFlow<Proposito?>(null)

    private val animalesConResumen = combine(
        animalRepository.observeActivos(),
        chapetaRepository.observeActivas(),
    ) { animales, chapetas -> animales to chapetas }
        .flatMapLatest { (animales, chapetas) ->
            val codigoPorAnimal = chapetas.associateBy({ it.idAnimal }, { it.codigo })
            if (animales.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(
                    animales.map { animal ->
                        registroRepository.observeUltimoByAnimal(animal.idAnimal).map { registro ->
                            AnimalConResumen(animal, codigoPorAnimal[animal.idAnimal], registro)
                        }
                    },
                ) { it.toList() }
            }
        }

    val estado: StateFlow<ListaAnimalesUiState> = combine(
        cuentaRepository.observePerfilFinca(),
        cuentaRepository.observeUsuario(),
        animalesConResumen,
        filtro,
        observarPesajesPendientes(),
    ) { perfil, usuario, animales, filtroActual, pendientes ->
        ListaAnimalesUiState(
            nombreFinca = perfil?.nombreFinca.orEmpty(),
            nombreUsuario = usuario?.nombre.orEmpty(),
            animales = animales.filter { filtroActual == null || it.animal.proposito == filtroActual },
            filtro = filtroActual,
            pesajesPendientes = pendientes.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListaAnimalesUiState())

    fun onFiltroSeleccionado(proposito: Proposito?) {
        filtro.value = proposito
    }

    fun onCerrarSesion() {
        viewModelScope.launch {
            sesionRepository.cerrarSesion()
            _eventos.send(ListaAnimalesEvento.SesionCerrada)
        }
    }
}
