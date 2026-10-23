package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.Pesaje
import com.devcris80.prototipo.domain.model.Registro
import com.devcris80.prototipo.domain.repository.AnimalRepository
import com.devcris80.prototipo.domain.repository.CuentaRepository
import com.devcris80.prototipo.domain.repository.RegistroRepository
import com.devcris80.prototipo.domain.repository.Transaccion
import java.util.UUID

/**
 * Guarda un pesaje manual (RF-27): un Registro que una persona digita, sin pasar por el ESP32.
 * Queda sin `idLectura`, a nombre del Usuario de este dispositivo y con la fecha y hora del
 * pesaje que se ingresó, que no puede ser futura.
 */
class RegistrarPesajeManual(
    private val animalRepository: AnimalRepository,
    private val registroRepository: RegistroRepository,
    private val cuentaRepository: CuentaRepository,
    private val transaccion: Transaccion,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    sealed interface Resultado {
        data object Registrado : Resultado

        /** El peso no cumple RN-07 ([Pesaje.pesoValido]). */
        data object PesoFueraDeRango : Resultado

        data object FechaFutura : Resultado

        /** El Animal no existe o está dado de baja. */
        data object AnimalNoDisponible : Resultado
    }

    suspend operator fun invoke(idAnimal: String, peso: Float, timestamp: Long): Resultado {
        if (!Pesaje.pesoValido(peso)) return Resultado.PesoFueraDeRango
        if (timestamp > clock()) return Resultado.FechaFutura
        // En la misma transacción que el insert, para que el Animal no se dé de baja entre medio.
        return transaccion.ejecutar {
            if (animalRepository.findById(idAnimal)?.activo != true) return@ejecutar Resultado.AnimalNoDisponible
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
            Resultado.Registrado
        }
    }
}
