package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.Registro
import com.devcris80.prototipo.domain.model.normalizarCodigo
import com.devcris80.prototipo.domain.repository.ChapetaRepository
import com.devcris80.prototipo.domain.repository.CuentaRepository
import com.devcris80.prototipo.domain.repository.RegistroRepository
import java.util.UUID

/**
 * Resuelve un pesaje recibido (codigo + peso) contra la Chapeta activa de ese codigo y crea el
 * Registro del Animal correspondiente, a nombre del Usuario de este dispositivo.
 */
class RegistrarPesaje(
    private val chapetaRepository: ChapetaRepository,
    private val registroRepository: RegistroRepository,
    private val cuentaRepository: CuentaRepository,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    sealed interface Resultado {
        data class Exito(val idRegistro: String) : Resultado
        data class CodigoSinChapetaActiva(val codigo: String) : Resultado

        /** Todavía no se ha creado la cuenta en este dispositivo: no hay a nombre de quién guardar. */
        data object SinUsuario : Resultado
    }

    suspend operator fun invoke(codigoRecibido: String, peso: Float): Resultado {
        val codigo = normalizarCodigo(codigoRecibido)
        val usuario = cuentaRepository.getUsuario() ?: return Resultado.SinUsuario
        val chapeta = chapetaRepository.findActivaByCodigo(codigo)
            ?: return Resultado.CodigoSinChapetaActiva(codigo)

        val registro = Registro(
            idRegistro = UUID.randomUUID().toString(),
            idAnimal = chapeta.idAnimal,
            idUsuario = usuario.idUsuario,
            peso = peso,
            timestamp = clock(),
        )
        registroRepository.insert(registro)
        return Resultado.Exito(registro.idRegistro)
    }
}
