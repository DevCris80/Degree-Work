package com.devcris80.prototipo.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devcris80.prototipo.domain.repository.ResultadoInicioSesion
import com.devcris80.prototipo.domain.repository.SesionRepository
import com.devcris80.prototipo.domain.repository.SincronizacionRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val correo: String = "",
    val contrasena: String = "",
    val cargando: Boolean = false,
    val error: String? = null,
) {
    val puedeIniciar: Boolean get() = correo.isNotBlank() && contrasena.isNotBlank() && !cargando
}

sealed interface LoginEvento {
    data object SesionIniciada : LoginEvento
}

/** Estado de Iniciar sesión: credenciales, inicio de sesión contra Supabase Auth y su error. */
class LoginViewModel(
    private val sesionRepository: SesionRepository,
    private val sincronizacionRepository: SincronizacionRepository,
) : ViewModel() {
    private val _estado = MutableStateFlow(LoginUiState())
    val estado: StateFlow<LoginUiState> = _estado.asStateFlow()

    private val _eventos = Channel<LoginEvento>(Channel.BUFFERED)
    val eventos: Flow<LoginEvento> = _eventos.receiveAsFlow()

    fun onCorreoCambiado(valor: String) = _estado.update { it.copy(correo = valor, error = null) }
    fun onContrasenaCambiada(valor: String) = _estado.update { it.copy(contrasena = valor, error = null) }

    fun onIniciarSesion() {
        val actual = _estado.value
        if (!actual.puedeIniciar) return
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            val resultado = sesionRepository.iniciarSesion(actual.correo.trim(), actual.contrasena)
            _estado.update { it.copy(cargando = false, error = resultado.mensaje()) }
            if (resultado == ResultadoInicioSesion.Exito) {
                // Trae lo que los demás celulares ya subieron a la finca.
                sincronizacionRepository.programar()
                _eventos.send(LoginEvento.SesionIniciada)
            }
        }
    }
}

private fun ResultadoInicioSesion.mensaje(): String? = when (this) {
    ResultadoInicioSesion.Exito -> null
    ResultadoInicioSesion.CredencialesInvalidas -> "Correo o contraseña incorrectos."
    ResultadoInicioSesion.SinConexion ->
        "Sin conexión. La primera vez en este celular hace falta señal para iniciar sesión."
    ResultadoInicioSesion.SinFinca -> "Esta cuenta todavía no pertenece a ninguna finca."
    ResultadoInicioSesion.OtraCuentaEnElDispositivo ->
        "Este celular tiene los datos de otra cuenta. Desinstala la app para empezar limpio."
    ResultadoInicioSesion.NoSePudo -> "No se pudo iniciar sesión. Inténtalo de nuevo en un momento."
}
