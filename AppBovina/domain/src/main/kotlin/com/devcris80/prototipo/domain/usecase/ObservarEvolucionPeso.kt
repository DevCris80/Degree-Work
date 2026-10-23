package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.Registro
import com.devcris80.prototipo.domain.repository.RegistroRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class EvolucionPeso(
    /** Registros del Animal que no están dados de baja, del más antiguo al más reciente. */
    val registros: List<Registro>,
    /**
     * kg por día entre el primer y el último Registro; nula con menos de dos, o si entre ellos hay
     * menos de [ObservarEvolucionPeso.DIAS_MINIMOS] días.
     */
    val gananciaDiariaKg: Double?,
)

/** La evolución de peso de un Animal y su ganancia diaria de peso promedio (RF-40). */
class ObservarEvolucionPeso(
    private val registroRepository: RegistroRepository,
) {
    operator fun invoke(idAnimal: String): Flow<EvolucionPeso> =
        registroRepository.observeByAnimal(idAnimal).map { registros ->
            val ordenados = registros.sortedBy { it.timestamp }
            EvolucionPeso(registros = ordenados, gananciaDiariaKg = gananciaDiaria(ordenados))
        }

    private fun gananciaDiaria(ordenados: List<Registro>): Double? {
        if (ordenados.size < 2) return null
        val primero = ordenados.first()
        val ultimo = ordenados.last()
        val dias = (ultimo.timestamp - primero.timestamp).toDouble() / MILIS_POR_DIA
        return if (dias >= DIAS_MINIMOS) (ultimo.peso - primero.peso) / dias else null
    }

    companion object {
        /** Con menos días entre pesajes la ganancia no dice nada: en una hora, 5 kg serían 120 kg/día. */
        const val DIAS_MINIMOS = 1
    }
}
