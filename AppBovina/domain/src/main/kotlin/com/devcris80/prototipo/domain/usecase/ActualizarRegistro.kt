package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.Pesaje
import com.devcris80.prototipo.domain.repository.AnimalRepository
import com.devcris80.prototipo.domain.repository.RegistroRepository
import com.devcris80.prototipo.domain.repository.Transaccion

/**
 * Edita un pesaje (RF-30, CU-18): su peso y su hora de medición, con las validaciones de RN-07 y la
 * misma regla de fecha que un pesaje manual. El Registro conserva su Animal, su autor y su `idLectura`,
 * así que un pesaje del ESP32 sigue siendo el de esa lectura. El Operario puede editar (SRS 2.1).
 */
class ActualizarRegistro(
    private val animalRepository: AnimalRepository,
    private val registroRepository: RegistroRepository,
    private val transaccion: Transaccion,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    sealed interface Resultado {
        data object Actualizado : Resultado

        /** El peso no cumple RN-07 ([Pesaje.pesoValido]). */
        data object PesoFueraDeRango : Resultado

        data object FechaFutura : Resultado

        /** El Registro no existe o está dado de baja, o su Animal está dado de baja. */
        data object NoDisponible : Resultado
    }

    suspend operator fun invoke(idRegistro: String, peso: Float, timestamp: Long): Resultado {
        if (!Pesaje.pesoValido(peso)) return Resultado.PesoFueraDeRango
        if (timestamp > clock()) return Resultado.FechaFutura
        return transaccion.ejecutar {
            val registro = registroRepository.findById(idRegistro)?.takeIf { it.fechaBaja == null }
                ?: return@ejecutar Resultado.NoDisponible
            if (animalRepository.findById(registro.idAnimal)?.activo != true) return@ejecutar Resultado.NoDisponible
            registroRepository.update(registro.copy(peso = peso, timestamp = timestamp))
            Resultado.Actualizado
        }
    }
}
