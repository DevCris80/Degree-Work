package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.Conciliacion
import com.devcris80.prototipo.domain.model.Registro
import com.devcris80.prototipo.domain.model.ResolucionPesajePendiente
import com.devcris80.prototipo.domain.repository.AnimalRepository
import com.devcris80.prototipo.domain.repository.CuentaRepository
import com.devcris80.prototipo.domain.repository.PesajePendienteRepository
import com.devcris80.prototipo.domain.repository.RegistroRepository
import com.devcris80.prototipo.domain.repository.Transaccion
import java.util.UUID

/**
 * Concilia Pesajes pendientes asignándolos a un Animal: cada uno crea su Registro y queda
 * resuelto, a nombre del Usuario de este dispositivo. Todo o nada: si el Animal no está activo o
 * algún pendiente ya está resuelto, no se guarda ninguno.
 *
 * El Animal lo elige quien concilia; no tiene que ser el de la Chapeta con ese codigo. El
 * Registro conserva el idLectura, la hora de medición y el Usuario que recibió el Pesaje, como
 * si se hubiera asignado al llegar.
 */
class AsignarPesajesPendientes(
    private val pesajePendienteRepository: PesajePendienteRepository,
    private val animalRepository: AnimalRepository,
    private val registroRepository: RegistroRepository,
    private val cuentaRepository: CuentaRepository,
    private val transaccion: Transaccion,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    sealed interface Resultado {
        data object Asignados : Resultado

        /** El Animal no existe o está dado de baja. */
        data object AnimalNoDisponible : Resultado

        /** Algún Pesaje pendiente no existe o ya está resuelto. */
        data object PendienteNoDisponible : Resultado
    }

    suspend operator fun invoke(idsPesajePendiente: List<String>, idAnimal: String): Resultado =
        transaccion.ejecutar {
            val animal = animalRepository.findById(idAnimal)
            if (animal == null || animal.fechaBaja != null) return@ejecutar Resultado.AnimalNoDisponible

            val pendientes = idsPesajePendiente.distinct().map { id ->
                pesajePendienteRepository.findById(id)?.takeIf { it.conciliacion == null }
                    ?: return@ejecutar Resultado.PendienteNoDisponible
            }

            // Un Pesaje pendiente solo se guarda con una cuenta creada, así que aquí hay Usuario.
            val usuario = checkNotNull(cuentaRepository.getUsuario()) { "No hay Usuario que concilie" }
            val conciliacion = Conciliacion(
                resolucion = ResolucionPesajePendiente.ASIGNADO,
                idUsuario = usuario.idUsuario,
                fecha = clock(),
            )
            pendientes.forEach { pendiente ->
                registroRepository.insert(
                    Registro(
                        idRegistro = UUID.randomUUID().toString(),
                        idAnimal = animal.idAnimal,
                        idUsuario = pendiente.idUsuario,
                        idLectura = pendiente.idLectura,
                        peso = pendiente.peso,
                        timestamp = pendiente.timestamp,
                    ),
                )
                pesajePendienteRepository.conciliar(pendiente.idPesajePendiente, conciliacion)
            }
            Resultado.Asignados
        }
}
