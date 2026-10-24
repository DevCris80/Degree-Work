package com.devcris80.prototipo.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.devcris80.prototipo.BovinaApplication
import com.devcris80.prototipo.domain.repository.ResultadoSincronizacion
import kotlinx.coroutines.flow.first

/**
 * El trabajo que sincroniza en segundo plano (CU-20). Sin señal reintenta con espera creciente (1b); si el servidor
 * responde con un error, lo intenta unas pocas veces más y espera al próximo disparo.
 */
class SincronizacionWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val container = (applicationContext as BovinaApplication).container
        // Sin sesión no hay a nombre de quién subir. Al iniciar sesión se vuelve a programar.
        if (!container.sesionRepository.observeSesionGuardada().first()) return Result.success()
        val sincronizacion = container.sincronizacionRepository
        // Mientras este trabajo corre no se encola otro: lo que se escribió en ese tiempo sube en otra pasada.
        repeat(PASADAS) {
            when (sincronizacion.sincronizar()) {
                is ResultadoSincronizacion.Completa ->
                    if (sincronizacion.observeEstado().first().cambiosSinSubir == 0) return Result.success()
                ResultadoSincronizacion.SinConexion -> return Result.retry()
                ResultadoSincronizacion.NoSePudo ->
                    return if (runAttemptCount < REINTENTOS_SI_FALLA) Result.retry() else Result.failure()
            }
        }
        return Result.success()
    }

    private companion object {
        const val PASADAS = 3
        const val REINTENTOS_SI_FALLA = 3
    }
}
