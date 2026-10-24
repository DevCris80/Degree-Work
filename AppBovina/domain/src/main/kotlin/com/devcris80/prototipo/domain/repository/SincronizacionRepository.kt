package com.devcris80.prototipo.domain.repository

/** La sincronización con el servidor (CU-20). Necesita señal y la sesión guardada. */
interface SincronizacionRepository {
    /**
     * Sube por lotes los cambios locales pendientes y aplica la respuesta fila por fila: una aceptada queda
     * sincronizada, un sibling deja la fila en Conflicto y un Rechazo pasa a la bandeja (ADR-0004). Después
     * descarga por páginas lo que los demás celulares de la finca subieron, sin pisar los cambios pendientes.
     */
    suspend fun sincronizar(): ResultadoSincronizacion
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
