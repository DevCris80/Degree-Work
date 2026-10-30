package com.devcris80.prototipo.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devcris80.prototipo.domain.repository.Invitacion
import com.devcris80.prototipo.domain.repository.InvitacionRepository
import com.devcris80.prototipo.domain.repository.ResultadoInvitacion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InvitarOperarioUiState(
    /** El último código generado en esta pantalla; no se guarda en el celular. */
    val invitacion: Invitacion? = null,
    val generando: Boolean = false,
    val error: String? = null,
)

/**
 * Pantalla de invitar a un Operario (CU-04, RF-05): el Ganadero genera un código de un solo uso, que vence a los 7
 * días, y se lo da a la persona para que cree su cuenta con él. Generarlo necesita señal.
 */
class InvitarOperarioViewModel(private val invitacionRepository: InvitacionRepository) : ViewModel() {
    private val _estado = MutableStateFlow(InvitarOperarioUiState())
    val estado: StateFlow<InvitarOperarioUiState> = _estado.asStateFlow()

    fun onGenerar() {
        if (_estado.value.generando) return
        _estado.update { it.copy(generando = true, error = null) }
        viewModelScope.launch {
            val resultado = invitacionRepository.generar()
            _estado.update {
                when (resultado) {
                    is ResultadoInvitacion.Generada -> it.copy(generando = false, invitacion = resultado.invitacion)
                    // Un nuevo intento que falla no borra el código anterior, que sigue vigente.
                    else -> it.copy(generando = false, error = resultado.mensaje())
                }
            }
        }
    }
}

private fun ResultadoInvitacion.mensaje(): String? = when (this) {
    is ResultadoInvitacion.Generada -> null
    ResultadoInvitacion.SinConexion -> "Sin señal. Generar un código necesita conexión a internet."
    ResultadoInvitacion.NoPermitido -> "Solo el Ganadero de la finca puede generar códigos de invitación."
    ResultadoInvitacion.NoSePudo -> "No se pudo generar el código. Inténtalo de nuevo en un momento."
}
