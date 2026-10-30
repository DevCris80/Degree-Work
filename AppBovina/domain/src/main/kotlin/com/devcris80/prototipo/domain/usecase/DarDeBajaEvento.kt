package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.puedeAdministrar
import com.devcris80.prototipo.domain.repository.CuentaRepository
import com.devcris80.prototipo.domain.repository.EventoRepository

/**
 * Da de baja un Evento (RF-31, CU-19, RN-04): deja de aparecer en el historial, en la pestaña Eventos y en los
 * indicadores, pero se conserva. Solo el Ganadero puede; el Operario es rechazado aunque se llame directo, también
 * sin señal (matriz 2.1). La baja es definitiva.
 */
class DarDeBajaEvento(
    private val eventoRepository: EventoRepository,
    private val cuentaRepository: CuentaRepository,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    sealed interface Resultado {
        data object Realizado : Resultado

        /** El Usuario de este dispositivo no es Ganadero. */
        data object NoPermitido : Resultado

        /** El Evento no existe o ya está dado de baja. */
        data object NoDisponible : Resultado
    }

    suspend operator fun invoke(idEvento: String): Resultado {
        if (!cuentaRepository.getUsuario().puedeAdministrar) return Resultado.NoPermitido
        val evento = eventoRepository.findById(idEvento)
        if (evento == null || evento.fechaBaja != null) return Resultado.NoDisponible
        eventoRepository.darDeBaja(idEvento, clock())
        return Resultado.Realizado
    }
}
