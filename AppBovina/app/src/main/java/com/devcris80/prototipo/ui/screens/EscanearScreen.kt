package com.devcris80.prototipo.ui.screens

import android.app.Activity
import android.content.Intent
import android.nfc.NfcAdapter
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.devcris80.prototipo.ui.components.BovinaPrimaryButton
import com.devcris80.prototipo.ui.components.BovinaSecondaryButton
import com.devcris80.prototipo.ui.components.BovinaTopBar
import com.devcris80.prototipo.ui.components.PillBadge
import com.devcris80.prototipo.ui.theme.BovinaScreenGutter
import com.devcris80.prototipo.ui.util.appViewModel

private val FLAGS_LECTURA_TAG = NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK

@Composable
fun EscanearScreen(
    onAbrirAnimal: (idAnimal: String) -> Unit,
    onRegistrar: (codigo: String, aviso: String?) -> Unit,
) {
    val viewModel = appViewModel { container ->
        EscanearViewModel(container.resolverDestinoEscaneo, container.liberarChapeta, container.cuentaRepository)
    }
    val estado by viewModel.estado.collectAsState()
    val puedeLiberarChapeta by viewModel.puedeLiberarChapeta.collectAsState(initial = false)

    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val nfcAdapter = remember { NfcAdapter.getDefaultAdapter(context) }
    var nfcActivo by remember { mutableStateOf(nfcAdapter?.isEnabled == true) }

    LaunchedEffect(viewModel) {
        viewModel.eventos.collect { evento ->
            when (evento) {
                is EscanearEvento.AbrirAnimal -> onAbrirAnimal(evento.idAnimal)
                is EscanearEvento.Registrar -> onRegistrar(evento.codigo, evento.aviso)
            }
        }
    }

    DisposableEffect(lifecycleOwner, nfcAdapter, activity) {
        val observer = LifecycleEventObserver { _, evento ->
            if (activity == null || nfcAdapter == null) return@LifecycleEventObserver
            when (evento) {
                Lifecycle.Event.ON_RESUME -> {
                    nfcActivo = nfcAdapter.isEnabled
                    if (nfcAdapter.isEnabled) {
                        nfcAdapter.enableReaderMode(
                            activity,
                            { tag ->
                                val codigo = tag.id.toCodigoHex()
                                activity.runOnUiThread { viewModel.onCodigoLeido(codigo) }
                            },
                            FLAGS_LECTURA_TAG,
                            null,
                        )
                    }
                }
                Lifecycle.Event.ON_PAUSE -> nfcAdapter.disableReaderMode(activity)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (activity != null && nfcAdapter != null) nfcAdapter.disableReaderMode(activity)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        BovinaTopBar(titulo = "Escanear", mostrarLogo = true)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(BovinaScreenGutter),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val dadoDeBaja = estado as? EscanearUiState.AnimalDadoDeBaja
            if (nfcAdapter != null && nfcActivo) {
                PillBadge(
                    texto = "Antena NFC lista",
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
            RadarNfc()
            when {
                nfcAdapter == null -> Text(
                    text = "Este celular no tiene NFC",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                )
                !nfcActivo -> {
                    Text(
                        text = "Active NFC en ajustes para escanear",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                    )
                    BovinaPrimaryButton(
                        texto = "Abrir ajustes de NFC",
                        onClick = { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                dadoDeBaja != null -> {
                    Text(
                        text = "La chapeta ${dadoDeBaja.codigo} pertenece a ${dadoDeBaja.nombreAnimal}, que está dado de baja.",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                    if (puedeLiberarChapeta) {
                        BovinaPrimaryButton(
                            texto = "Liberar chapeta",
                            onClick = { viewModel.onLiberarChapeta() },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        Text(
                            text = "Solo el ganadero puede liberar la chapeta.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                        )
                    }
                    BovinaSecondaryButton(
                        texto = "Cancelar",
                        onClick = { viewModel.onCancelar() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                estado is EscanearUiState.Resolviendo -> Text(
                    text = "Leyendo chapeta…",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                )
                else -> Text(
                    text = "Acerque el tag NFC al celular",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** Anillos concéntricos con el ícono NFC al centro (solo decorativo). */
@Composable
private fun RadarNfc() {
    Box(
        modifier = Modifier
            .size(240.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(190.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(132.dp)
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                    .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Nfc,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp),
                )
            }
        }
    }
}

private fun ByteArray.toCodigoHex(): String =
    joinToString(separator = "") { (it.toInt() and 0xFF).toString(16).padStart(2, '0').uppercase() }
