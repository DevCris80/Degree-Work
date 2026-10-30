package com.devcris80.prototipo.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devcris80.prototipo.domain.model.CuentaNueva
import com.devcris80.prototipo.domain.repository.ResultadoCreacionCuenta
import com.devcris80.prototipo.domain.usecase.CrearCuenta
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CrearCuentaUiState(
    val nombreGanadero: String = "",
    val nombreFinca: String = "",
    val correo: String = "",
    val contrasena: String = "",
    val confirmacion: String = "",
    val aceptaAutorizacion: Boolean = false,
    /** La persona se une a una finca con un código de invitación en vez de crear la suya (CU-04). */
    val tengoCodigo: Boolean = false,
    val codigoInvitacion: String = "",
    val cargando: Boolean = false,
    val error: String? = null,
    /** El correo ya tiene cuenta: la pantalla ofrece ir a Iniciar sesión. */
    val ofrecerIniciarSesion: Boolean = false,
) {
    val cuenta: CuentaNueva
        get() = CuentaNueva(
            nombreGanadero, nombreFinca, correo, contrasena, confirmacion, aceptaAutorizacion, tengoCodigo, codigoInvitacion,
        )

    val puedeCrear: Boolean get() = cuenta.faltaParaCrear == null && !cargando
}

sealed interface CrearCuentaEvento {
    data object CuentaCreada : CrearCuentaEvento
}

/**
 * Estado de Crear cuenta (CU-01, CU-04): los datos de la persona y de su finca, o el código de invitación para unirse
 * a una, la aceptación del texto de autorización y la creación de la cuenta en Supabase con el caso de uso
 * CrearCuenta.
 */
class CrearCuentaViewModel(private val crearCuenta: CrearCuenta) : ViewModel() {
    private val _estado = MutableStateFlow(CrearCuentaUiState())
    val estado: StateFlow<CrearCuentaUiState> = _estado.asStateFlow()

    private val _eventos = Channel<CrearCuentaEvento>(Channel.BUFFERED)
    val eventos: Flow<CrearCuentaEvento> = _eventos.receiveAsFlow()

    fun onNombreGanaderoCambiado(valor: String) = _estado.update { it.copy(nombreGanadero = valor, error = null) }
    fun onNombreFincaCambiado(valor: String) = _estado.update { it.copy(nombreFinca = valor, error = null) }
    fun onCorreoCambiado(valor: String) =
        _estado.update { it.copy(correo = valor, error = null, ofrecerIniciarSesion = false) }
    fun onContrasenaCambiada(valor: String) = _estado.update { it.copy(contrasena = valor, error = null) }
    fun onConfirmacionCambiada(valor: String) = _estado.update { it.copy(confirmacion = valor, error = null) }
    fun onAutorizacionCambiada(acepta: Boolean) = _estado.update { it.copy(aceptaAutorizacion = acepta) }
    fun onTengoCodigoCambiado(tengo: Boolean) =
        _estado.update { it.copy(tengoCodigo = tengo, error = null, ofrecerIniciarSesion = false) }
    fun onCodigoInvitacionCambiado(valor: String) = _estado.update { it.copy(codigoInvitacion = valor, error = null) }

    fun onCrearCuenta() {
        val actual = _estado.value
        if (!actual.puedeCrear) return
        _estado.update { it.copy(cargando = true, error = null, ofrecerIniciarSesion = false) }
        viewModelScope.launch {
            // El botón no se habilita con el formulario incompleto; si igual llega, no hay nada que contar.
            val resultado = when (val creacion = crearCuenta(actual.cuenta)) {
                is CrearCuenta.Resultado.Incompleta -> {
                    _estado.update { it.copy(cargando = false) }
                    return@launch
                }
                is CrearCuenta.Resultado.DelServidor -> creacion.resultado
            }
            _estado.update {
                it.copy(
                    cargando = false,
                    error = resultado.mensaje(),
                    ofrecerIniciarSesion = resultado == ResultadoCreacionCuenta.CorreoYaRegistrado,
                )
            }
            if (resultado == ResultadoCreacionCuenta.Exito) _eventos.send(CrearCuentaEvento.CuentaCreada)
        }
    }
}

private fun ResultadoCreacionCuenta.mensaje(): String? = when (this) {
    ResultadoCreacionCuenta.Exito -> null
    ResultadoCreacionCuenta.CorreoYaRegistrado -> "Este correo ya tiene una cuenta. Inicia sesión con él."
    ResultadoCreacionCuenta.ContrasenaRechazada ->
        "El servidor no aceptó la contraseña. Usa una más larga, con letras y números."
    ResultadoCreacionCuenta.SinConexion -> "Sin señal. Crear la cuenta necesita conexión a internet."
    ResultadoCreacionCuenta.OtraCuentaEnElDispositivo ->
        "Este celular tiene los datos de otra cuenta. Desinstala la app para empezar limpio."
    ResultadoCreacionCuenta.CodigoRechazado ->
        "No se pudo unir a la finca. Revisa que el código esté vigente y sin usar, o inténtalo de nuevo."
    ResultadoCreacionCuenta.NoSePudo -> "No se pudo crear la cuenta. Inténtalo de nuevo en un momento."
}
