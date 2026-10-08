package com.devcris80.prototipo

import android.app.Application
import com.devcris80.prototipo.data.local.AppDatabase
import com.devcris80.prototipo.data.local.seedTestDataIfEmpty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BovinaApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val container: AppContainer by lazy { AppContainer(database) }
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            database.seedTestDataIfEmpty()
        }
    }
}
