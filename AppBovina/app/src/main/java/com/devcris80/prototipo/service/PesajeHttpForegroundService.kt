package com.devcris80.prototipo.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.devcris80.prototipo.BovinaApplication
import com.devcris80.prototipo.R
import com.devcris80.prototipo.domain.RegistroPesoResolver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Foreground Service que mantiene el servidor HTTP escuchando POST /registro-peso incluso con
 * la pantalla apagada (requisito de estabilidad, sección 5 de la spec).
 */
class PesajeHttpForegroundService : Service() {

    private var server: PesajeHttpServer? = null

    override fun onCreate() {
        super.onCreate()
        val database = (application as BovinaApplication).database
        val resolver = RegistroPesoResolver(database.chapetaDao(), database.registroDao())
        server = PesajeHttpServer(PesajeHttpServer.DEFAULT_PORT, resolver).also { it.start() }
        startForeground(NOTIFICATION_ID, buildNotification())
        _isRunning.value = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        server?.stop()
        server = null
        _isRunning.value = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        createNotificationChannel()
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.pesaje_service_notification_title))
            .setContentText(
                getString(R.string.pesaje_service_notification_text, PesajeHttpServer.DEFAULT_PORT),
            )
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.pesaje_service_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        private const val CHANNEL_ID = "pesaje_http_service"
        private const val NOTIFICATION_ID = 1

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning

        fun start(context: Context) {
            context.startForegroundService(Intent(context, PesajeHttpForegroundService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, PesajeHttpForegroundService::class.java))
        }
    }
}
