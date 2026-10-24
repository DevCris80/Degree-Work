package com.devcris80.prototipo

import android.app.Application

class BovinaApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
