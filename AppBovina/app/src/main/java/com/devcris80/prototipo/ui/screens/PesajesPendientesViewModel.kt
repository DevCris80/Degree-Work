package com.devcris80.prototipo.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.PesajePendiente
import com.devcris80.prototipo.domain.repository.AnimalRepository
import com.devcris80.prototipo.domain.model.puedeAdministrar
import com.devcris80.prototipo.domain.repository.ChapetaRepository
import com.devcris80.prototipo.domain.repository.CuentaRepository
import com.devcris80.prototipo.domain.repository.PesajePendienteRepository
import com.devcris80.prototipo.domain.usecase.AsignarPesajesPendientes
import com.devcris80.prototipo.domain.usecase.DescartarPesajePendiente
import com.devcris80.prototipo.domain.usecase.ObservarPesajesPendientes
import com.devcris80.prototipo.domain.usecase.PesajePendientePorConciliar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AnimalConCodigo(val animal: Animal, val codigoChapeta: String?)

sealed interface PesajesPendientesError {
    data object AnimalNoDisponible : PesajesPendientesError
    data object PendienteNoDisponible : PesajesPendientesError
}

data class OfertaRestantes(val codigo: String, val idAnimal: String, val idsPesajePendiente: List<String>) {
    val cantidad: Int get() = idsPesajePendiente.size
}

data class PesajesPendientesUiState(
    val pendientes: List<PesajePendientePorConciliar> = emptyList(),
    val animalesActivos: List<AnimalConCodigo> = emptyList(),
    val error: PesajesPendientesError? = null,
    val ofertaRestantes: OfertaRestantes? = null,
    /** Descartar un Pesaje pendiente es del Ganadero; el Operario solo puede asignarlo. */
    val puedeDescartar: Boolean = false,
)

/** Estado de Pesajes pendientes: la lista sin resolver con el estado de su codigo, y sus acciones. */
class PesajesPendientesViewModel(
    observarPesajesPendientes: ObservarPesajesPendientes,
    animalRepository: AnimalRepository,
    chapetaRepository: ChapetaRepository,
    private val pesajePendienteRepository: PesajePendienteRepository,
    private val asignarPesajesPendientes: AsignarPesajesPendientes,
    private val descartarPesajePendiente: DescartarPesajePendiente,
    cuentaRepository: CuentaRepository,
) : ViewModel() {
    private val error = MutableStateFlow<PesajesPendientesError?>(null)
    private val ofertaRestantes = MutableStateFlow<OfertaRestantes?>(null)

    private val animalesConCodigo = combine(
        animalRepository.observeActivos(),
        chapetaRepository.observeActivas(),
    ) { animales, chapetas ->
        val codigoPorAnimal = chapetas.associateBy({ it.idAnimal }, { it.codigo })
        animales.map { AnimalConCodigo(it, codigoPorAnimal[it.idAnimal]) }
    }

    val estado: StateFlow<PesajesPendientesUiState> = combine(
        observarPesajesPendientes(),
        animalesConCodigo,
        error,
        ofertaRestantes,
        cuentaRepository.observeUsuario(),
    ) { pendientes, animales, errorActual, oferta, usuario ->
        PesajesPendientesUiState(pendientes, animales, errorActual, oferta, usuario.puedeAdministrar)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PesajesPendientesUiState())

    fun onConfirmarAsignar(pendiente: PesajePendiente, idAnimal: String) {
        viewModelScope.launch {
            when (asignarPesajesPendientes(listOf(pendiente.idPesajePendiente), idAnimal)) {
                AsignarPesajesPendientes.Resultado.Asignados -> {
                    val otrosDelMismoCodigo = pesajePendienteRepository.findSinResolverByCodigo(pendiente.codigo)
                    if (otrosDelMismoCodigo.isNotEmpty()) {
                        ofertaRestantes.value = OfertaRestantes(
                            pendiente.codigo,
                            idAnimal,
                            otrosDelMismoCodigo.map { it.idPesajePendiente },
                        )
                    }
                }
                AsignarPesajesPendientes.Resultado.AnimalNoDisponible ->
                    error.value = PesajesPendientesError.AnimalNoDisponible
                AsignarPesajesPendientes.Resultado.PendienteNoDisponible ->
                    error.value = PesajesPendientesError.PendienteNoDisponible
            }
        }
    }

    fun onConfirmarDescartar(idPesajePendiente: String) {
        viewModelScope.launch {
            when (descartarPesajePendiente(idPesajePendiente)) {
                // La pantalla no ofrece Descartar al Operario; si igual llega, el caso de uso no escribe.
                DescartarPesajePendiente.Resultado.Descartado,
                DescartarPesajePendiente.Resultado.NoPermitido,
                -> Unit
                DescartarPesajePendiente.Resultado.PendienteNoDisponible ->
                    error.value = PesajesPendientesError.PendienteNoDisponible
            }
        }
    }

    fun onAsignarRestantesConfirmado() {
        val oferta = ofertaRestantes.value ?: return
        ofertaRestantes.value = null
        viewModelScope.launch {
            when (asignarPesajesPendientes(oferta.idsPesajePendiente, oferta.idAnimal)) {
                AsignarPesajesPendientes.Resultado.Asignados -> Unit
                AsignarPesajesPendientes.Resultado.AnimalNoDisponible ->
                    error.value = PesajesPendientesError.AnimalNoDisponible
                AsignarPesajesPendientes.Resultado.PendienteNoDisponible ->
                    error.value = PesajesPendientesError.PendienteNoDisponible
            }
        }
    }

    fun onAsignarRestantesDescartado() {
        ofertaRestantes.value = null
    }

    fun onErrorVisto() {
        error.value = null
    }
}
