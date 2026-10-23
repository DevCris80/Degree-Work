package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.Pesaje
import com.devcris80.prototipo.domain.model.Registro
import com.devcris80.prototipo.domain.repository.AnimalRepository
import com.devcris80.prototipo.domain.repository.CuentaRepository
import com.devcris80.prototipo.domain.repository.RegistroRepository
import java.util.UUID

/**
 * Guarda un Registro que una persona digita, sin pasar por el ESP32 (RF-27): queda sin
 * `idLectura`, a nombre del Usuario de este dispositivo y con la hora de la medición que se
 * ingresó, que no puede ser futura.
 */
class RegistrarPesajeManual(
    private val animalRepository: AnimalRepository,
    private val registroRepository: RegistroRepository,
    private val cuentaRepository: CuentaRepository,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    sealed interface Resultado {
        data object Registrado : Resultado

        /** El peso no cumple 0 < peso ≤ [Pesaje.PESO_MAXIMO_KG] (RN-07). */
        data object PesoFueraDeRango : Resultado

        data object FechaFutura : Resultado

        /** El Animal no existe o está dado de baja. */
        data object AnimalNoDisponible : Resultado
    }

    suspend operator fun invoke(idAnimal: String, peso: Float, timestamp: Long): Resultado {
        // `!(peso > 0)` en vez de `peso <= 0` para rechazar también un peso que no sea un número.
        if (!(peso > 0f) || peso > Pesaje.PESO_MAXIMO_KG) return Resultado.PesoFueraDeRango
        if (timestamp > clock()) return Resultado.FechaFutura
        val animal = animalRepository.findById(idAnimal)
        if (animal == null || animal.fechaBaja != null) return Resultado.AnimalNoDisponible
        // Solo se llega al Detalle de un Animal con una cuenta creada, así que aquí hay Usuario.
        val usuario = checkNotNull(cuentaRepository.getUsuario()) { "No hay Usuario que registre el pesaje" }
        registroRepository.insert(
            Registro(
                idRegistro = UUID.randomUUID().toString(),
                idAnimal = idAnimal,
                idUsuario = usuario.idUsuario,
                peso = peso,
                timestamp = timestamp,
            ),
        )
        return Resultado.Registrado
    }
}
