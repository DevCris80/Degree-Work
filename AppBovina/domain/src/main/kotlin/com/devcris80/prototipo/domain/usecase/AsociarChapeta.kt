package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.Chapeta
import com.devcris80.prototipo.domain.model.PesajePendiente
import com.devcris80.prototipo.domain.model.normalizarCodigo
import com.devcris80.prototipo.domain.repository.AnimalRepository
import com.devcris80.prototipo.domain.repository.ChapetaRepository
import com.devcris80.prototipo.domain.repository.PesajePendienteRepository
import com.devcris80.prototipo.domain.repository.Transaccion
import java.util.UUID

/**
 * Asocia una Chapeta nueva a un Animal activo que no tiene una activa (RF-19, CU-11): el codigo
 * no puede tener otra Chapeta activa, y las Chapetas liberadas del Animal quedan como historial.
 * Todo se comprueba dentro de la transacción.
 *
 * Si el codigo tiene Pesajes pendientes sin resolver, los devuelve para ofrecer asignarlos al
 * Animal, igual que al registrarlo ([RegistrarAnimal]). No los asigna: eso lo decide una persona.
 */
class AsociarChapeta(
    private val animalRepository: AnimalRepository,
    private val chapetaRepository: ChapetaRepository,
    private val pesajePendienteRepository: PesajePendienteRepository,
    private val transaccion: Transaccion,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    sealed interface Resultado {
        data class Asociada(val pesajesPendientes: List<PesajePendiente>) : Resultado

        /** El codigo ya es la Chapeta activa de un Animal. */
        data class CodigoYaActivo(val codigo: String) : Resultado

        /** El Animal ya tiene una Chapeta activa: primero se libera. */
        data object AnimalYaTieneChapeta : Resultado

        /** El Animal no existe o está dado de baja. */
        data object AnimalNoDisponible : Resultado

        /** El codigo queda vacío al normalizarlo. */
        data object CodigoVacio : Resultado
    }

    suspend operator fun invoke(idAnimal: String, codigoIngresado: String): Resultado = transaccion.ejecutar {
        val codigo = normalizarCodigo(codigoIngresado)
        if (codigo.isEmpty()) return@ejecutar Resultado.CodigoVacio
        if (animalRepository.findById(idAnimal)?.activo != true) return@ejecutar Resultado.AnimalNoDisponible
        if (chapetaRepository.findActivaByAnimal(idAnimal) != null) return@ejecutar Resultado.AnimalYaTieneChapeta
        if (chapetaRepository.findActivaByCodigo(codigo) != null) return@ejecutar Resultado.CodigoYaActivo(codigo)
        chapetaRepository.insert(
            Chapeta(
                idChapeta = UUID.randomUUID().toString(),
                codigo = codigo,
                idAnimal = idAnimal,
                fechaAsociacion = clock(),
            ),
        )
        Resultado.Asociada(pesajePendienteRepository.findSinResolverByCodigo(codigo))
    }
}
