package com.devcris80.prototipo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.devcris80.prototipo.ui.components.BannerInformativo
import com.devcris80.prototipo.ui.components.BovinaCard
import com.devcris80.prototipo.ui.components.BovinaPrimaryButton
import com.devcris80.prototipo.ui.components.BovinaSecondaryButton
import com.devcris80.prototipo.ui.components.CampoConIcono
import com.devcris80.prototipo.ui.components.EncabezadoAuth
import com.devcris80.prototipo.ui.theme.BovinaScreenGutter
import com.devcris80.prototipo.ui.util.appViewModel

@Composable
fun LoginScreen(
    onIniciarSesion: () -> Unit,
    onCrearCuentaNueva: () -> Unit,
) {
    val viewModel = appViewModel { container -> LoginViewModel(container.cuentaRepository) }
    val usuarioActual by viewModel.usuario.collectAsState()
    var correo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = innerPadding.calculateBottomPadding()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            EncabezadoAuth(
                etiqueta = "MIGANADO",
                titulo = "Iniciar sesión",
                subtitulo = "Ingrese con su cuenta para continuar con el registro de su ganado.",
                oscuro = true,
                modifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = BovinaScreenGutter),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                usuarioActual?.let { usuarioLogin ->
                    Text(
                        text = "Hola, ${usuarioLogin.nombre}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }

                BannerInformativo(
                    texto = "Modo campo activo: trabaja sin cobertura",
                    icon = Icons.Filled.WifiOff,
                )

                BovinaCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        CampoConIcono(
                            label = "Correo electrónico",
                            value = correo,
                            onValueChange = { correo = it },
                            icon = Icons.Filled.Email,
                            keyboardType = KeyboardType.Email,
                        )
                        CampoConIcono(
                            label = "Contraseña",
                            value = password,
                            onValueChange = { password = it },
                            icon = Icons.Filled.Lock,
                            esPassword = true,
                            passwordVisible = passwordVisible,
                            onTogglePasswordVisible = { passwordVisible = !passwordVisible },
                        )

                        TextButton(
                            onClick = {},
                            modifier = Modifier.align(Alignment.End),
                        ) {
                            Text(
                                text = "¿Olvidaste tu contraseña?",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

                        BovinaPrimaryButton(
                            texto = "Iniciar sesión",
                            onClick = onIniciarSesion,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
                        )

                        BovinaSecondaryButton(
                            texto = "Crear cuenta nueva",
                            onClick = onCrearCuentaNueva,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                Row(
                    modifier = Modifier.padding(bottom = BovinaScreenGutter),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Flag,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Hecho para el campo colombiano · Con o sin internet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
