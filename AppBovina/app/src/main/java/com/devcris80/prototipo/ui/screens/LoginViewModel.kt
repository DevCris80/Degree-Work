package com.devcris80.prototipo.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devcris80.prototipo.domain.model.CuentaGuardada
import com.devcris80.prototipo.domain.repository.CuentasGuardadasRepository
import com.devcris80.prototipo.domain.repository.ResultadoInicioSesion
import com.devcris80.prototipo.domain.repository.ResultadoQuitarCuenta
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

/** El diálogo de quitar una cuenta guardada del celular: la cuenta y, si no se pudo, por qué (RN-27). */
data class QuitarCuentaUiState(
    val cuenta: CuentaGuardada,
    val quitando: Boolean = false,
) {
    /** Con algo por subir no se ofrece quitarla: hay que iniciar sesión y sincronizar primero. */
    val puedeQuitar: Boolean get() = !cuenta.tienePendientes && !quitando
}

data class LoginUiState(
    val correo: String = "",
    val contrasena: String = "",
    val cargando: Boolean = false,
    val error: String? = null,
    val recuperacion: RecuperacionUiState? = null,
    val cuentasGuardadas: List<CuentaGuardada> = emptyList(),
    val quitarCuenta: QuitarCuentaUiState? = null,
) {
    val puedeIniciar: Boolean get() = correo.isNotBlank() && contrasena.isNotBlank() && !cargando
}

sealed interface LoginEvento {
    data object SesionIniciada : LoginEvento
}

/**
 * Estado de Iniciar sesión: credenciales, inicio de sesión contra Supabase Auth y su error, el diálogo de recuperar la
 * contraseña, y las cuentas guardadas en el celular con la forma de quitarlas (ADR-0006).
 */
class LoginViewModel(
    private val sesionRepository: SesionRepository,
    private val sincronizacionRepository: SincronizacionRepository,
    cuentasGuardadasRepository: CuentasGuardadasRepository,
) : ViewModel() {
    private val cuentasGuardadas = cuentasGuardadasRepository
    private val _estado = MutableStateFlow(LoginUiState())
    val estado: StateFlow<LoginUiState> = _estado.asStateFlow()

    private val _eventos = Channel<LoginEvento>(Channel.BUFFERED)
    val eventos: Flow<LoginEvento> = _eventos.receiveAsFlow()

    init {
        viewModelScope.launch {
            cuentasGuardadas.observe().collect { cuentas ->
                _estado.update { actual ->
                    actual.copy(
                        cuentasGuardadas = cuentas,
                        // El diálogo sigue la cuenta que muestra: si tenía algo por subir y ya no, se puede quitar.
                        quitarCuenta = actual.quitarCuenta?.let { dialogo ->
                            cuentas.firstOrNull { it.idUsuario == dialogo.cuenta.idUsuario }?.let { dialogo.copy(cuenta = it) }
                        },
                    )
                }
            }
        }
    }

    fun onCorreoCambiado(valor: String) = _estado.update { it.copy(correo = valor, error = null) }
    fun onContrasenaCambiada(valor: String) = _estado.update { it.copy(contrasena = valor, error = null) }

    /** Tocar una cuenta guardada rellena el correo; la contraseña se pide igual (Q3). */
    fun onCuentaGuardadaClick(cuenta: CuentaGuardada) {
        val correo = cuenta.correo ?: return
        _estado.update { it.copy(correo = correo, contrasena = "", error = null) }
    }

    fun onQuitarCuentaClick(cuenta: CuentaGuardada) =
        _estado.update { it.copy(quitarCuenta = QuitarCuentaUiState(cuenta)) }

    fun onCancelarQuitarCuenta() = _estado.update { it.copy(quitarCuenta = null) }

    fun onConfirmarQuitarCuenta() {
        val dialogo = _estado.value.quitarCuenta ?: return
        if (!dialogo.puedeQuitar) return
        _estado.update { it.copy(quitarCuenta = dialogo.copy(quitando = true)) }
        viewModelScope.launch {
            // El repositorio vuelve a mirar: entre abrir el diálogo y confirmar pudo llegar algo por subir.
            val resultado = cuentasGuardadas.quitar(dialogo.cuenta.idUsuario)
            _estado.update {
                when (resultado) {
                    ResultadoQuitarCuenta.Quitada -> it.copy(quitarCuenta = null)
                    ResultadoQuitarCuenta.TienePendientes -> it.copy(quitarCuenta = dialogo.copy(quitando = false))
                }
            }
        }
    }

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
    ResultadoInicioSesion.SinConexion -> "Sin conexión. Para iniciar sesión hace falta señal."
    ResultadoInicioSesion.SinFinca -> "Esta cuenta todavía no pertenece a ninguna finca."
    ResultadoInicioSesion.CuentaQuitada ->
        "Ya no tienes acceso a esa finca. Borramos sus datos de este celular."
    ResultadoInicioSesion.NoSePudo -> "No se pudo iniciar sesión. Inténtalo de nuevo en un momento."
}

// El mismo mensaje con o sin cuenta: no se revela quién está registrado.
private fun ResultadoRecuperacion.mensaje(): String = when (this) {
    ResultadoRecuperacion.Enviado ->
        "Si el correo está registrado, te enviamos un enlace para recuperar la contraseña."
    ResultadoRecuperacion.SinConexion -> "Sin señal. Hace falta conexión para enviar el correo de recuperación."
    ResultadoRecuperacion.NoSePudo -> "No se pudo enviar el correo. Inténtalo de nuevo en un momento."
}
