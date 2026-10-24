package com.devcris80.prototipo

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
