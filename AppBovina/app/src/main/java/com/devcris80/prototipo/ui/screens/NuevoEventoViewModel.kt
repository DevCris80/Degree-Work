package com.devcris80.prototipo.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.TipoEvento
import com.devcris80.prototipo.domain.model.fechaEstaProgramada
import com.devcris80.prototipo.domain.repository.AnimalRepository
import com.devcris80.prototipo.domain.usecase.RegistrarEvento
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Las vías de aplicación que ofrece el formulario; lo que se guarda es el texto. */
val VIAS_DE_APLICACION = listOf("Subcutánea", "Intramuscular", "Intravenosa", "Oral", "Tópica")

sealed interface NuevoEventoError {
    data object AnimalNoDisponible : NuevoEventoError
    data object CriaNoValida : NuevoEventoError
    data object CriaConOtroProgenitor : NuevoEventoError
    data object FaltanDatos : NuevoEventoError
    data object SinUsuario : NuevoEventoError
}

data class NuevoEventoUiState(
    val tipo: TipoEvento = TipoEvento.VACUNACION,
    /** Título del Evento: se guarda como su detalle. */
    val titulo: String = "",
    /** Fecha y hora del Evento; pasada o futura. */
    val fecha: Long,
    /** El momento en que se abrió el formulario, para saber si [fecha] todavía no llega. */
    val ahora: Long,
    val idAnimal: String? = null,
    val animales: List<Animal> = emptyList(),
    val biologico: String = "",
    val dosis: String = "",
    val via: String? = null,
    val idCria: String? = null,
    val error: NuevoEventoError? = null,
    /** Verdadero desde que se pide guardar hasta que falla: evita guardar dos veces con un doble toque. */
    val guardando: Boolean = false,
) {
    val animal: Animal? get() = animales.firstOrNull { it.idAnimal == idAnimal }

    /** Un Evento programado: su fecha todavía no llega. */
    val programado: Boolean get() = fechaEstaProgramada(fecha, ahora)

    /** Los Animales que pueden ser la cría de un parto: cualquiera menos el del Evento. */
    val candidatasCria: List<Animal> get() = animales.filter { it.idAnimal != idAnimal }

    val puedeGuardar: Boolean
        get() = !guardando && idAnimal != null && titulo.isNotBlank() &&
            (tipo != TipoEvento.VACUNACION || biologico.isNotBlank())
}

sealed interface NuevoEventoEvento {
    data object Guardado : NuevoEventoEvento
}

/**
 * Estado del formulario Registrar evento. Desde el Detalle de un Animal ([idAnimalInicial]) el Animal ya viene
 * elegido; desde la pestaña Eventos se elige aquí. La fecha empieza en ahora y puede ser pasada o futura.
 */
class NuevoEventoViewModel(
    idAnimalInicial: String?,
    animalRepository: AnimalRepository,
    private val registrarEvento: RegistrarEvento,
    clock: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {
    private val _estado = MutableStateFlow(
        clock().let { ahora -> NuevoEventoUiState(fecha = ahora, ahora = ahora, idAnimal = idAnimalInicial) },
    )
    val estado: StateFlow<NuevoEventoUiState> = _estado.asStateFlow()

    private val _eventos = Channel<NuevoEventoEvento>(Channel.BUFFERED)
    val eventos: Flow<NuevoEventoEvento> = _eventos.receiveAsFlow()

    init {
        viewModelScope.launch {
            animalRepository.observeActivos().collect { animales ->
                _estado.update { it.copy(animales = animales) }
            }
        }
    }

    fun onTipoSeleccionado(tipo: TipoEvento) = _estado.update { it.copy(tipo = tipo, error = null) }
    fun onTituloCambiado(valor: String) = _estado.update { it.copy(titulo = valor, error = null) }
    fun onFechaCambiada(fecha: Long) = _estado.update { it.copy(fecha = fecha, error = null) }
    fun onAnimalSeleccionado(idAnimal: String) =
        _estado.update { it.copy(idAnimal = idAnimal, idCria = it.idCria.takeIf { cria -> cria != idAnimal }, error = null) }
    fun onBiologicoCambiado(valor: String) = _estado.update { it.copy(biologico = valor) }
    fun onDosisCambiada(valor: String) = _estado.update { it.copy(dosis = valor) }
    fun onViaSeleccionada(via: String) = _estado.update { it.copy(via = via) }
    fun onCriaSeleccionada(idAnimal: String?) = _estado.update { it.copy(idCria = idAnimal, error = null) }

    fun onGuardar() {
        val actual = _estado.value
        val idAnimal = actual.idAnimal
        if (!actual.puedeGuardar || idAnimal == null) return
        _estado.update { it.copy(guardando = true) }
        viewModelScope.launch {
            val resultado = registrarEvento(
                idAnimal = idAnimal,
                tipo = actual.tipo,
                detalle = actual.titulo.trim(),
                fecha = actual.fecha,
                biologico = actual.biologico,
                dosis = actual.dosis,
                via = actual.via,
                idCriaResultante = actual.idCria,
            )
            val error = when (resultado) {
                is RegistrarEvento.Resultado.Registrado -> null
                RegistrarEvento.Resultado.SinUsuario -> NuevoEventoError.SinUsuario
                RegistrarEvento.Resultado.AnimalNoDisponible -> NuevoEventoError.AnimalNoDisponible
                RegistrarEvento.Resultado.CriaNoValida -> NuevoEventoError.CriaNoValida
                RegistrarEvento.Resultado.CriaConOtroProgenitor -> NuevoEventoError.CriaConOtroProgenitor
                RegistrarEvento.Resultado.FaltanDatos -> NuevoEventoError.FaltanDatos
            }
            if (error == null) {
                _eventos.send(NuevoEventoEvento.Guardado)
            } else {
                _estado.update { it.copy(error = error, guardando = false) }
            }
        }
    }
}
