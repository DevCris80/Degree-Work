package com.devcris80.prototipo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.devcris80.prototipo.data.PerfilFinca
import com.devcris80.prototipo.ui.components.BannerInformativo
import com.devcris80.prototipo.ui.components.CampoConIcono
import com.devcris80.prototipo.ui.components.PillBadge
import com.devcris80.prototipo.ui.theme.BovinaMinFieldHeight
import com.devcris80.prototipo.ui.theme.BovinaScreenGutter

@Composable
fun RegistroScreen(
    onCrearCuenta: (PerfilFinca) -> Unit,
    onIrALogin: () -> Unit,
) {
    var nombreGanadero by remember { mutableStateOf("") }
    var nombreFinca by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmarPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmarVisible by remember { mutableStateOf(false) }

    val puedeCrear = nombreGanadero.isNotBlank() && nombreFinca.isNotBlank() &&
        correo.isNotBlank() && password.length >= 6 && password == confirmarPassword

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(BovinaScreenGutter),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .padding(20.dp)
                        .size(40.dp),
                )
            }
            Text(
                text = "Crear cuenta",
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Regístrese para empezar a llevar el control de su ganado.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            CampoConIcono(
                label = "Nombre completo",
                value = nombreGanadero,
                onValueChange = { nombreGanadero = it },
                icon = Icons.Filled.Person,
            )
            CampoConIcono(
                label = "Nombre de la finca",
                value = nombreFinca,
                onValueChange = { nombreFinca = it },
                icon = Icons.Filled.Home,
            )
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
                helperText = "Mínimo 6 caracteres",
                esPassword = true,
                passwordVisible = passwordVisible,
                onTogglePasswordVisible = { passwordVisible = !passwordVisible },
            )
            CampoConIcono(
                label = "Confirmar contraseña",
                value = confirmarPassword,
                onValueChange = { confirmarPassword = it },
                icon = Icons.Filled.Shield,
                esPassword = true,
                passwordVisible = confirmarVisible,
                onTogglePasswordVisible = { confirmarVisible = !confirmarVisible },
            )

            BannerInformativo(
                texto = "Sus registros de pesaje y ganado se guardan automáticamente en este " +
                    "celular sin gastar sus datos.",
                icon = Icons.Filled.CheckCircle,
            )

            Button(
                onClick = {
                    onCrearCuenta(
                        PerfilFinca(
                            nombreGanadero = nombreGanadero,
                            nombreFinca = nombreFinca,
                        ),
                    )
                },
                enabled = puedeCrear,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BovinaMinFieldHeight),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Crear cuenta →", style = MaterialTheme.typography.labelLarge)
            }

            TextButton(onClick = onIrALogin) {
                Text("¿Ya tiene una cuenta? Iniciar sesión")
            }

            PillBadge(texto = "Funciona también sin señal de internet en su finca.")
        }
    }
}
