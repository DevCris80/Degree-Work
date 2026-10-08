package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.Conciliacion
import com.devcris80.prototipo.domain.model.ResolucionPesajePendiente
import com.devcris80.prototipo.domain.repository.CuentaRepository
import com.devcris80.prototipo.domain.repository.PesajePendienteRepository
import com.devcris80.prototipo.domain.repository.Transaccion

/**
 * Concilia un Pesaje pendiente descartándolo: no corresponde a ningún Animal. No crea ningún
 * Registro ni borra nada; el pendiente queda resuelto como descartado, a nombre del Usuario de
 * este dispositivo.
 */
class DescartarPesajePendiente(
    private val pesajePendienteRepository: PesajePendienteRepository,
    private val cuentaRepository: CuentaRepository,
    private val transaccion: Transaccion,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    sealed interface Resultado {
        data object Descartado : Resultado

        /** El Pesaje pendiente no existe o ya está resuelto. */
        data object PendienteNoDisponible : Resultado
    }

    suspend operator fun invoke(idPesajePendiente: String): Resultado = transaccion.ejecutar {
        val pendiente = pesajePendienteRepository.findById(idPesajePendiente)
        if (pendiente == null || pendiente.conciliacion != null) {
            return@ejecutar Resultado.PendienteNoDisponible
        }
        // Un Pesaje pendiente solo se guarda con una cuenta creada, así que aquí hay Usuario.
        val usuario = checkNotNull(cuentaRepository.getUsuario()) { "No hay Usuario que concilie" }
        pesajePendienteRepository.conciliar(
            idPesajePendiente,
            Conciliacion(
                resolucion = ResolucionPesajePendiente.DESCARTADO,
                idUsuario = usuario.idUsuario,
                fecha = clock(),
            ),
        )
        Resultado.Descartado
    }
}
