package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.Chapeta
import com.devcris80.prototipo.domain.model.Rol
import com.devcris80.prototipo.domain.model.Usuario
import com.devcris80.prototipo.testing.FakeChapetaRepository
import com.devcris80.prototipo.testing.FakeCuentaRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LiberarChapetaTest {
    private val chapetaRepository = FakeChapetaRepository()
    private val cuentaRepository = FakeCuentaRepository()
    private val liberarChapeta = LiberarChapeta(chapetaRepository, cuentaRepository, clock = { 9L })

    private fun conChapeta(rol: Rol?) {
        cuentaRepository.usuario.value = rol?.let { Usuario("usuario-1", "perfil_local", "Ana", it) }
        chapetaRepository.set(Chapeta("chapeta-1", "A1", "animal-1", fechaAsociacion = 1L))
    }

    @Test
    fun elGanaderoLiberaUnaChapeta() = runTest {
        conChapeta(Rol.GANADERO)

        val resultado = liberarChapeta("chapeta-1")

        assertEquals(LiberarChapeta.Resultado.Realizado, resultado)
        assertEquals(9L, chapetaRepository.chapeta("chapeta-1")?.fechaDesasociacion)
    }

    @Test
    fun elOperarioNoLiberaUnaChapetaAunqueSeLlameAlCasoDeUso() = runTest {
        conChapeta(Rol.OPERARIO)

        val resultado = liberarChapeta("chapeta-1")

        assertEquals(LiberarChapeta.Resultado.NoPermitido, resultado)
        assertNull(chapetaRepository.chapeta("chapeta-1")?.fechaDesasociacion)
    }

    @Test
    fun sinUsuarioEnElDispositivoNoSeLiberaLaChapeta() = runTest {
        conChapeta(null)

        assertEquals(LiberarChapeta.Resultado.NoPermitido, liberarChapeta("chapeta-1"))
        assertNull(chapetaRepository.chapeta("chapeta-1")?.fechaDesasociacion)
    }
}
