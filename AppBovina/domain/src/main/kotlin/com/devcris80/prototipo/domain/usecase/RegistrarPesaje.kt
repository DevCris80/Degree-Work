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
 *
 * La hora del Registro es la de la medición: el momento de recepción menos la antigüedad que
 * reporta el ESP32. Nunca se usa un reloj enviado por el ESP32.
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
        data class Invalido(val motivo: MotivoInvalido) : Resultado

        /** Todavía no se ha creado la cuenta en este dispositivo: no hay a nombre de quién guardar. */
        data object SinUsuario : Resultado
    }

    enum class MotivoInvalido { PESO_FUERA_DE_RANGO, ANTIGUEDAD_FUERA_DE_RANGO }

    suspend operator fun invoke(pesaje: Pesaje): Resultado {
        val recibidoEn = clock()
        // `!(peso > 0)` en vez de `peso <= 0` para rechazar también un peso que no sea un número.
        if (!(pesaje.peso > 0f) || pesaje.peso > Pesaje.PESO_MAXIMO_KG) {
            return Resultado.Invalido(MotivoInvalido.PESO_FUERA_DE_RANGO)
        }
        // Una antigüedad mayor que la hora de recepción daría una hora de medición imposible.
        if (pesaje.antiguedadMs < 0 || pesaje.antiguedadMs > recibidoEn) {
            return Resultado.Invalido(MotivoInvalido.ANTIGUEDAD_FUERA_DE_RANGO)
        }
        return transaccion.ejecutar { registrar(pesaje, timestamp = recibidoEn - pesaje.antiguedadMs) }
    }

    private suspend fun registrar(pesaje: Pesaje, timestamp: Long): Resultado {
        val codigo = normalizarCodigo(pesaje.codigo)

        registroRepository.findByIdLectura(pesaje.idLectura)?.let { yaRegistrado ->
            return Resultado.Registrado(yaRegistrado.idRegistro)
        }

        val usuario = cuentaRepository.getUsuario() ?: return Resultado.SinUsuario
        val chapeta = chapetaRepository.findActivaByCodigo(codigo)
            ?: return Resultado.CodigoSinChapetaActiva(codigo)

        val registro = Registro(
            idRegistro = UUID.randomUUID().toString(),
            idAnimal = chapeta.idAnimal,
            idUsuario = usuario.idUsuario,
            idLectura = pesaje.idLectura,
            peso = pesaje.peso,
            timestamp = timestamp,
        )
        registroRepository.insert(registro)
        return Resultado.Registrado(registro.idRegistro)
    }
}
