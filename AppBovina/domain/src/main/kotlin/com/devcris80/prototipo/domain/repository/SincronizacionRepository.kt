package com.devcris80.prototipo.domain.repository

/** La sincronización con el servidor (CU-20). Necesita señal y la sesión guardada. */
interface SincronizacionRepository {
    /**
     * Sube por lotes los cambios locales pendientes y aplica la respuesta fila por fila: una aceptada queda
     * sincronizada, un sibling deja la fila en Conflicto y un Rechazo pasa a la bandeja (ADR-0004).
     */
    suspend fun subirCambios(): ResultadoSubida
}

sealed interface ResultadoSubida {
    /** Todos los lotes llegaron. Cuenta los cambios de cada resultado. */
    data class Completa(val aceptados: Int, val enConflicto: Int, val rechazados: Int) : ResultadoSubida

    /** Se perdió la señal. Los lotes ya confirmados quedaron sincronizados; el resto se reenvía después. */
    data object SinConexion : ResultadoSubida

    /** El servidor respondió con un error inesperado. */
    data object NoSePudo : ResultadoSubida
}
