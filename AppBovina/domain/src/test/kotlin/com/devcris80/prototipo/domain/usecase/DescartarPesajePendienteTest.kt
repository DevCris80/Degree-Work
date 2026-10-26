package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.MotivoPesajePendiente
import com.devcris80.prototipo.domain.model.PesajePendiente
import com.devcris80.prototipo.domain.model.Rol
import com.devcris80.prototipo.domain.model.Usuario
import com.devcris80.prototipo.testing.FakeCuentaRepository
import com.devcris80.prototipo.testing.FakePesajePendienteRepository
import com.devcris80.prototipo.testing.FakeTransaccion
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DescartarPesajePendienteTest {
    private val pesajePendienteRepository = FakePesajePendienteRepository()
    private val cuentaRepository = FakeCuentaRepository()
    private val descartarPesajePendiente =
        DescartarPesajePendiente(pesajePendienteRepository, cuentaRepository, FakeTransaccion(), clock = { 7L })

    private fun conPendiente(rol: Rol?) {
        cuentaRepository.usuario.value = rol?.let { Usuario("usuario-1", "perfil_local", "Ana", it) }
        pesajePendienteRepository.set(
            PesajePendiente(
                idPesajePendiente = "pendiente-1",
                idLectura = "lectura-1",
                codigo = "A1",
                peso = 100f,
                timestamp = 1L,
                idUsuario = "usuario-1",
                motivo = MotivoPesajePendiente.CODIGO_SIN_CHAPETA_ACTIVA,
            ),
        )
    }

    private fun conciliacionGuardada() =
        pesajePendienteRepository.todos.value.single().conciliacion

    @Test
    fun elGanaderoDescartaUnPesajePendiente() = runTest {
        conPendiente(Rol.GANADERO)

        val resultado = descartarPesajePendiente("pendiente-1")

        assertEquals(DescartarPesajePendiente.Resultado.Descartado, resultado)
        assertNotNull(conciliacionGuardada())
    }

    @Test
    fun elOperarioNoDescartaUnPesajePendienteAunqueSeLlameAlCasoDeUso() = runTest {
        conPendiente(Rol.OPERARIO)

        val resultado = descartarPesajePendiente("pendiente-1")

        assertEquals(DescartarPesajePendiente.Resultado.NoPermitido, resultado)
        assertNull(conciliacionGuardada())
    }

    @Test
    fun sinUsuarioEnElDispositivoNoSeDescartaElPesajePendiente() = runTest {
        conPendiente(null)

        assertEquals(DescartarPesajePendiente.Resultado.NoPermitido, descartarPesajePendiente("pendiente-1"))
        assertNull(conciliacionGuardada())
    }
}
