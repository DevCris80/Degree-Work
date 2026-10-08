package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.PesajePendiente
import com.devcris80.prototipo.domain.repository.AnimalRepository
import com.devcris80.prototipo.domain.repository.ChapetaRepository
import com.devcris80.prototipo.domain.repository.PesajePendienteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Cómo está hoy el codigo de un Pesaje pendiente; decide qué se puede hacer con él. */
sealed interface EstadoCodigo {
    /** Se puede registrar un Animal nuevo con este codigo. */
    data object SinChapetaActiva : EstadoCodigo

    /** El Animal que hoy lleva esa Chapeta: el que se sugiere al asignar. */
    data class ChapetaDeAnimalActivo(val animal: Animal) : EstadoCodigo

    /** La Chapeta sigue asociada a un Animal dado de baja, que no es elegible para asignar. */
    data object ChapetaDeAnimalDadoDeBaja : EstadoCodigo
}

data class PesajePendientePorConciliar(
    val pesajePendiente: PesajePendiente,
    val estadoCodigo: EstadoCodigo,
)

/**
 * Los Pesajes pendientes sin resolver, del más reciente al más antiguo, cada uno con el estado
 * actual de su codigo. Lo que se puede hacer con un pendiente sale de ese estado y no de su
 * motivo, que es el del momento en que llegó.
 */
class ObservarPesajesPendientes(
    private val pesajePendienteRepository: PesajePendienteRepository,
    private val chapetaRepository: ChapetaRepository,
    private val animalRepository: AnimalRepository,
) {
    operator fun invoke(): Flow<List<PesajePendientePorConciliar>> = combine(
        pesajePendienteRepository.observeSinResolver(),
        chapetaRepository.observeActivas(),
        animalRepository.observeActivos(),
    ) { pendientes, chapetasActivas, animalesActivos ->
        val idAnimalPorCodigo = chapetasActivas.associate { it.codigo to it.idAnimal }
        val animalActivoPorId = animalesActivos.associateBy { it.idAnimal }
        pendientes.map { pendiente ->
            val idAnimal = idAnimalPorCodigo[pendiente.codigo]
            val animal = idAnimal?.let(animalActivoPorId::get)
            PesajePendientePorConciliar(
                pesajePendiente = pendiente,
                estadoCodigo = when {
                    idAnimal == null -> EstadoCodigo.SinChapetaActiva
                    animal == null -> EstadoCodigo.ChapetaDeAnimalDadoDeBaja
                    else -> EstadoCodigo.ChapetaDeAnimalActivo(animal)
                },
            )
        }
    }
}
