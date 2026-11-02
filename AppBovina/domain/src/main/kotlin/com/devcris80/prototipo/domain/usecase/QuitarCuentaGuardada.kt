package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.repository.CuentasGuardadasRepository
import com.devcris80.prototipo.domain.repository.ResultadoQuitarCuenta

/**
 * Quita del celular una cuenta guardada (RN-27). Solo si no le queda nada por subir: lo que tenga pendiente existe
 * únicamente aquí, y quitarla lo perdería.
 */
class QuitarCuentaGuardada(private val cuentasGuardadasRepository: CuentasGuardadasRepository) {
    suspend operator fun invoke(idUsuario: String): ResultadoQuitarCuenta {
        if (cuentasGuardadasRepository.pendientes(idUsuario) > 0) return ResultadoQuitarCuenta.TienePendientes
        cuentasGuardadasRepository.quitar(idUsuario)
        return ResultadoQuitarCuenta.Quitada
    }
}
