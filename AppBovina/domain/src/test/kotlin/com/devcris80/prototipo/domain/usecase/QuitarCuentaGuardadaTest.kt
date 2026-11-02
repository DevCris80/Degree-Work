package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.CuentaGuardada
import com.devcris80.prototipo.domain.model.Rol
import com.devcris80.prototipo.domain.repository.ResultadoQuitarCuenta
import com.devcris80.prototipo.testing.FakeCuentasGuardadasRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuitarCuentaGuardadaTest {
    private val cuentasGuardadas = FakeCuentasGuardadasRepository()
    private val quitarCuentaGuardada = QuitarCuentaGuardada(cuentasGuardadas)

    private fun cuenta(cambios: Int = 0, fotos: Int = 0, rechazos: Int = 0) = CuentaGuardada(
        idUsuario = "u-1",
        correo = "luis@finca.co",
        nombreUsuario = "Luis",
        rol = Rol.GANADERO,
        nombreFinca = "La Esperanza",
        cambiosSinSubir = cambios,
        fotosSinSubir = fotos,
        rechazosAbiertos = rechazos,
    )

    @Test
    fun unaCuentaSinNadaPorSubirSeQuita() = runTest {
        cuentasGuardadas.cuentas.value = listOf(cuenta())

        val resultado = quitarCuentaGuardada("u-1")

        assertEquals(ResultadoQuitarCuenta.Quitada, resultado)
        assertEquals(listOf("u-1"), cuentasGuardadas.quitadas)
    }

    @Test
    fun conCambiosFotosORechazosPorSubirNoSeQuita() = runTest {
        listOf(cuenta(cambios = 1), cuenta(fotos = 1), cuenta(rechazos = 1)).forEach { pendiente ->
            cuentasGuardadas.cuentas.value = listOf(pendiente)

            val resultado = quitarCuentaGuardada("u-1")

            assertEquals(ResultadoQuitarCuenta.TienePendientes, resultado)
        }
        assertTrue(cuentasGuardadas.quitadas.isEmpty())
    }

    @Test
    fun cuentaLoPendienteAunqueLaBaseNoTengaUsuarioNiFinca() = runTest {
        cuentasGuardadas.pendientesPorCuenta["u-1"] = 2

        val resultado = quitarCuentaGuardada("u-1")

        assertEquals(ResultadoQuitarCuenta.TienePendientes, resultado)
        assertTrue(cuentasGuardadas.quitadas.isEmpty())
    }
}
