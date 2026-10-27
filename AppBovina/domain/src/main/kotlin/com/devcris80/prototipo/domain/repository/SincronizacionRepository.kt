package com.devcris80.prototipo.domain.repository

import kotlinx.coroutines.flow.Flow

/** La sincronización con el servidor (CU-20). Necesita señal y la sesión guardada. */
interface SincronizacionRepository {
    /**
     * Sube por lotes los cambios locales pendientes y aplica la respuesta fila por fila: una aceptada queda
     * sincronizada, un sibling deja la fila en Conflicto y un Rechazo pasa a la bandeja (ADR-0004). Después
     * descarga por páginas lo que los demás celulares de la finca subieron, sin pisar los cambios pendientes.
     * Nunca corren dos a la vez: si ya hay una en curso, espera a que termine. Las fotos no van aquí: ver
     * [sincronizarFotos].
     */
    suspend fun sincronizar(): ResultadoSincronizacion

    /**
     * Sube las fotos de los Animales que están por subir y baja las que cambiaron en otros celulares (RF-38). Va
     * aparte de [sincronizar], después, para que las fotos nunca retrasen los datos: lo que no pasa queda para la
     * próxima vez, sin error. Tampoco corre a la vez que [sincronizar].
     */
    suspend fun sincronizarFotos()

    /**
     * Deja la sincronización pendiente para que corra sola, en segundo plano, cuando haya señal. Si ya hay una
     * pendiente o en curso, no agrega otra.
     */
    fun programar()

    /** Cuántos cambios y fotos faltan por subir y cuándo terminó la última sincronización; se lee de Room, no de la red. */
    fun observeEstado(): Flow<EstadoSincronizacion>
}

sealed interface ResultadoSincronizacion {
    /** Subió y descargó todo. Cuenta los cambios subidos según su resultado. */
    data class Completa(val aceptados: Int, val enConflicto: Int, val rechazados: Int) : ResultadoSincronizacion

    /**
     * Se perdió la señal. Lo ya confirmado quedó: los lotes subidos y las páginas descargadas. El resto se
     * reenvía, y la descarga sigue desde la última página guardada.
     */
    data object SinConexion : ResultadoSincronizacion

    /** El servidor respondió con un error inesperado. */
    data object NoSePudo : ResultadoSincronizacion
}

/**
 * Cuántas filas locales tienen cambios que el servidor todavía no confirmó, y cuándo terminó la última
 * sincronización completa (nula si nunca terminó una). [fotosSinSubir] cuenta aparte las fotos de los Animales que
 * no han subido: viajan por separado y no detienen los datos.
 */
data class EstadoSincronizacion(val cambiosSinSubir: Int, val ultimaSincronizacion: Long?, val fotosSinSubir: Int = 0)
