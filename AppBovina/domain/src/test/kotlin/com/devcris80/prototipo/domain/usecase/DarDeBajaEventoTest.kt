package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.Evento
import com.devcris80.prototipo.domain.model.Rol
import com.devcris80.prototipo.domain.model.TipoEvento
import com.devcris80.prototipo.domain.model.Usuario
import com.devcris80.prototipo.domain.usecase.DarDeBajaEvento.Resultado
import com.devcris80.prototipo.testing.FakeCuentaRepository
import com.devcris80.prototipo.testing.FakeEventoRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DarDeBajaEventoTest {
    private val eventoRepository = FakeEventoRepository()
    private val cuentaRepository = FakeCuentaRepository()
    private val darDeBajaEvento = DarDeBajaEvento(eventoRepository, cuentaRepository, clock = { 5L })

    private fun conUsuario(rol: Rol) {
        cuentaRepository.usuario.value = Usuario("usuario-1", "perfil_local", "Ana", rol)
        eventoRepository.set(
            "vaca-1",
            listOf(Evento("evento-1", "vaca-1", "usuario-1", TipoEvento.VACUNACION.nombre, 1_000L, "Vacunación", biologico = "Aftosa")),
        )
    }

    @Test
    fun elGanaderoDaDeBajaUnEventoYDejaDeAparecerPeroSeConserva() = runTest {
        conUsuario(Rol.GANADERO)

        val resultado = darDeBajaEvento("evento-1")

        assertEquals(Resultado.Realizado, resultado)
        assertTrue(eventoRepository.observeByAnimal("vaca-1").first().isEmpty())
        assertTrue(eventoRepository.observeActivos().first().isEmpty())
        assertEquals(5L, eventoRepository.findById("evento-1")!!.fechaBaja)
    }

    @Test
    fun elOperarioNoDaDeBajaUnEventoAunqueSeLlameAlCasoDeUso() = runTest {
        conUsuario(Rol.OPERARIO)

        val resultado = darDeBajaEvento("evento-1")

        assertEquals(Resultado.NoPermitido, resultado)
        assertEquals(1, eventoRepository.observeByAnimal("vaca-1").first().size)
    }

    @Test
    fun unEventoInexistenteODadoDeBajaNoSeVuelveADarDeBaja() = runTest {
        conUsuario(Rol.GANADERO)
        darDeBajaEvento("evento-1")

        assertEquals(Resultado.NoDisponible, darDeBajaEvento("evento-1"))
        assertEquals(Resultado.NoDisponible, darDeBajaEvento("no-existe"))
        assertEquals(5L, eventoRepository.findById("evento-1")!!.fechaBaja)
    }
}
