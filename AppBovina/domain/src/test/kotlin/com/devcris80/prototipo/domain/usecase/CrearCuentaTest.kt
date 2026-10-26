package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.Rol
import com.devcris80.prototipo.testing.FakeCuentaRepository
import com.devcris80.prototipo.testing.FakeTransaccion
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CrearCuentaTest {
    private val cuentaRepository = FakeCuentaRepository()
    private val crearCuenta = CrearCuenta(cuentaRepository, FakeTransaccion())

    @Test
    fun creaLaFincaYSuPrimerUsuarioGanaderoEnLaMismaFinca() = runTest {
        val usuario = crearCuenta(nombreUsuario = "Ana", nombreFinca = "La Esperanza")

        val perfil = cuentaRepository.getPerfilFinca()!!
        assertEquals("La Esperanza", perfil.nombreFinca)
        assertEquals(usuario, cuentaRepository.getUsuario())
        assertEquals("Ana", usuario.nombre)
        assertEquals(Rol.GANADERO, usuario.rol)
        assertEquals(perfil.idPerfilFinca, usuario.idPerfilFinca)
        assertTrue(usuario.idUsuario.isNotBlank())
        assertNotEquals("el Usuario no reutiliza el id de la finca", perfil.idPerfilFinca, usuario.idUsuario)
    }
}
