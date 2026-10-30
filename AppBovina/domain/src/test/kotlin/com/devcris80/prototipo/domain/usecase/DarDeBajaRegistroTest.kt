package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.Registro
import com.devcris80.prototipo.domain.model.Rol
import com.devcris80.prototipo.domain.model.Usuario
import com.devcris80.prototipo.domain.usecase.DarDeBajaRegistro.Resultado
import com.devcris80.prototipo.testing.FakeCuentaRepository
import com.devcris80.prototipo.testing.FakeRegistroRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DarDeBajaRegistroTest {
    private val registroRepository = FakeRegistroRepository()
    private val cuentaRepository = FakeCuentaRepository()
    private val darDeBajaRegistro = DarDeBajaRegistro(registroRepository, cuentaRepository, clock = { 5L })

    private fun conUsuario(rol: Rol) {
        cuentaRepository.usuario.value = Usuario("usuario-1", "perfil_local", "Ana", rol)
        registroRepository.set("animal-1", listOf(Registro("registro-1", "animal-1", "usuario-1", null, 300f, 1_000L)))
    }

    @Test
    fun elGanaderoDaDeBajaUnPesajeYDejaDeAparecerEnElHistorialPeroSeConserva() = runTest {
        conUsuario(Rol.GANADERO)

        val resultado = darDeBajaRegistro("registro-1")

        assertEquals(Resultado.Realizado, resultado)
        assertTrue(registroRepository.observeByAnimal("animal-1").first().isEmpty())
        assertEquals(5L, registroRepository.findById("registro-1")!!.fechaBaja)
    }

    @Test
    fun elOperarioNoDaDeBajaUnPesajeAunqueSeLlameAlCasoDeUso() = runTest {
        conUsuario(Rol.OPERARIO)

        val resultado = darDeBajaRegistro("registro-1")

        assertEquals(Resultado.NoPermitido, resultado)
        assertEquals(1, registroRepository.observeByAnimal("animal-1").first().size)
    }

    @Test
    fun unPesajeInexistenteODadoDeBajaNoSeVuelveADarDeBaja() = runTest {
        conUsuario(Rol.GANADERO)
        darDeBajaRegistro("registro-1")

        assertEquals(Resultado.NoDisponible, darDeBajaRegistro("registro-1"))
        assertEquals(Resultado.NoDisponible, darDeBajaRegistro("no-existe"))
        assertEquals(5L, registroRepository.findById("registro-1")!!.fechaBaja)
    }
}
