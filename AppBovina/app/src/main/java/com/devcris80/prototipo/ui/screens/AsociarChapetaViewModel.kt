package com.devcris80.prototipo.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.repository.AnimalRepository
import com.devcris80.prototipo.domain.repository.ChapetaRepository
import com.devcris80.prototipo.domain.usecase.AsignarPesajesPendientes
import com.devcris80.prototipo.domain.usecase.AsociarChapeta
import com.devcris80.prototipo.ui.components.OfertaPesajesPendientes
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AsociarChapetaUiState(
    /** El Animal elegido: viene del Detalle, o se elige de [candidatos] cuando se llega con un codigo leído. */
    val idAnimal: String? = null,
    val nombreAnimal: String? = null,
    /** El Animal ya viene fijo desde su Detalle: no se elige. */
    val animalFijo: Boolean = false,
    val codigo: String = "",
    val codigoSoloLectura: Boolean = false,
    /** Animales activos sin Chapeta activa, por nombre. */
    val candidatos: List<Animal> = emptyList(),
    val guardando: Boolean = false,
    val error: String? = null,
    val ofertaPesajesPendientes: OfertaPesajesPendientes? = null,
    val errorAsignacionPendientes: Boolean = false,
) {
    val puedeAsociar: Boolean get() = idAnimal != null && codigo.isNotBlank() && !guardando
}

sealed interface AsociarChapetaEvento {
    data class Asociada(val idAnimal: String) : AsociarChapetaEvento
}

/**
 * Estado de Asociar chapeta (CU-11, RF-19): el codigo (leído o digitado) y el Animal que lo recibe, y el
 * guardado con AsociarChapeta, que ofrece asignar los Pesajes pendientes del codigo.
 */
class AsociarChapetaViewModel(
    idAnimalFijo: String?,
    codigoPrecargado: String?,
    animalRepository: AnimalRepository,
    chapetaRepository: ChapetaRepository,
    private val asociarChapeta: AsociarChapeta,
    private val asignarPesajesPendientes: AsignarPesajesPendientes,
) : ViewModel() {
    private val _estado = MutableStateFlow(
        AsociarChapetaUiState(
            idAnimal = idAnimalFijo,
            animalFijo = idAnimalFijo != null,
            codigo = codigoPrecargado.orEmpty(),
            codigoSoloLectura = codigoPrecargado != null,
        ),
    )
    val estado: StateFlow<AsociarChapetaUiState> = _estado.asStateFlow()

    private val _eventos = Channel<AsociarChapetaEvento>(Channel.BUFFERED)
    val eventos: Flow<AsociarChapetaEvento> = _eventos.receiveAsFlow()

    init {
        if (idAnimalFijo != null) {
            viewModelScope.launch {
                val nombre = animalRepository.findById(idAnimalFijo)?.nombre
                _estado.update { it.copy(nombreAnimal = nombre) }
            }
        }
        viewModelScope.launch {
            combine(animalRepository.observeActivos(), chapetaRepository.observeActivas()) { animales, chapetas ->
                val conChapeta = chapetas.map { it.idAnimal }.toSet()
                animales.filter { it.idAnimal !in conChapeta }.sortedBy { it.nombre }
            }.collect { candidatos -> _estado.update { it.copy(candidatos = candidatos) } }
        }
    }

    fun onCodigoCambiado(valor: String) = _estado.update { it.copy(codigo = valor, error = null) }

    fun onAnimalSeleccionado(idAnimal: String) = _estado.update {
        it.copy(idAnimal = idAnimal, nombreAnimal = it.candidatos.firstOrNull { a -> a.idAnimal == idAnimal }?.nombre, error = null)
    }

    fun onErrorAsignacionPendientesVisto() = _estado.update { it.copy(errorAsignacionPendientes = false) }

    fun onAsociar() {
        val actual = _estado.value
        val idAnimal = actual.idAnimal
        if (!actual.puedeAsociar || idAnimal == null) return
        _estado.update { it.copy(guardando = true, error = null) }
        viewModelScope.launch {
            when (val resultado = asociarChapeta(idAnimal, actual.codigo)) {
                is AsociarChapeta.Resultado.Asociada -> {
                    if (resultado.pesajesPendientes.isEmpty()) {
                        _estado.update { it.copy(guardando = false) }
                        _eventos.send(AsociarChapetaEvento.Asociada(idAnimal))
                    } else {
                        _estado.update {
                            it.copy(
                                guardando = false,
                                ofertaPesajesPendientes = OfertaPesajesPendientes(
                                    idAnimal = idAnimal,
                                    nombreAnimal = it.nombreAnimal.orEmpty(),
                                    codigo = resultado.pesajesPendientes.first().codigo,
                                    pesajes = resultado.pesajesPendientes,
                                ),
                            )
                        }
                    }
                }
                is AsociarChapeta.Resultado.CodigoYaActivo ->
                    terminarConError("La chapeta ${resultado.codigo} ya está asociada a otro animal.")
                AsociarChapeta.Resultado.AnimalYaTieneChapeta ->
                    terminarConError("Este animal ya tiene una chapeta activa. Libérela antes de asociar otra.")
                AsociarChapeta.Resultado.AnimalNoDisponible -> terminarConError("Este animal ya no está disponible.")
                AsociarChapeta.Resultado.CodigoVacio -> terminarConError("Escriba o escanee el código de la chapeta.")
            }
        }
    }

    fun onOfertaPesajesPendientesRespondida(asignar: Boolean) {
        val oferta = _estado.value.ofertaPesajesPendientes ?: return
        _estado.update { it.copy(ofertaPesajesPendientes = null) }
        viewModelScope.launch {
            if (asignar) {
                when (asignarPesajesPendientes(oferta.pesajes.map { it.idPesajePendiente }, oferta.idAnimal)) {
                    AsignarPesajesPendientes.Resultado.Asignados -> Unit
                    AsignarPesajesPendientes.Resultado.AnimalNoDisponible,
                    AsignarPesajesPendientes.Resultado.PendienteNoDisponible,
                    -> _estado.update { it.copy(errorAsignacionPendientes = true) }
                }
            }
            _eventos.send(AsociarChapetaEvento.Asociada(oferta.idAnimal))
        }
    }

    private fun terminarConError(mensaje: String) = _estado.update { it.copy(guardando = false, error = mensaje) }
}
