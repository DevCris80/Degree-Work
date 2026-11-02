package com.devcris80.prototipo.domain.repository

import com.devcris80.prototipo.domain.model.CuentaGuardada
import kotlinx.coroutines.flow.Flow

/** Las cuentas cuyos datos guarda este celular, abiertas o no (ADR-0006). */
interface CuentasGuardadasRepository {
    /** Las cuentas guardadas con la sesión cerrada, para elegir cuál abrir o quitar. La que tiene la sesión abierta no sale. */
    fun observe(): Flow<List<CuentaGuardada>>

    /** Borra del celular los datos de la cuenta, salvo que le quede algo por subir (RN-27). */
    suspend fun quitar(idUsuario: String): ResultadoQuitarCuenta
}

sealed interface ResultadoQuitarCuenta {
    data object Quitada : ResultadoQuitarCuenta

    /** Tiene cambios, fotos o rechazos por subir: hay que iniciar sesión y sincronizar primero. */
    data object TienePendientes : ResultadoQuitarCuenta
}
