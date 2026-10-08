package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Chapeta
import com.devcris80.prototipo.domain.model.PesajePendiente
import com.devcris80.prototipo.domain.model.normalizarCodigo
import com.devcris80.prototipo.domain.repository.AnimalRepository
import com.devcris80.prototipo.domain.repository.ChapetaRepository
import com.devcris80.prototipo.domain.repository.PesajePendienteRepository
import com.devcris80.prototipo.domain.repository.Transaccion
import java.util.UUID

/**
 * Guarda un Animal y, si hay codigo, su Chapeta activa en una sola transacción: o se guardan
 * ambos o ninguno. Verifica dentro de la transacción que el codigo no tenga otra Chapeta activa.
 *
 * Si ese codigo tiene Pesajes pendientes sin resolver, los devuelve para ofrecer asignarlos al
 * Animal recién registrado. No los asigna: eso lo decide una persona (ver docs/adr/0003).
 */
class RegistrarAnimal(
    private val animalRepository: AnimalRepository,
    private val chapetaRepository: ChapetaRepository,
    private val pesajePendienteRepository: PesajePendienteRepository,
    private val transaccion: Transaccion,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    sealed interface Resultado {
        data class Exito(val idAnimal: String, val pesajesPendientes: List<PesajePendiente>) : Resultado
        data class CodigoYaActivo(val codigo: String) : Resultado
    }

    suspend operator fun invoke(animal: Animal, codigoIngresado: String?): Resultado = transaccion.ejecutar {
        val codigo = codigoIngresado?.let(::normalizarCodigo)?.takeIf { it.isNotEmpty() }
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
        Resultado.Exito(
            idAnimal = animal.idAnimal,
            pesajesPendientes = codigo?.let { pesajePendienteRepository.findSinResolverByCodigo(it) }.orEmpty(),
        )
    }
}
