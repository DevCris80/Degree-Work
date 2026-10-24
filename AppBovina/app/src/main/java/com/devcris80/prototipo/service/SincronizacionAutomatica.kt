package com.devcris80.prototipo.service

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.devcris80.prototipo.domain.repository.SincronizacionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/** Encola la sincronización en WorkManager como un único trabajo, que corre cuando hay señal (CU-20). */
class ProgramadorSincronizacion(context: Context) {
    private val workManager = WorkManager.getInstance(context)

    fun programar() {
        val trabajo = OneTimeWorkRequestBuilder<SincronizacionWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            // Señal débil (CU-20 1b): cada reintento espera el doble que el anterior.
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, ESPERA_PRIMER_REINTENTO_SEGUNDOS, TimeUnit.SECONDS)
            .build()
        // Si ya hay uno esperando señal o en curso, ese sube también estos cambios.
        workManager.enqueueUniqueWork(NOMBRE_TRABAJO, ExistingWorkPolicy.KEEP, trabajo)
    }

    private companion object {
        const val NOMBRE_TRABAJO = "sincronizar"
        const val ESPERA_PRIMER_REINTENTO_SEGUNDOS = 30L
    }
}

/** Programa la sincronización cada vez que las escrituras locales se calman, mientras la app está viva. */
fun CoroutineScope.programarSincronizacionTrasEscrituras(sincronizacionRepository: SincronizacionRepository) {
    launch {
        sincronizacionRepository.observeEstado().map { it.cambiosSinSubir }
            .disparosDeSincronizacion(ESPERA_ESCRITURAS)
            .collect { sincronizacionRepository.programar() }
    }
}

/**
 * Un disparo cada vez que las escrituras locales se calman por [espera] y quedan cambios sin subir (CU-20). Cada
 * valor es cuántos cambios faltan por subir: una jornada de pesajes seguidos da un solo disparo, al final.
 */
@OptIn(FlowPreview::class)
internal fun Flow<Int>.disparosDeSincronizacion(espera: Duration): Flow<Unit> =
    debounce(espera).filter { it > 0 }.map { }

/**
 * Lo que se espera después de la última escritura. Agrupa las seguidas: en una jornada en la manga, un pesaje cada
 * uno o dos minutos da una sola sincronización, al final.
 */
private val ESPERA_ESCRITURAS = 2.minutes
