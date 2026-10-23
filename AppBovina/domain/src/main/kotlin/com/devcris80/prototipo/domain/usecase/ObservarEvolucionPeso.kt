package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.Registro
import com.devcris80.prototipo.domain.repository.RegistroRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class EvolucionPeso(
    /** Registros del Animal que no están dados de baja, del más antiguo al más reciente. */
    val registros: List<Registro>,
    /** kg por día entre el primer y el último Registro; nula si no hay dos Registros en momentos distintos. */
    val gananciaDiariaKg: Double?,
)

/** La evolución de peso de un Animal y su ganancia diaria de peso promedio (RF-40). */
class ObservarEvolucionPeso(
    private val registroRepository: RegistroRepository,
) {
    operator fun invoke(idAnimal: String): Flow<EvolucionPeso> =
        registroRepository.observeByAnimal(idAnimal).map { registros ->
            val ordenados = registros.sortedBy { it.timestamp }
            val primero = ordenados.firstOrNull()
            val ultimo = ordenados.lastOrNull()
            val dias = if (primero != null && ultimo != null) {
                (ultimo.timestamp - primero.timestamp).toDouble() / MILIS_POR_DIA
            } else {
                0.0
            }
            EvolucionPeso(
                registros = ordenados,
                gananciaDiariaKg = if (dias > 0) (ultimo!!.peso - primero!!.peso) / dias else null,
            )
        }

    private companion object {
        const val MILIS_POR_DIA = 24L * 60 * 60 * 1000
    }
}
