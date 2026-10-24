package com.devcris80.prototipo

import android.app.Application
import com.devcris80.prototipo.service.programarSincronizacionTrasEscrituras
import kotlinx.coroutines.MainScope

class BovinaApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        MainScope().programarSincronizacionTrasEscrituras(container.sincronizacionRepository)
    }
}
