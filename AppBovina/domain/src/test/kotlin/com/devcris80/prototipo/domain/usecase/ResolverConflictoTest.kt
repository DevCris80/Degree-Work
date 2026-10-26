package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.EntidadConRevisiones
import com.devcris80.prototipo.domain.model.Eleccion
import com.devcris80.prototipo.domain.usecase.ResolverConflicto.Resultado
import com.devcris80.prototipo.testing.FakeConflictoRepository
import com.devcris80.prototipo.testing.conflictoDeAnimal
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResolverConflictoTest {
    private val conflictoRepository = FakeConflictoRepository()
    private val resolverConflicto = ResolverConflicto(conflictoRepository)

    @Test
    fun conservarLaVigenteCierraLosSiblingsQueLaPersonaVio() = runTest {
        val conflicto = conflictoDeAnimal("a1", "v1" to "Jersey", "v2" to "Cebú")
        conflictoRepository.conflictos.value = listOf(conflicto)

        val resultado = resolverConflicto(conflicto, Eleccion.Vigente)

        assertEquals(Resultado.Resuelto, resultado)
        assertEquals(
            listOf(FakeConflictoRepository.Resolucion(EntidadConRevisiones.ANIMAL, "a1", null, listOf("v1", "v2"))),
            conflictoRepository.resoluciones,
        )
    }

    @Test
    fun elegirUnSiblingSeQuedaConSuFilaYCierraSoloLosQueSeVieron() = runTest {
        val visto = conflictoDeAnimal("a1", "v1" to "Jersey")

        resolverConflicto(visto, Eleccion.DeSibling("v1"))

        assertEquals(
            FakeConflictoRepository.Resolucion(EntidadConRevisiones.ANIMAL, "a1", "v1", listOf("v1")),
            conflictoRepository.resoluciones.single(),
        )
    }

    @Test
    fun unSiblingQueNoEsDelConflictoSeRechazaYNoEscribeNada() = runTest {
        val conflicto = conflictoDeAnimal("a1", "v1" to "Jersey")

        val error = runCatching { resolverConflicto(conflicto, Eleccion.DeSibling("de-otro")) }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertTrue(conflictoRepository.resoluciones.isEmpty())
    }

    @Test
    fun unConflictoSinSiblingsSeRechazaYNoEscribeNada() = runTest {
        val error = runCatching { resolverConflicto(conflictoDeAnimal("a1"), Eleccion.Vigente) }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertTrue(conflictoRepository.resoluciones.isEmpty())
    }

    @Test
    fun siLaFilaYaNoExisteAvisaYNoEscribe() = runTest {
        conflictoRepository.puedeResolver = false

        val resultado = resolverConflicto(conflictoDeAnimal("a1", "v1" to "Jersey"), Eleccion.Vigente)

        assertEquals(Resultado.YaNoExiste, resultado)
        assertTrue(conflictoRepository.resoluciones.isEmpty())
    }
}
