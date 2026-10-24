package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.Conflicto
import com.devcris80.prototipo.domain.model.Eleccion
import com.devcris80.prototipo.domain.repository.ConflictoRepository

/**
 * Cierra un Conflicto con lo que la persona eligió (RF-34, ADR-0004 regla 5). Resuelve solo los Siblings del
 * [Conflicto] que la persona vio: uno que llegue después de abrirlo queda sin resolver.
 */
class ResolverConflicto(private val conflictoRepository: ConflictoRepository) {
    sealed interface Resultado {
        data object Resuelto : Resultado

        /** La fila o el Sibling elegido ya no están, o no se pueden leer: no se escribió nada. */
        data object YaNoExiste : Resultado
    }

    suspend operator fun invoke(conflicto: Conflicto, eleccion: Eleccion): Resultado {
        val idsResueltos = conflicto.siblings.map { it.sibling.idVersion }
        require(idsResueltos.isNotEmpty()) { "El Conflicto no tiene Siblings por resolver" }
        val idVersionElegida = when (eleccion) {
            Eleccion.Vigente -> null
            is Eleccion.DeSibling -> {
                require(eleccion.idVersion in idsResueltos) { "El Sibling ${eleccion.idVersion} no es de este Conflicto" }
                eleccion.idVersion
            }
        }
        val resuelto = conflictoRepository.resolver(conflicto.entidad, conflicto.idFila, idVersionElegida, idsResueltos)
        return if (resuelto) Resultado.Resuelto else Resultado.YaNoExiste
    }
}
