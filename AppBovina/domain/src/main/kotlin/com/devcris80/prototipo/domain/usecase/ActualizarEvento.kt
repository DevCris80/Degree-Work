package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.TipoEvento
import com.devcris80.prototipo.domain.repository.AnimalRepository
import com.devcris80.prototipo.domain.repository.EventoRepository
import com.devcris80.prototipo.domain.repository.Transaccion

/**
 * Edita un Evento (RF-30, CU-18): su título, su fecha y, en una vacunación, el biológico, la dosis y la vía. El
 * Evento conserva su Animal, su autor, su tipo y su cría. El Operario puede editar (SRS 2.1).
 */
class ActualizarEvento(
    private val animalRepository: AnimalRepository,
    private val eventoRepository: EventoRepository,
    private val transaccion: Transaccion,
) {
    sealed interface Resultado {
        data object Actualizado : Resultado

        /** Falta el título del Evento o, en una vacunación, el biológico. */
        data object FaltanDatos : Resultado

        /** El Evento no existe o está dado de baja, o su Animal está dado de baja. */
        data object NoDisponible : Resultado
    }

    suspend operator fun invoke(
        idEvento: String,
        detalle: String,
        fecha: Long,
        biologico: String? = null,
        dosis: String? = null,
        via: String? = null,
    ): Resultado = transaccion.ejecutar {
        val evento = eventoRepository.findById(idEvento)?.takeIf { it.fechaBaja == null }
            ?: return@ejecutar Resultado.NoDisponible
        if (animalRepository.findById(evento.idAnimal)?.activo != true) return@ejecutar Resultado.NoDisponible
        val titulo = detalle.trim()
        val esVacunacion = evento.tipoConocido == TipoEvento.VACUNACION
        val biologicoDeLaVacuna = biologico.takeIf { esVacunacion }.limpio()
        if (titulo.isEmpty() || (esVacunacion && biologicoDeLaVacuna == null)) return@ejecutar Resultado.FaltanDatos
        eventoRepository.update(
            evento.copy(
                detalle = titulo,
                fecha = fecha,
                biologico = biologicoDeLaVacuna,
                dosis = dosis.takeIf { esVacunacion }.limpio(),
                via = via.takeIf { esVacunacion }.limpio(),
            ),
        )
        Resultado.Actualizado
    }
}

private fun String?.limpio(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
