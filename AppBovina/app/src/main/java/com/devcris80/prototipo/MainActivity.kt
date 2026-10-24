package com.devcris80.prototipo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.devcris80.prototipo.ui.nav.AppNavHost
import com.devcris80.prototipo.ui.theme.Prototipo1Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Al abrir la app, no al girar la pantalla. Si ya hay una sincronización programada, no se programa otra.
        if (savedInstanceState == null) (application as BovinaApplication).container.sincronizacionRepository.programar()
        enableEdgeToEdge()
        setContent {
            Prototipo1Theme {
                AppNavHost()
            }
        }
    }
}
