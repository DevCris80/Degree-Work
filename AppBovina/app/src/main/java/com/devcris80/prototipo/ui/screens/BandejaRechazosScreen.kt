package com.devcris80.prototipo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.devcris80.prototipo.domain.model.AccionSugerida
import com.devcris80.prototipo.domain.model.EntidadSincronizada
import com.devcris80.prototipo.domain.model.MotivoRechazo
import com.devcris80.prototipo.domain.model.Rechazo
import com.devcris80.prototipo.domain.model.RechazoEnBandeja
import com.devcris80.prototipo.ui.components.BannerInformativo
import com.devcris80.prototipo.ui.components.BovinaCard
import com.devcris80.prototipo.ui.components.BovinaPrimaryButton
import com.devcris80.prototipo.ui.components.BovinaSecondaryButton
import com.devcris80.prototipo.ui.components.BovinaTopBar
import com.devcris80.prototipo.ui.theme.BovinaScreenGutter
import com.devcris80.prototipo.ui.theme.Prototipo1Theme
import com.devcris80.prototipo.ui.util.appViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun BandejaRechazosScreen(
    onVolver: () -> Unit,
    onVerAnimal: (idAnimal: String) -> Unit,
) {
    val viewModel = appViewModel { container ->
        BandejaRechazosViewModel(container.observarBandejaRechazos, container.rechazoRepository)
    }
    val estado by viewModel.estado.collectAsState()
    BandejaRechazosContent(
        estado = estado,
        onVolver = onVolver,
        onVerAnimal = onVerAnimal,
        onCerrar = viewModel::onCerrar,
    )
}

@Composable
private fun BandejaRechazosContent(
    estado: BandejaRechazosUiState,
    onVolver: () -> Unit,
    onVerAnimal: (idAnimal: String) -> Unit,
    onCerrar: (idRechazo: String) -> Unit,
) {
    val formatoFecha = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    Scaffold(
        topBar = { BovinaTopBar(titulo = "Cambios rechazados", onVolver = onVolver) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = BovinaScreenGutter),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
        ) {
            if (!estado.cargando && estado.rechazos.isEmpty()) {
                item {
                    Text(
                        text = "No hay cambios rechazados.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else if (estado.rechazos.isNotEmpty()) {
                item {
                    BannerInformativo(
                        texto = "El servidor no aceptó estos cambios. No se reenvían: corríjalos o ciérrelos. " +
                            "Cerrados, se conservan.",
                        icon = Icons.Filled.Info,
                    )
                }
            }
            items(estado.rechazos, key = { it.rechazo.idRechazo }) { enBandeja ->
                TarjetaRechazo(
                    enBandeja = enBandeja,
                    fecha = formatoFecha.format(enBandeja.rechazo.fechaRechazo),
                    onVerAnimal = onVerAnimal,
                    onCerrar = onCerrar,
                )
            }
        }
    }
}

@Composable
private fun TarjetaRechazo(
    enBandeja: RechazoEnBandeja,
    fecha: String,
    onVerAnimal: (idAnimal: String) -> Unit,
    onCerrar: (idRechazo: String) -> Unit,
) {
    val rechazo = enBandeja.rechazo
    BovinaCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = rechazo.titulo(enBandeja.nombreAnimal),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = enBandeja.motivo.texto(),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "Rechazado el $fecha",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // Corregirlo en el Animal es una escritura nueva; el Rechazo se cierra aparte.
            val idAnimal = rechazo.idAnimal
            if (idAnimal != null && enBandeja.nombreAnimal != null) {
                BovinaSecondaryButton(
                    texto = if (enBandeja.accion == AccionSugerida.ASOCIAR_OTRO_CODIGO) "Asociar otro código" else "Ver animal",
                    onClick = { onVerAnimal(idAnimal) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            BovinaPrimaryButton(
                texto = enBandeja.textoCerrar(),
                onClick = { onCerrar(rechazo.idRechazo) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

private fun Rechazo.titulo(nombreAnimal: String?): String {
    val deAnimal = nombreAnimal?.let { " de $it" }.orEmpty()
    return when (entidad) {
        EntidadSincronizada.PERFIL_FINCA -> "Datos de la finca"
        EntidadSincronizada.ANIMAL -> nombreAnimal?.let { "Animal $it" } ?: "Un animal"
        EntidadSincronizada.CHAPETA -> "Chapeta ${codigoChapeta.orEmpty()}$deAnimal"
        EntidadSincronizada.EVENTO -> "Evento$deAnimal"
        EntidadSincronizada.REGISTRO -> "Pesaje$deAnimal"
        EntidadSincronizada.PESAJE_PENDIENTE -> "Pesaje pendiente"
        EntidadSincronizada.FOTO -> "Foto$deAnimal"
    }
}

private fun MotivoRechazo.texto(): String = when (this) {
    is MotivoRechazo.CodigoEnOtroAnimal -> nombreOtroAnimal
        ?.let { "Este código ya está asociado a $it, desde otro celular." }
        ?: "Este código ya está asociado a otro animal de la finca."
    MotivoRechazo.YaConciliado ->
        "Este pesaje ya se concilió en otro celular. Aquí quedó como lo dejó el servidor."
    MotivoRechazo.SinPermiso -> "Su rol o su finca no permiten este cambio."
    MotivoRechazo.SeRechazoSuAnimal -> "Se rechazó el animal al que pertenece, y salió con él."
    MotivoRechazo.FotoNoAceptada -> "El servidor no aceptó el archivo de la foto."
    is MotivoRechazo.Otro -> "El servidor no aceptó el cambio: $mensaje"
}

private fun RechazoEnBandeja.textoCerrar(): String = when (accion) {
    AccionSugerida.ASOCIAR_OTRO_CODIGO -> (motivo as? MotivoRechazo.CodigoEnOtroAnimal)?.nombreOtroAnimal
        ?.let { "El código es de $it: cerrar" }
        ?: "El código es del otro animal: cerrar"
    AccionSugerida.ACEPTAR_LA_DEL_SERVIDOR -> "Aceptar la del servidor"
    AccionSugerida.CERRAR -> "Cerrar sin reenviar"
}

@Preview(showBackground = true)
@Composable
private fun BandejaRechazosPreview() {
    val chapeta = Rechazo(
        idRechazo = "rechazo-1",
        entidad = EntidadSincronizada.CHAPETA,
        idFila = "chapeta-1",
        contenido = "{}",
        motivo = "23505: duplicate key value",
        fechaRechazo = 0L,
        codigo = "23505",
        idAnimal = "lucero",
        codigoChapeta = "04A31B2C",
        porSuAnimal = false,
    )
    Prototipo1Theme {
        BandejaRechazosContent(
            estado = BandejaRechazosUiState(
                rechazos = listOf(
                    RechazoEnBandeja(chapeta, "Lucero", MotivoRechazo.CodigoEnOtroAnimal("Tormenta"), AccionSugerida.ASOCIAR_OTRO_CODIGO),
                    RechazoEnBandeja(
                        chapeta.copy(idRechazo = "rechazo-2", entidad = EntidadSincronizada.PESAJE_PENDIENTE, idAnimal = null),
                        null,
                        MotivoRechazo.YaConciliado,
                        AccionSugerida.ACEPTAR_LA_DEL_SERVIDOR,
                    ),
                ),
                cargando = false,
            ),
            onVolver = {},
            onVerAnimal = {},
            onCerrar = {},
        )
    }
}
