package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.model.Rol
import com.devcris80.prototipo.domain.model.Sexo
import com.devcris80.prototipo.domain.model.Usuario
import com.devcris80.prototipo.domain.usecase.RegistrarPesajeManual.Resultado
import com.devcris80.prototipo.testing.FakeAnimalRepository
import com.devcris80.prototipo.testing.FakeCuentaRepository
import com.devcris80.prototipo.testing.FakeRegistroRepository
import com.devcris80.prototipo.testing.FakeTransaccion
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RegistrarPesajeManualTest {
    private val animalRepository = FakeAnimalRepository()
    private val registroRepository = FakeRegistroRepository()
    private val cuentaRepository = FakeCuentaRepository()
    private val registrarPesajeManual = RegistrarPesajeManual(
        animalRepository,
        registroRepository,
        cuentaRepository,
        FakeTransaccion(),
        clock = { AHORA },
    )

    private fun conAnimal(fechaBaja: Long? = null) {
        cuentaRepository.usuario.value = Usuario("usuario-1", "perfil_local", "Ana", Rol.OPERARIO)
        animalRepository.set(
            Animal("animal-1", "perfil_local", "Manchas", "Holstein", Sexo.HEMBRA, Etapa.VACA, 1L, proposito = Proposito.LECHE, fechaBaja = fechaBaja),
        )
    }

    private suspend fun registros() = registroRepository.observeByAnimal("animal-1").first()

    @Test
    fun guardaUnRegistroSinIdLecturaANombreDelUsuarioConLaFechaIngresada() = runTest {
        conAnimal()

        val resultado = registrarPesajeManual("animal-1", peso = 320f, timestamp = AHORA - 60_000)

        assertEquals(Resultado.Registrado, resultado)
        val registro = registros().single()
        assertEquals("animal-1", registro.idAnimal)
        assertEquals("usuario-1", registro.idUsuario)
        assertEquals(320f, registro.peso)
        assertEquals(AHORA - 60_000, registro.timestamp)
        assertNull(registro.idLectura)
    }

    @Test
    fun unPesoFueraDeRangoNoSeGuarda() = runTest {
        conAnimal()

        assertEquals(Resultado.PesoFueraDeRango, registrarPesajeManual("animal-1", peso = 0f, timestamp = AHORA))
        assertEquals(Resultado.PesoFueraDeRango, registrarPesajeManual("animal-1", peso = 1501f, timestamp = AHORA))
        assertTrue(registros().isEmpty())
    }

    @Test
    fun unaFechaFuturaNoSeGuarda() = runTest {
        conAnimal()

        assertEquals(Resultado.FechaFutura, registrarPesajeManual("animal-1", peso = 320f, timestamp = AHORA + 1))
        assertTrue(registros().isEmpty())
    }

    @Test
    fun laFechaDeAhoraMismoSiSeGuarda() = runTest {
        conAnimal()

        assertEquals(Resultado.Registrado, registrarPesajeManual("animal-1", peso = 320f, timestamp = AHORA))
    }

    @Test
    fun unAnimalDadoDeBajaOInexistenteNoRecibePesajes() = runTest {
        conAnimal(fechaBaja = 5L)

        assertEquals(Resultado.AnimalNoDisponible, registrarPesajeManual("animal-1", peso = 320f, timestamp = AHORA))
        assertEquals(Resultado.AnimalNoDisponible, registrarPesajeManual("no-existe", peso = 320f, timestamp = AHORA))
        assertTrue(registros().isEmpty())
    }

    private companion object {
        const val AHORA = 1_000_000L
    }
}
