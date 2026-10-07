package com.devcris80.prototipo.ui.screens

import android.app.Activity
import android.content.Intent
import android.nfc.NfcAdapter
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.devcris80.prototipo.domain.DestinoEscaneo
import com.devcris80.prototipo.ui.theme.BovinaMinFieldHeight
import com.devcris80.prototipo.ui.theme.BovinaScreenGutter
import kotlinx.coroutines.launch

private val FLAGS_LECTURA_TAG = NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK

private class Lectura(val codigo: String)

@Composable
fun EscanearScreen(
    resolverDestino: suspend (codigo: String) -> DestinoEscaneo,
    onAbrirAnimal: (idAnimal: String) -> Unit,
    onRegistrar: (codigo: String, aviso: String?) -> Unit,
    onLiberarChapeta: suspend (idChapeta: String) -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val nfcAdapter = remember { NfcAdapter.getDefaultAdapter(context) }
    val scope = rememberCoroutineScope()
    var nfcActivo by remember { mutableStateOf(nfcAdapter?.isEnabled == true) }
    var lectura by remember { mutableStateOf<Lectura?>(null) }
    var destino by remember { mutableStateOf<DestinoEscaneo?>(null) }

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
                                activity.runOnUiThread { lectura = Lectura(codigo) }
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

    LaunchedEffect(lectura) {
        val leido = lectura ?: return@LaunchedEffect
        destino = resolverDestino(leido.codigo)
    }

    LaunchedEffect(destino) {
        when (val actual = destino) {
            is DestinoEscaneo.AbrirAnimal -> {
                destino = null
                lectura = null
                onAbrirAnimal(actual.idAnimal)
            }
            is DestinoEscaneo.RegistrarConAviso -> {
                destino = null
                lectura = null
                onRegistrar(actual.codigo, "Este tag estuvo asociado a ${actual.nombreAnimalAnterior}")
            }
            is DestinoEscaneo.RegistrarNuevo -> {
                destino = null
                lectura = null
                onRegistrar(actual.codigo, null)
            }
            is DestinoEscaneo.AnimalDadoDeBaja, null -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(BovinaScreenGutter),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.Nfc,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp),
        )
        val dadoDeBaja = destino as? DestinoEscaneo.AnimalDadoDeBaja
        when {
            nfcAdapter == null -> Text(
                text = "Este celular no tiene NFC",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            !nfcActivo -> {
                Text(
                    text = "Active NFC en ajustes para escanear",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Button(
                    onClick = { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(BovinaMinFieldHeight),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Abrir ajustes de NFC", style = MaterialTheme.typography.labelLarge)
                }
            }
            dadoDeBaja != null -> {
                Text(
                    text = "La chapeta ${dadoDeBaja.codigo} pertenece a ${dadoDeBaja.nombreAnimal}, que está dado de baja.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Button(
                    onClick = {
                        scope.launch {
                            onLiberarChapeta(dadoDeBaja.idChapeta)
                            lectura = Lectura(dadoDeBaja.codigo)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(BovinaMinFieldHeight),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Liberar chapeta", style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = {
                        destino = null
                        lectura = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(BovinaMinFieldHeight),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Cancelar", style = MaterialTheme.typography.labelLarge)
                }
            }
            lectura != null -> Text(
                text = "Leyendo chapeta…",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            else -> Text(
                text = "Acerque el tag NFC al celular",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun ByteArray.toCodigoHex(): String =
    joinToString(separator = "") { (it.toInt() and 0xFF).toString(16).padStart(2, '0').uppercase() }
