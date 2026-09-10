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
        enableEdgeToEdge()
        val database = (application as BovinaApplication).database
        setContent {
            Prototipo1Theme {
                AppNavHost(database)
            }
        }
    }
}
