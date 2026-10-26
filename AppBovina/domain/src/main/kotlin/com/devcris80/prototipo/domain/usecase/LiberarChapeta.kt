package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.puedeAdministrar
import com.devcris80.prototipo.domain.repository.ChapetaRepository
import com.devcris80.prototipo.domain.repository.CuentaRepository

/**
 * Libera una Chapeta: deja de estar activa y su codigo queda disponible para otro Animal. Solo el
 * Ganadero puede; el Operario es rechazado aunque se llame directo, también sin señal.
 */
class LiberarChapeta(
    private val chapetaRepository: ChapetaRepository,
    private val cuentaRepository: CuentaRepository,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    sealed interface Resultado {
        data object Realizado : Resultado

        /** El Usuario de este dispositivo no es Ganadero. */
        data object NoPermitido : Resultado
    }

    suspend operator fun invoke(idChapeta: String): Resultado {
        if (!cuentaRepository.getUsuario().puedeAdministrar) return Resultado.NoPermitido
        chapetaRepository.liberar(idChapeta, clock())
        return Resultado.Realizado
    }
}
