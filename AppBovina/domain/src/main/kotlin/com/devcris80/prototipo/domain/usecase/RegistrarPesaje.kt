package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.MotivoPesajePendiente
import com.devcris80.prototipo.domain.model.Pesaje
import com.devcris80.prototipo.domain.model.PesajePendiente
import com.devcris80.prototipo.domain.model.Registro
import com.devcris80.prototipo.domain.model.normalizarCodigo
import com.devcris80.prototipo.domain.repository.AnimalRepository
import com.devcris80.prototipo.domain.repository.ChapetaRepository
import com.devcris80.prototipo.domain.repository.CuentaRepository
import com.devcris80.prototipo.domain.repository.PesajePendienteRepository
import com.devcris80.prototipo.domain.repository.RegistroRepository
import com.devcris80.prototipo.domain.repository.Transaccion
import java.util.UUID

/**
 * Guarda todo Pesaje válido, a nombre del Usuario de este dispositivo: como Registro si su
 * codigo tiene una Chapeta activa de un Animal activo, o como Pesaje pendiente si no. Nunca
 * crea Animales ni Chapetas. Recibir dos veces el mismo Pesaje no crea nada nuevo: responde lo
 * mismo que la primera vez, aunque el pendiente ya se haya conciliado (ver docs/adr/0003).
 *
 * La hora que se guarda es la de la medición: el momento de recepción menos la antigüedad que
 * reporta el ESP32. Nunca se usa un reloj enviado por el ESP32.
 */
class RegistrarPesaje(
    private val chapetaRepository: ChapetaRepository,
    private val animalRepository: AnimalRepository,
    private val registroRepository: RegistroRepository,
    private val pesajePendienteRepository: PesajePendienteRepository,
    private val cuentaRepository: CuentaRepository,
    private val transaccion: Transaccion,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    sealed interface Resultado {
        data class Registrado(val idRegistro: String) : Resultado
        data class Pendiente(val idPesajePendiente: String, val motivo: MotivoPesajePendiente) : Resultado
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

        // Primero entre los pendientes: uno ya asignado tiene además un Registro con su idLectura,
        // y el reintento debe recibir lo mismo que la primera vez, no ese Registro.
        pesajePendienteRepository.findByIdLectura(pesaje.idLectura)?.let { yaPendiente ->
            return Resultado.Pendiente(yaPendiente.idPesajePendiente, yaPendiente.motivo)
        }
        registroRepository.findByIdLectura(pesaje.idLectura)?.let { yaRegistrado ->
            return Resultado.Registrado(yaRegistrado.idRegistro)
        }

        val usuario = cuentaRepository.getUsuario() ?: return Resultado.SinUsuario
        val animal = chapetaRepository.findActivaByCodigo(codigo)?.let { animalRepository.findById(it.idAnimal) }

        if (animal == null || animal.fechaBaja != null) {
            val pendiente = PesajePendiente(
                idPesajePendiente = UUID.randomUUID().toString(),
                idLectura = pesaje.idLectura,
                codigo = codigo,
                peso = pesaje.peso,
                timestamp = timestamp,
                idUsuario = usuario.idUsuario,
                motivo = if (animal == null) {
                    MotivoPesajePendiente.CODIGO_SIN_CHAPETA_ACTIVA
                } else {
                    MotivoPesajePendiente.ANIMAL_DADO_DE_BAJA
                },
            )
            pesajePendienteRepository.insert(pendiente)
            return Resultado.Pendiente(pendiente.idPesajePendiente, pendiente.motivo)
        }

        val registro = Registro(
            idRegistro = UUID.randomUUID().toString(),
            idAnimal = animal.idAnimal,
            idUsuario = usuario.idUsuario,
            idLectura = pesaje.idLectura,
            peso = pesaje.peso,
            timestamp = timestamp,
        )
        registroRepository.insert(registro)
        return Resultado.Registrado(registro.idRegistro)
    }
}
