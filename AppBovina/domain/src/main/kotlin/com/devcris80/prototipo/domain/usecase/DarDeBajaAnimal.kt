package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.puedeAdministrar
import com.devcris80.prototipo.domain.repository.AnimalRepository
import com.devcris80.prototipo.domain.repository.CuentaRepository

/**
 * Da de baja un Animal: deja de aparecer entre los activos, pero su historial se conserva. Solo el
 * Ganadero puede; el Operario es rechazado aunque se llame directo, también sin señal.
 */
class DarDeBajaAnimal(
    private val animalRepository: AnimalRepository,
    private val cuentaRepository: CuentaRepository,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    sealed interface Resultado {
        data object Realizado : Resultado

        /** El Usuario de este dispositivo no es Ganadero. */
        data object NoPermitido : Resultado
    }

    suspend operator fun invoke(idAnimal: String): Resultado {
        if (!cuentaRepository.getUsuario().puedeAdministrar) return Resultado.NoPermitido
        animalRepository.darDeBaja(idAnimal, clock())
        return Resultado.Realizado
    }
}
