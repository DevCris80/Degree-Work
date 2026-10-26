package com.devcris80.prototipo.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devcris80.prototipo.domain.repository.ResultadoInicioSesion
import com.devcris80.prototipo.domain.repository.ResultadoRecuperacion
import com.devcris80.prototipo.domain.repository.SesionRepository
import com.devcris80.prototipo.domain.repository.SincronizacionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** El diálogo de "¿Olvidaste tu contraseña?": pide el correo y dice qué pasó al enviarlo. */
data class RecuperacionUiState(
    val correo: String,
    val enviando: Boolean = false,
    val enviado: Boolean = false,
    val mensaje: String? = null,
) {
    val puedeEnviar: Boolean get() = correo.isNotBlank() && !enviando && !enviado
}

data class LoginUiState(
    val correo: String = "",
    val contrasena: String = "",
    val cargando: Boolean = false,
    val error: String? = null,
    val recuperacion: RecuperacionUiState? = null,
) {
    val puedeIniciar: Boolean get() = correo.isNotBlank() && contrasena.isNotBlank() && !cargando
}

sealed interface LoginEvento {
    data object SesionIniciada : LoginEvento
}

/** Estado de Iniciar sesión: credenciales, inicio de sesión contra Supabase Auth y su error, y el diálogo de recuperar la contraseña. */
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

    private var envioDeRecuperacion: Job? = null

    fun onOlvideContrasena() {
        envioDeRecuperacion?.cancel()
        _estado.update { it.copy(recuperacion = RecuperacionUiState(correo = it.correo)) }
    }

    fun onRecuperacionCorreoCambiado(valor: String) =
        _estado.update { it.copy(recuperacion = it.recuperacion?.copy(correo = valor, mensaje = null)) }

    // Un envío en curso se cancela: su resultado no debe aparecer en un diálogo que se abra después.
    fun onCerrarRecuperacion() {
        envioDeRecuperacion?.cancel()
        _estado.update { it.copy(recuperacion = null) }
    }

    fun onEnviarRecuperacion() {
        val actual = _estado.value.recuperacion ?: return
        if (!actual.puedeEnviar) return
        _estado.update { it.copy(recuperacion = actual.copy(enviando = true, mensaje = null)) }
        envioDeRecuperacion = viewModelScope.launch {
            val resultado = sesionRepository.recuperarContrasena(actual.correo.trim())
            _estado.update {
                it.copy(
                    recuperacion = it.recuperacion?.copy(
                        enviando = false,
                        enviado = resultado == ResultadoRecuperacion.Enviado,
                        mensaje = resultado.mensaje(),
                    ),
                )
            }
        }
    }

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

// El mismo mensaje con o sin cuenta: no se revela quién está registrado.
private fun ResultadoRecuperacion.mensaje(): String = when (this) {
    ResultadoRecuperacion.Enviado ->
        "Si el correo está registrado, te enviamos un enlace para recuperar la contraseña."
    ResultadoRecuperacion.SinConexion -> "Sin señal. Hace falta conexión para enviar el correo de recuperación."
    ResultadoRecuperacion.NoSePudo -> "No se pudo enviar el correo. Inténtalo de nuevo en un momento."
}
