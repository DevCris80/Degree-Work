package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.Rol
import com.devcris80.prototipo.domain.model.Usuario
import com.devcris80.prototipo.domain.repository.ResultadoQuitarOperario
import com.devcris80.prototipo.testing.FakeCuentaRepository
import com.devcris80.prototipo.testing.FakeOperarioRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuitarOperarioTest {
    private val operarioRepository = FakeOperarioRepository()
    private val cuentaRepository = FakeCuentaRepository()
    private val quitarOperario = QuitarOperario(operarioRepository, cuentaRepository)

    private fun conUsuario(rol: Rol?) {
        cuentaRepository.usuario.value = rol?.let { Usuario("usuario-1", "perfil_local", "Ana", it) }
    }

    @Test
    fun elGanaderoQuitaAUnOperario() = runTest {
        conUsuario(Rol.GANADERO)

        val resultado = quitarOperario("operario-1")

        assertEquals(ResultadoQuitarOperario.Quitado, resultado)
        assertEquals(listOf("operario-1"), operarioRepository.quitados)
    }

    @Test
    fun elOperarioNoQuitaANadieAunqueSeLlameAlCasoDeUsoYNoLlegaAlServidor() = runTest {
        conUsuario(Rol.OPERARIO)

        val resultado = quitarOperario("operario-2")

        assertEquals(ResultadoQuitarOperario.NoPermitido, resultado)
        assertTrue(operarioRepository.quitados.isEmpty())
    }

    @Test
    fun sinUsuarioEnElDispositivoNoSeQuitaANadie() = runTest {
        conUsuario(null)

        assertEquals(ResultadoQuitarOperario.NoPermitido, quitarOperario("operario-1"))
        assertTrue(operarioRepository.quitados.isEmpty())
    }

    @Test
    fun devuelveLoQueContestaElServidor() = runTest {
        conUsuario(Rol.GANADERO)
        for (respuesta in listOf(
            ResultadoQuitarOperario.SinConexion,
            ResultadoQuitarOperario.NoDisponible,
            ResultadoQuitarOperario.NoPermitido,
            ResultadoQuitarOperario.NoSePudo,
        )) {
            operarioRepository.resultadoQuitar = respuesta

            assertEquals(respuesta, quitarOperario("operario-1"))
        }
    }
}
