package com.devcris80.prototipo.domain.repository

/** Ejecuta el bloque completo o nada: si falla, no queda guardada ninguna de sus escrituras. */
interface Transaccion {
    suspend fun <T> ejecutar(bloque: suspend () -> T): T
}
