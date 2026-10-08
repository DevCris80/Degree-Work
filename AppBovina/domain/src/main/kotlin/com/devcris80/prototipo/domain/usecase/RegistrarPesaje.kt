package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.Pesaje
import com.devcris80.prototipo.domain.model.Registro
import com.devcris80.prototipo.domain.model.normalizarCodigo
import com.devcris80.prototipo.domain.repository.ChapetaRepository
import com.devcris80.prototipo.domain.repository.CuentaRepository
import com.devcris80.prototipo.domain.repository.RegistroRepository
import com.devcris80.prototipo.domain.repository.Transaccion
import java.util.UUID

/**
 * Resuelve un Pesaje contra la Chapeta activa de su codigo y crea el Registro del Animal
 * correspondiente, a nombre del Usuario de este dispositivo. Recibir dos veces el mismo Pesaje
 * no crea nada nuevo: responde lo mismo que la primera vez (ver docs/adr/0003).
 */
class RegistrarPesaje(
    private val chapetaRepository: ChapetaRepository,
    private val registroRepository: RegistroRepository,
    private val cuentaRepository: CuentaRepository,
    private val transaccion: Transaccion,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    sealed interface Resultado {
        data class Registrado(val idRegistro: String) : Resultado
        data class CodigoSinChapetaActiva(val codigo: String) : Resultado

        /** Todavía no se ha creado la cuenta en este dispositivo: no hay a nombre de quién guardar. */
        data object SinUsuario : Resultado
    }

    suspend operator fun invoke(pesaje: Pesaje): Resultado = transaccion.ejecutar {
        val codigo = normalizarCodigo(pesaje.codigo)

        registroRepository.findByIdLectura(pesaje.idLectura)?.let { yaRegistrado ->
            return@ejecutar Resultado.Registrado(yaRegistrado.idRegistro)
        }

        val usuario = cuentaRepository.getUsuario() ?: return@ejecutar Resultado.SinUsuario
        val chapeta = chapetaRepository.findActivaByCodigo(codigo)
            ?: return@ejecutar Resultado.CodigoSinChapetaActiva(codigo)

        val registro = Registro(
            idRegistro = UUID.randomUUID().toString(),
            idAnimal = chapeta.idAnimal,
            idUsuario = usuario.idUsuario,
            idLectura = pesaje.idLectura,
            peso = pesaje.peso,
            timestamp = clock(),
        )
        registroRepository.insert(registro)
        Resultado.Registrado(registro.idRegistro)
    }
}
