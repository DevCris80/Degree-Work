package com.devcris80.prototipo.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.devcris80.prototipo.BovinaApplication
import com.devcris80.prototipo.domain.repository.ResultadoSincronizacion
import kotlinx.coroutines.flow.first

/**
 * El trabajo que sincroniza en segundo plano (CU-20). WorkManager no lo arranca sin red. Con señal débil o un error
 * del servidor reintenta con espera creciente (1b), unas pocas veces: mientras espera un reintento sigue programado y
 * no deja programar otro, así que después de [INTENTOS] se rinde y el próximo disparo lo vuelve a programar. Una foto
 * que no subió también se reintenta así, aunque los datos ya hayan quedado.
 */
class SincronizacionWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as BovinaApplication).container
        // Sin sesión no hay a nombre de quién subir. Al iniciar sesión se vuelve a programar.
        if (!container.sesionRepository.observeSesionGuardada().first()) return Result.success()
        val sincronizacion = container.sincronizacionRepository
        // Mientras este trabajo corre no se encola otro: lo que se escribió en ese tiempo sube en otra pasada.
        for (pasada in 1..PASADAS) {
            when (sincronizacion.sincronizar()) {
                is ResultadoSincronizacion.Completa ->
                    if (sincronizacion.observeEstado().first().cambiosSinSubir == 0) break
                ResultadoSincronizacion.SinConexion, ResultadoSincronizacion.NoSePudo -> return reintentar()
                // La sesión ya se cerró: no hay nada más que sincronizar ni que reintentar.
                ResultadoSincronizacion.CuentaQuitada -> return Result.success()
            }
        }
        // Las fotos, después de los datos y aparte: una que no pasa no los detiene.
        sincronizacion.sincronizarFotos()
        return if (sincronizacion.observeEstado().first().fotosSinSubir == 0) Result.success() else reintentar()
    }

    private fun reintentar() = if (runAttemptCount + 1 < INTENTOS) Result.retry() else Result.failure()

    private companion object {
        const val PASADAS = 3

        /** Con la espera que se dobla desde 30 segundos, el último intento llega unos 15 minutos después del primero. */
        const val INTENTOS = 6
    }
}
