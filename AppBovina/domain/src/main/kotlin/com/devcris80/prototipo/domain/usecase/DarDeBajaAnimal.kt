package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.repository.AnimalRepository

/** Da de baja un Animal: deja de aparecer entre los activos, pero su historial se conserva. */
class DarDeBajaAnimal(
    private val animalRepository: AnimalRepository,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    suspend operator fun invoke(idAnimal: String) {
        animalRepository.darDeBaja(idAnimal, clock())
    }
}
