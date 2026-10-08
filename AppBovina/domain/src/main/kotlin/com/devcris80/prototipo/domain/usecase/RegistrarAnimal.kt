package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Chapeta
import com.devcris80.prototipo.domain.repository.AnimalRepository
import com.devcris80.prototipo.domain.repository.ChapetaRepository
import com.devcris80.prototipo.domain.repository.Transaccion
import java.util.UUID

/**
 * Guarda un Animal y, si hay codigo, su Chapeta activa en una sola transacción: o se guardan
 * ambos o ninguno. Verifica dentro de la transacción que el codigo no tenga otra Chapeta activa.
 */
class RegistrarAnimal(
    private val animalRepository: AnimalRepository,
    private val chapetaRepository: ChapetaRepository,
    private val transaccion: Transaccion,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    sealed interface Resultado {
        data class Exito(val idAnimal: String) : Resultado
        data class CodigoYaActivo(val codigo: String) : Resultado
    }

    suspend operator fun invoke(animal: Animal, codigo: String?): Resultado = transaccion.ejecutar {
        if (codigo != null && chapetaRepository.findActivaByCodigo(codigo) != null) {
            return@ejecutar Resultado.CodigoYaActivo(codigo)
        }
        animalRepository.insert(animal)
        if (codigo != null) {
            chapetaRepository.insert(
                Chapeta(
                    idChapeta = UUID.randomUUID().toString(),
                    codigo = codigo,
                    idAnimal = animal.idAnimal,
                    fechaAsociacion = clock(),
                ),
            )
        }
        Resultado.Exito(animal.idAnimal)
    }
}
