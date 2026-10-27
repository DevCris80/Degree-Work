package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.Evento
import com.devcris80.prototipo.domain.model.Sexo
import com.devcris80.prototipo.domain.model.TipoEvento
import com.devcris80.prototipo.domain.repository.AnimalRepository
import com.devcris80.prototipo.domain.repository.CuentaRepository
import com.devcris80.prototipo.domain.repository.EventoRepository
import com.devcris80.prototipo.domain.repository.Transaccion
import java.util.UUID

/**
 * Registra un Evento de un Animal a nombre del Usuario del dispositivo. La [fecha] puede ser pasada o
 * futura: un Evento futuro es un Evento programado.
 *
 * Cada tipo guarda solo sus campos: la vacunación, el biológico, la dosis y la vía; el parto, la cría. En un
 * parto con cría, la cría queda con el Animal del Evento como madre si es hembra, o como padre si es macho,
 * en la misma transacción que el Evento.
 */
class RegistrarEvento(
    private val animalRepository: AnimalRepository,
    private val eventoRepository: EventoRepository,
    private val cuentaRepository: CuentaRepository,
    private val transaccion: Transaccion,
) {
    sealed interface Resultado {
        data class Registrado(val idEvento: String) : Resultado

        /** No hay Usuario en este dispositivo a cuyo nombre registrarlo. */
        data object SinUsuario : Resultado

        /** El Animal no existe o está dado de baja. */
        data object AnimalNoDisponible : Resultado

        /** La cría de un parto no existe, está dada de baja o es el mismo Animal del Evento. */
        data object CriaNoValida : Resultado
    }

    suspend operator fun invoke(
        idAnimal: String,
        tipo: TipoEvento,
        detalle: String,
        fecha: Long,
        biologico: String? = null,
        dosis: String? = null,
        via: String? = null,
        idCriaResultante: String? = null,
    ): Resultado = transaccion.ejecutar {
        val usuario = cuentaRepository.getUsuario() ?: return@ejecutar Resultado.SinUsuario
        val animal = animalRepository.findById(idAnimal)?.takeIf { it.activo }
            ?: return@ejecutar Resultado.AnimalNoDisponible
        val cria = idCriaResultante?.takeIf { tipo == TipoEvento.PARTO }?.let { idCria ->
            animalRepository.findById(idCria)?.takeIf { it.activo && it.idAnimal != animal.idAnimal }
                ?: return@ejecutar Resultado.CriaNoValida
        }
        val evento = Evento(
            idEvento = UUID.randomUUID().toString(),
            idAnimal = idAnimal,
            idUsuario = usuario.idUsuario,
            tipoEvento = tipo.nombre,
            fecha = fecha,
            detalle = detalle,
            biologico = biologico.takeIf { tipo == TipoEvento.VACUNACION }.limpio(),
            dosis = dosis.takeIf { tipo == TipoEvento.VACUNACION }.limpio(),
            via = via.takeIf { tipo == TipoEvento.VACUNACION }.limpio(),
            idCriaResultante = cria?.idAnimal,
        )
        eventoRepository.insert(evento)
        if (cria != null) {
            val conProgenitor =
                if (animal.sexo == Sexo.HEMBRA) cria.copy(idMadre = animal.idAnimal) else cria.copy(idPadre = animal.idAnimal)
            if (conProgenitor != cria) animalRepository.update(conProgenitor)
        }
        Resultado.Registrado(evento.idEvento)
    }
}

private fun String?.limpio(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
