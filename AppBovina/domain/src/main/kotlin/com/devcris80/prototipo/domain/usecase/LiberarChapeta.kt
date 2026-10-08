package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.repository.ChapetaRepository

/** Libera una Chapeta: deja de estar activa y su codigo queda disponible para otro Animal. */
class LiberarChapeta(
    private val chapetaRepository: ChapetaRepository,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    suspend operator fun invoke(idChapeta: String) {
        chapetaRepository.liberar(idChapeta, clock())
    }
}
