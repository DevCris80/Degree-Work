package com.devcris80.prototipo.domain.repository

import com.devcris80.prototipo.domain.model.CuentaGuardada
import kotlinx.coroutines.flow.Flow

/** Las cuentas cuyos datos guarda este celular, abiertas o no (ADR-0006). */
interface CuentasGuardadasRepository {
    /** Las cuentas guardadas con la sesión cerrada, para elegir cuál abrir o quitar. La que tiene la sesión abierta no sale. */
    fun observe(): Flow<List<CuentaGuardada>>

    /**
     * Lo que le queda por subir a la cuenta: cambios, fotos y cambios rechazados abiertos. Cuenta también cuando su
     * base no tiene Usuario ni finca. Cero si el celular no la guarda.
     */
    suspend fun pendientes(idUsuario: String): Int

    /** Borra del celular los datos de la cuenta, sin mirar lo que le quede por subir: de eso se encarga [QuitarCuentaGuardada][com.devcris80.prototipo.domain.usecase.QuitarCuentaGuardada]. */
    suspend fun quitar(idUsuario: String)
}

sealed interface ResultadoQuitarCuenta {
    data object Quitada : ResultadoQuitarCuenta

    /** Tiene cambios, fotos o rechazos por subir: hay que iniciar sesión y sincronizar primero. */
    data object TienePendientes : ResultadoQuitarCuenta
}
