package com.devcris80.prototipo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.devcris80.prototipo.domain.model.AutorizacionDatos
import com.devcris80.prototipo.domain.model.CuentaNueva
import com.devcris80.prototipo.ui.components.BannerInformativo
import com.devcris80.prototipo.ui.components.BovinaCard
import com.devcris80.prototipo.ui.components.BovinaPrimaryButton
import com.devcris80.prototipo.ui.components.BovinaSecondaryButton
import com.devcris80.prototipo.ui.components.CampoConIcono
import com.devcris80.prototipo.ui.components.EncabezadoAuth
import com.devcris80.prototipo.ui.components.PillBadge
import com.devcris80.prototipo.ui.theme.BovinaScreenGutter
import com.devcris80.prototipo.ui.util.appViewModel

@Composable
fun CrearCuentaScreen(
    onCuentaCreada: () -> Unit,
    onIrALogin: () -> Unit,
) {
    val viewModel = appViewModel { container -> CrearCuentaViewModel(container.crearCuenta) }
    val estado by viewModel.estado.collectAsState()

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmarVisible by remember { mutableStateOf(false) }
    var leyendoAutorizacion by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.eventos.collect { evento ->
            when (evento) {
                CrearCuentaEvento.CuentaCreada -> onCuentaCreada()
            }
        }
    }

    if (leyendoAutorizacion) {
        AlertDialog(
            onDismissRequest = { leyendoAutorizacion = false },
            title = { Text("Autorización de datos personales") },
            text = {
                Text(
                    text = AutorizacionDatos.TEXTO,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onAutorizacionCambiada(true)
                    leyendoAutorizacion = false
                }) { Text("Acepto") }
            },
            dismissButton = {
                TextButton(onClick = { leyendoAutorizacion = false }) { Text("Cerrar") }
            },
        )
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(BovinaScreenGutter),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            EncabezadoAuth(
                etiqueta = "MIGANADO",
                titulo = "Crear cuenta",
                subtitulo = "Regístrese para empezar a llevar el control de su ganado.",
            )

            BovinaCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // CU-04: con un código la persona se une a una finca que ya existe, y no escribe el nombre de una.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(checked = estado.tengoCodigo, onCheckedChange = viewModel::onTengoCodigoCambiado)
                        Text(
                            text = "Tengo un código de invitación",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 12.dp),
                        )
                    }
                    CampoConIcono(
                        label = "Nombre completo",
                        value = estado.nombreGanadero,
                        onValueChange = viewModel::onNombreGanaderoCambiado,
                        icon = Icons.Filled.Person,
                    )
                    if (estado.tengoCodigo) {
                        CampoConIcono(
                            label = "Código de invitación",
                            value = estado.codigoInvitacion,
                            onValueChange = viewModel::onCodigoInvitacionCambiado,
                            icon = Icons.Filled.VpnKey,
                            helperText = "Te lo da el Ganadero de la finca. Vale por 7 días y una sola vez.",
                        )
                    } else {
                        CampoConIcono(
                            label = "Nombre de la finca",
                            value = estado.nombreFinca,
                            onValueChange = viewModel::onNombreFincaCambiado,
                            icon = Icons.Filled.Home,
                        )
                    }
                    CampoConIcono(
                        label = "Correo electrónico",
                        value = estado.correo,
                        onValueChange = viewModel::onCorreoCambiado,
                        icon = Icons.Filled.Email,
                        keyboardType = KeyboardType.Email,
                    )
                    CampoConIcono(
                        label = "Contraseña",
                        value = estado.contrasena,
                        onValueChange = viewModel::onContrasenaCambiada,
                        icon = Icons.Filled.Lock,
                        helperText = "Mínimo ${CuentaNueva.CONTRASENA_MINIMA} caracteres",
                        esPassword = true,
                        passwordVisible = passwordVisible,
                        onTogglePasswordVisible = { passwordVisible = !passwordVisible },
                    )
                    CampoConIcono(
                        label = "Confirmar contraseña",
                        value = estado.confirmacion,
                        onValueChange = viewModel::onConfirmacionCambiada,
                        icon = Icons.Filled.Shield,
                        esPassword = true,
                        passwordVisible = confirmarVisible,
                        onTogglePasswordVisible = { confirmarVisible = !confirmarVisible },
                    )

                    // CU-01 paso 3: la autorización se acepta después de ver su texto, en el diálogo.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = estado.aceptaAutorizacion,
                            onCheckedChange = { marcada ->
                                if (marcada) leyendoAutorizacion = true else viewModel.onAutorizacionCambiada(false)
                            },
                        )
                        Column {
                            Text(
                                text = "Acepto el tratamiento de mis datos personales (Ley 1581 de 2012).",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            TextButton(onClick = { leyendoAutorizacion = true }) { Text("Leer la autorización") }
                        }
                    }

                    BannerInformativo(
                        texto = "Crear la cuenta necesita señal. Después, sus registros se guardan en este " +
                            "celular aunque no haya cobertura.",
                        icon = Icons.Filled.WifiOff,
                    )

                    estado.error?.let { error ->
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    if (estado.ofrecerIniciarSesion) {
                        BovinaSecondaryButton(
                            texto = "Iniciar sesión con este correo",
                            onClick = onIrALogin,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    BovinaPrimaryButton(
                        texto = if (estado.cargando) "Creando cuenta…" else "Crear cuenta",
                        onClick = viewModel::onCrearCuenta,
                        enabled = estado.puedeCrear,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
                    )
                }
            }

            TextButton(onClick = onIrALogin) {
                Text(
                    text = "¿Ya tiene una cuenta? Iniciar sesión",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            PillBadge(
                texto = "Funciona también sin señal de internet en su finca.",
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            )
        }
    }
}
