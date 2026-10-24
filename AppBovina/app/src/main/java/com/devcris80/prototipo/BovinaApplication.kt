package com.devcris80.prototipo

import android.app.Application
import com.devcris80.prototipo.service.programarSincronizacionAutomatica
import kotlinx.coroutines.MainScope

class BovinaApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        MainScope().programarSincronizacionAutomatica(container.sesionRepository, container.sincronizacionRepository)
    }
}
