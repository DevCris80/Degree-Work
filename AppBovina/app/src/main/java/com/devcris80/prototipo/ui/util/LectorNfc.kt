package com.devcris80.prototipo.ui.util

import android.app.Activity
import android.nfc.NfcAdapter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

enum class EstadoNfc { SIN_NFC, APAGADO, LISTO }

private val FLAGS_LECTURA_TAG = NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK

/**
 * Lee tags NFC mientras la pantalla está en primer plano y entrega su codigo (el UID en hexadecimal, en
 * mayúsculas). Devuelve si el celular tiene NFC y si está encendido.
 */
@Composable
fun LectorNfc(onCodigoLeido: (String) -> Unit): EstadoNfc {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    val nfcAdapter = remember { NfcAdapter.getDefaultAdapter(context) }
    var nfcActivo by remember { mutableStateOf(nfcAdapter?.isEnabled == true) }
    val alLeer by rememberUpdatedState(onCodigoLeido)

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
                                activity.runOnUiThread { alLeer(codigo) }
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

    return when {
        nfcAdapter == null -> EstadoNfc.SIN_NFC
        !nfcActivo -> EstadoNfc.APAGADO
        else -> EstadoNfc.LISTO
    }
}

private fun ByteArray.toCodigoHex(): String =
    joinToString(separator = "") { (it.toInt() and 0xFF).toString(16).padStart(2, '0').uppercase() }
