package com.devcris80.prototipo.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devcris80.prototipo.domain.repository.OperarioDeLaFinca
import com.devcris80.prototipo.domain.repository.OperarioRepository
import com.devcris80.prototipo.domain.repository.ResultadoListaOperarios
import com.devcris80.prototipo.domain.repository.ResultadoQuitarOperario
import com.devcris80.prototipo.domain.usecase.QuitarOperario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OperariosUiState(
    /** Los Operarios que siguen en la finca; nulo mientras no se ha podido leer la lista. */
    val operarios: List<OperarioDeLaFinca>? = null,
    val cargando: Boolean = true,
    /** Verdadero mientras se quita a alguien: evita repetirlo con un doble toque. */
    val quitando: Boolean = false,
    val error: String? = null,
    /** Lo que pasó al quitar a alguien, hasta que la pantalla lo muestra. */
    val aviso: String? = null,
)

/**
 * Estado de la lista de Operarios de la finca (CU-05): se lee del servidor, y quitar a uno pide señal y el rol
 * Ganadero (RF-37). La confirmación que explica el efecto la pide la pantalla antes de llamar [onQuitar].
 */
class OperariosViewModel(
    private val operarioRepository: OperarioRepository,
    private val quitarOperario: QuitarOperario,
) : ViewModel() {
    private val _estado = MutableStateFlow(OperariosUiState())
    val estado: StateFlow<OperariosUiState> = _estado.asStateFlow()

    init {
        cargar()
    }

    fun onReintentar() {
        if (_estado.value.cargando) return
        cargar()
    }

    fun onAvisoMostrado() = _estado.update { it.copy(aviso = null) }

    fun onQuitar(operario: OperarioDeLaFinca) {
        if (_estado.value.quitando) return
        _estado.update { it.copy(quitando = true, error = null) }
        viewModelScope.launch {
            when (quitarOperario(operario.idUsuario)) {
                ResultadoQuitarOperario.Quitado -> _estado.update {
                    it.copy(
                        quitando = false,
                        operarios = it.operarios?.filterNot { otro -> otro.idUsuario == operario.idUsuario },
                        aviso = "Se quitó a ${operario.nombre} de la finca.",
                    )
                }
                // Alguien lo quitó antes: la lista que se ve ya no es la del servidor.
                ResultadoQuitarOperario.NoDisponible -> {
                    _estado.update { it.copy(quitando = false, aviso = "${operario.nombre} ya no estaba en la finca.") }
                    cargar()
                }
                ResultadoQuitarOperario.SinConexion ->
                    fallo("Sin señal. Quitar a un Operario necesita conexión a internet.")
                ResultadoQuitarOperario.NoPermitido -> fallo("Solo el Ganadero de la finca puede quitar a un Operario.")
                ResultadoQuitarOperario.NoSePudo -> fallo("No se pudo quitar al Operario. Inténtalo de nuevo en un momento.")
            }
        }
    }

    private fun fallo(mensaje: String) = _estado.update { it.copy(quitando = false, error = mensaje) }

    private fun cargar() {
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            when (val resultado = operarioRepository.listar()) {
                is ResultadoListaOperarios.Lista ->
                    _estado.update { it.copy(operarios = resultado.operarios, cargando = false) }
                ResultadoListaOperarios.SinConexion -> _estado.update {
                    it.copy(cargando = false, error = "Sin señal. La lista de Operarios necesita conexión a internet.")
                }
                ResultadoListaOperarios.NoSePudo -> _estado.update {
                    it.copy(cargando = false, error = "No se pudo leer la lista de Operarios. Inténtalo de nuevo.")
                }
            }
        }
    }
}
