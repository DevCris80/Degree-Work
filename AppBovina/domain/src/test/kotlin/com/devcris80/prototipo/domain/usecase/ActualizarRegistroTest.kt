package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.model.Registro
import com.devcris80.prototipo.domain.model.Sexo
import com.devcris80.prototipo.domain.usecase.ActualizarRegistro.Resultado
import com.devcris80.prototipo.testing.FakeAnimalRepository
import com.devcris80.prototipo.testing.FakeRegistroRepository
import com.devcris80.prototipo.testing.FakeTransaccion
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ActualizarRegistroTest {
    private val animalRepository = FakeAnimalRepository()
    private val registroRepository = FakeRegistroRepository()
    private val actualizarRegistro = ActualizarRegistro(
        animalRepository,
        registroRepository,
        FakeTransaccion(),
        clock = { AHORA },
    )

    private fun conAnimal(fechaBaja: Long? = null) = animalRepository.set(
        Animal("animal-1", "perfil_local", "Manchas", "Holstein", Sexo.HEMBRA, Etapa.VACA, 1L, proposito = Proposito.LECHE, fechaBaja = fechaBaja),
    )

    private fun conRegistro(idLectura: String? = null, fechaBaja: Long? = null) =
        Registro("registro-1", "animal-1", "usuario-1", idLectura, 300f, 1_000L, fechaBaja).also {
            registroRepository.set("animal-1", listOf(it))
        }

    private suspend fun registros() = registroRepository.observeByAnimal("animal-1").first()

    @Test
    fun cambiaElPesoYLaHoraDeMedicionYElHistorialMuestraElValorNuevo() = runTest {
        conAnimal()
        conRegistro()

        val resultado = actualizarRegistro("registro-1", peso = 355.5f, timestamp = 2_000L)

        assertEquals(Resultado.Actualizado, resultado)
        val registro = registros().single()
        assertEquals(355.5f, registro.peso)
        assertEquals(2_000L, registro.timestamp)
    }

    @Test
    fun unPesajeDelEsp32ConservaSuIdLecturaYSuAutor() = runTest {
        conAnimal()
        conRegistro(idLectura = "lectura-1")

        actualizarRegistro("registro-1", peso = 310f, timestamp = 1_500L)

        val registro = registros().single()
        assertEquals("lectura-1", registro.idLectura)
        assertEquals("usuario-1", registro.idUsuario)
        assertEquals("animal-1", registro.idAnimal)
    }

    @Test
    fun unPesoFueraDeRn07NoSeGuarda() = runTest {
        conAnimal()
        conRegistro()

        assertEquals(Resultado.PesoFueraDeRango, actualizarRegistro("registro-1", 0f, 2_000L))
        assertEquals(Resultado.PesoFueraDeRango, actualizarRegistro("registro-1", 1_500.1f, 2_000L))
        assertEquals(Resultado.PesoFueraDeRango, actualizarRegistro("registro-1", Float.NaN, 2_000L))

        assertEquals(300f, registros().single().peso)
    }

    @Test
    fun unaHoraFuturaNoSeGuarda() = runTest {
        conAnimal()
        conRegistro()

        val resultado = actualizarRegistro("registro-1", 310f, timestamp = AHORA + 1)

        assertEquals(Resultado.FechaFutura, resultado)
        assertEquals(1_000L, registros().single().timestamp)
    }

    @Test
    fun unRegistroDadoDeBajaInexistenteOCuyoAnimalEstaDeBajaNoSeEdita() = runTest {
        conAnimal()
        conRegistro(fechaBaja = 5L)
        assertEquals(Resultado.NoDisponible, actualizarRegistro("registro-1", 310f, 2_000L))
        assertEquals(Resultado.NoDisponible, actualizarRegistro("no-existe", 310f, 2_000L))

        conAnimal(fechaBaja = 9L)
        conRegistro()
        assertEquals(Resultado.NoDisponible, actualizarRegistro("registro-1", 310f, 2_000L))
        assertEquals(300f, registros().single().peso)
    }

    private companion object {
        const val AHORA = 10_000L
    }
}
