package com.devcris80.prototipo

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.devcris80.prototipo.ui.nav.AppNavHost
import com.devcris80.prototipo.ui.theme.Prototipo1Theme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Al abrir la app, no al girar la pantalla.
        if (savedInstanceState == null) programarSincronizacion()
        enableEdgeToEdge()
        setContent {
            Prototipo1Theme {
                AppNavHost()
            }
        }
    }

    // Con la app en primer plano, un tag que ninguna pantalla lee no abre el visor del sistema. Las pantallas que
    // leen (LectorNfc) usan el modo lector, que tiene prioridad sobre este despacho.
    override fun onResume() {
        super.onResume()
        val adapter = NfcAdapter.getDefaultAdapter(this)?.takeIf { it.isEnabled } ?: return
        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        // Mutable: el sistema le agrega el tag al Intent.
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
        val filtros = arrayOf(IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED))
        adapter.enableForegroundDispatch(this, pendingIntent, filtros, null)
    }

    override fun onPause() {
        super.onPause()
        NfcAdapter.getDefaultAdapter(this)?.disableForegroundDispatch(this)
    }

    // El tag llega aquí y se descarta a propósito: solo se quería evitar el visor del sistema.
    override fun onNewIntent(intent: Intent) {
        if (intent.action != NfcAdapter.ACTION_TAG_DISCOVERED) super.onNewIntent(intent)
    }

    // Sin sesión no hay a nombre de quién sincronizar: la programa el inicio de sesión.
    private fun programarSincronizacion() {
        val container = (application as BovinaApplication).container
        lifecycleScope.launch {
            if (container.sesionRepository.observeSesionGuardada().first()) {
                container.sincronizacionRepository.programar()
            }
        }
    }
}
