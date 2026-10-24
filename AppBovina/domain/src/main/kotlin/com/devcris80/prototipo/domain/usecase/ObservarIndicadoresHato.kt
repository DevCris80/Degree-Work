package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.Evento
import com.devcris80.prototipo.domain.model.PesajePendiente
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.model.Registro
import com.devcris80.prototipo.domain.repository.AnimalRepository
import com.devcris80.prototipo.domain.repository.ChapetaRepository
import com.devcris80.prototipo.domain.repository.EventoRepository
import com.devcris80.prototipo.domain.repository.PesajePendienteRepository
import com.devcris80.prototipo.domain.repository.RegistroRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Cuántos Animales activos hay de un propósito y una etapa; la etapa ya dice el sexo. */
data class GrupoInventario(val proposito: Proposito, val etapa: Etapa, val cantidad: Int)

/** Cuántos Eventos hubo en un mes, por tipo. */
data class EventosDelMes(val mes: AnioMes, val porTipo: Map<String, Int>)

data class IndicadoresHato(
    /** Solo los grupos con algún Animal, por propósito y luego por etapa. */
    val inventario: List<GrupoInventario>,
    /** Pesajes pendientes sin resolver. */
    val pesajesPendientes: Int,
    /** El Pesaje pendiente sin resolver que llegó primero; nulo si no hay ninguno. */
    val pesajePendienteMasAntiguo: PesajePendiente?,
    val animalesSinChapetaActiva: List<Animal>,
    /** Animales sin Registro en los últimos [ObservarIndicadoresHato.DIAS_SIN_PESAJE] días. */
    val animalesSinPesajeReciente: List<Animal>,
    /** Solo los meses con algún Evento, del más reciente al más antiguo (RF-42). */
    val eventosPorMes: List<EventosDelMes>,
)

/**
 * Los indicadores del hato, calculados sobre la copia local (LIM-05). Lo dado de baja no cuenta
 * en ninguno (RN-04).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ObservarIndicadoresHato(
    private val animalRepository: AnimalRepository,
    private val chapetaRepository: ChapetaRepository,
    private val registroRepository: RegistroRepository,
    private val pesajePendienteRepository: PesajePendienteRepository,
    private val eventoRepository: EventoRepository,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    operator fun invoke(): Flow<IndicadoresHato> = combine(
        animalesConUltimoRegistro(),
        chapetaRepository.observeActivas(),
        pesajePendienteRepository.observeSinResolver(),
        eventoRepository.observeActivos(),
    ) { animales, chapetasActivas, pendientes, eventos ->
        val idsActivos = animales.map { it.animal.idAnimal }.toSet()
        val conChapetaActiva = chapetasActivas.map { it.idAnimal }.toSet()
        val desde = clock() - DIAS_SIN_PESAJE * MILIS_POR_DIA
        IndicadoresHato(
            inventario = animales
                .groupingBy { it.animal.proposito to it.animal.etapa }
                .eachCount()
                .map { (grupo, cantidad) -> GrupoInventario(grupo.first, grupo.second, cantidad) }
                .sortedWith(compareBy({ it.proposito }, { it.etapa })),
            pesajesPendientes = pendientes.size,
            pesajePendienteMasAntiguo = pendientes.minByOrNull { it.timestamp },
            animalesSinChapetaActiva = animales
                .map { it.animal }
                .filter { it.idAnimal !in conChapetaActiva },
            animalesSinPesajeReciente = animales
                .filter { it.ultimoRegistro == null || it.ultimoRegistro.timestamp < desde }
                .map { it.animal },
            eventosPorMes = eventosPorMes(eventos.filter { it.idAnimal in idsActivos }),
        )
    }

    // Cada tipo se muestra como lo escriben más veces, para que "BVD" no pase a "Bvd".
    private fun eventosPorMes(eventos: List<Evento>): List<EventosDelMes> {
        val nombrePorClave = eventos.groupBy { it.claveDeTipo }.mapValues { (_, delTipo) ->
            delTipo.groupingBy { it.tipoEscrito }.eachCount().maxBy { it.value }.key
        }
        return eventos
            .groupBy { AnioMes.de(it.fecha) }
            .map { (mes, delMes) ->
                EventosDelMes(mes, delMes.groupingBy { nombrePorClave.getValue(it.claveDeTipo) }.eachCount())
            }
            .sortedByDescending { it.mes }
    }

    private data class AnimalConUltimoRegistro(val animal: Animal, val ultimoRegistro: Registro?)

    private fun animalesConUltimoRegistro(): Flow<List<AnimalConUltimoRegistro>> =
        animalRepository.observeActivos().flatMapLatest { animales ->
            if (animales.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(
                    animales.map { animal ->
                        registroRepository.observeUltimoByAnimal(animal.idAnimal).map { ultimo ->
                            AnimalConUltimoRegistro(animal, ultimo)
                        }
                    },
                ) { it.toList() }
            }
        }

    companion object {
        /** La N de RF-43: un Animal sin pesaje en este número de días tiene el historial incompleto. */
        const val DIAS_SIN_PESAJE = 30
    }
}
