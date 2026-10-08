package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.Registro
import com.devcris80.prototipo.domain.model.normalizarCodigo
import com.devcris80.prototipo.domain.repository.ChapetaRepository
import com.devcris80.prototipo.domain.repository.RegistroRepository
import java.util.UUID

/**
 * Resuelve un pesaje recibido (codigo + peso) contra la Chapeta activa de ese codigo y crea el
 * Registro del Animal correspondiente.
 */
class RegistrarPesaje(
    private val chapetaRepository: ChapetaRepository,
    private val registroRepository: RegistroRepository,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    sealed interface Resultado {
        data class Exito(val idRegistro: String) : Resultado
        data class CodigoSinChapetaActiva(val codigo: String) : Resultado
    }

    suspend operator fun invoke(codigoRecibido: String, peso: Float): Resultado {
        val codigo = normalizarCodigo(codigoRecibido)
        val chapeta = chapetaRepository.findActivaByCodigo(codigo)
            ?: return Resultado.CodigoSinChapetaActiva(codigo)

        val registro = Registro(
            idRegistro = UUID.randomUUID().toString(),
            idAnimal = chapeta.idAnimal,
            peso = peso,
            timestamp = clock(),
        )
        registroRepository.insert(registro)
        return Resultado.Exito(registro.idRegistro)
    }
}
