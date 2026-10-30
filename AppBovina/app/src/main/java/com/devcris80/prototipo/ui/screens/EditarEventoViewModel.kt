package com.devcris80.prototipo.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devcris80.prototipo.domain.model.TipoEvento
import com.devcris80.prototipo.domain.model.puedeAdministrar
import com.devcris80.prototipo.domain.model.fechaEstaProgramada
import com.devcris80.prototipo.domain.repository.AnimalRepository
import com.devcris80.prototipo.domain.repository.CuentaRepository
import com.devcris80.prototipo.domain.repository.EventoRepository
import com.devcris80.prototipo.domain.usecase.ActualizarEvento
import com.devcris80.prototipo.domain.usecase.DarDeBajaEvento
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditarEventoUiState(
    /** El tipo como se guardó, para mostrarlo: no se cambia al editar. */
    val tipo: String = "",
    val esVacunacion: Boolean = false,
    val titulo: String = "",
    val fecha: Long = 0L,
    /** El momento en que se abrió la pantalla, para saber si [fecha] todavía no llega. */
    val ahora: Long = 0L,
    val biologico: String = "",
    val dosis: String = "",
    val via: String? = null,
    val nombreAnimal: String? = null,
    /** La cría de un parto: se muestra pero no se cambia. */
    val nombreCria: String? = null,
    /** El Evento ya se leyó de la base: hasta entonces no hay nada que guardar. */
    val cargado: Boolean = false,
    val error: EditarEventoError? = null,
    /** Verdadero desde que se pide guardar o dar de baja hasta que falla: evita repetirlo con un doble toque. */
    val guardando: Boolean = false,
    /** Dar de baja es del Ganadero; al Operario no se le ofrece (matriz 2.1). */
    val puedeDarDeBaja: Boolean = false,
) {
    /** Un Evento programado: su fecha todavía no llega. */
    val programado: Boolean get() = fechaEstaProgramada(fecha, ahora)

    val puedeGuardar: Boolean
        get() = cargado && !guardando && titulo.isNotBlank() && (!esVacunacion || biologico.isNotBlank())
}

sealed interface EditarEventoError {
    data object FaltanDatos : EditarEventoError
    data object EventoNoDisponible : EditarEventoError
}

sealed interface EditarEventoEvento {
    data object Guardado : EditarEventoEvento
    data object DadoDeBaja : EditarEventoEvento
}

/**
 * Estado del formulario Editar evento (CU-18, CU-19): parte de lo guardado, se carga una sola vez para no pisar lo
 * que la persona va escribiendo, guarda con ActualizarEvento y da de baja con DarDeBajaEvento.
 */
class EditarEventoViewModel(
    private val idEvento: String,
    eventoRepository: EventoRepository,
    animalRepository: AnimalRepository,
    cuentaRepository: CuentaRepository,
    private val actualizarEvento: ActualizarEvento,
    private val darDeBajaEvento: DarDeBajaEvento,
    clock: () -> Long = { System.currentTimeMillis() },
) : ViewModel() {
    private val _estado = MutableStateFlow(EditarEventoUiState(ahora = clock()))
    val estado: StateFlow<EditarEventoUiState> = _estado.asStateFlow()

    private val _eventos = Channel<EditarEventoEvento>(Channel.BUFFERED)
    val eventos: Flow<EditarEventoEvento> = _eventos.receiveAsFlow()

    init {
        viewModelScope.launch {
            val evento = eventoRepository.findById(idEvento)?.takeIf { it.fechaBaja == null }
            if (evento == null) {
                _estado.update { it.copy(error = EditarEventoError.EventoNoDisponible) }
            } else {
                _estado.update {
                    it.copy(
                        tipo = evento.tipoEscrito,
                        esVacunacion = evento.tipoConocido == TipoEvento.VACUNACION,
                        titulo = evento.detalle,
                        fecha = evento.fecha,
                        biologico = evento.biologico.orEmpty(),
                        dosis = evento.dosis.orEmpty(),
                        via = evento.via,
                        nombreAnimal = animalRepository.findById(evento.idAnimal)?.nombre,
                        nombreCria = evento.idCriaResultante?.let { idCria -> animalRepository.findById(idCria)?.nombre },
                        cargado = true,
                    )
                }
            }
        }
        viewModelScope.launch {
            cuentaRepository.observeUsuario().collect { usuario ->
                _estado.update { it.copy(puedeDarDeBaja = usuario.puedeAdministrar) }
            }
        }
    }

    fun onTituloCambiado(valor: String) = _estado.update { it.copy(titulo = valor, error = null) }
    fun onFechaCambiada(fecha: Long) = _estado.update { it.copy(fecha = fecha, error = null) }
    fun onBiologicoCambiado(valor: String) = _estado.update { it.copy(biologico = valor, error = null) }
    fun onDosisCambiada(valor: String) = _estado.update { it.copy(dosis = valor) }
    fun onViaSeleccionada(via: String) = _estado.update { it.copy(via = via) }

    fun onGuardar() {
        val actual = _estado.value
        if (!actual.puedeGuardar) return
        _estado.update { it.copy(guardando = true) }
        viewModelScope.launch {
            val resultado = actualizarEvento(
                idEvento = idEvento,
                detalle = actual.titulo,
                fecha = actual.fecha,
                biologico = actual.biologico,
                dosis = actual.dosis,
                via = actual.via,
            )
            val error = when (resultado) {
                ActualizarEvento.Resultado.Actualizado -> null
                ActualizarEvento.Resultado.FaltanDatos -> EditarEventoError.FaltanDatos
                ActualizarEvento.Resultado.NoDisponible -> EditarEventoError.EventoNoDisponible
            }
            if (error == null) {
                _eventos.send(EditarEventoEvento.Guardado)
            } else {
                _estado.update { it.copy(error = error, guardando = false) }
            }
        }
    }

    fun onDarDeBaja() {
        val actual = _estado.value
        if (!actual.cargado || actual.guardando || !actual.puedeDarDeBaja) return
        _estado.update { it.copy(guardando = true) }
        viewModelScope.launch {
            when (darDeBajaEvento(idEvento)) {
                DarDeBajaEvento.Resultado.Realizado -> _eventos.send(EditarEventoEvento.DadoDeBaja)
                DarDeBajaEvento.Resultado.NoPermitido ->
                    _estado.update { it.copy(guardando = false, puedeDarDeBaja = false) }
                DarDeBajaEvento.Resultado.NoDisponible ->
                    _estado.update { it.copy(guardando = false, error = EditarEventoError.EventoNoDisponible) }
            }
        }
    }
}
