package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.puedeAdministrar
import com.devcris80.prototipo.domain.repository.CuentaRepository
import com.devcris80.prototipo.domain.repository.OperarioRepository
import com.devcris80.prototipo.domain.repository.ResultadoQuitarOperario

/**
 * Quita a un Operario de la finca (RF-37, CU-05). Solo el Ganadero puede; el Operario es rechazado aunque se llame
 * directo, sin llegar al servidor (que también lo rechaza).
 */
class QuitarOperario(
    private val operarioRepository: OperarioRepository,
    private val cuentaRepository: CuentaRepository,
) {
    suspend operator fun invoke(idUsuario: String): ResultadoQuitarOperario {
        if (!cuentaRepository.getUsuario().puedeAdministrar) return ResultadoQuitarOperario.NoPermitido
        return operarioRepository.quitar(idUsuario)
    }
}
