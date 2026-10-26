package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Conciliacion
import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.MotivoPesajePendiente
import com.devcris80.prototipo.domain.model.PesajePendiente
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.model.ResolucionPesajePendiente
import com.devcris80.prototipo.domain.model.Rol
import com.devcris80.prototipo.domain.model.Sexo
import com.devcris80.prototipo.domain.model.Usuario
import com.devcris80.prototipo.domain.usecase.AsignarPesajesPendientes.Resultado
import com.devcris80.prototipo.testing.FakeAnimalRepository
import com.devcris80.prototipo.testing.FakeCuentaRepository
import com.devcris80.prototipo.testing.FakePesajePendienteRepository
import com.devcris80.prototipo.testing.FakeRegistroRepository
import com.devcris80.prototipo.testing.FakeTransaccion
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AsignarPesajesPendientesTest {
    private val pesajePendienteRepository = FakePesajePendienteRepository()
    private val animalRepository = FakeAnimalRepository()
    private val registroRepository = FakeRegistroRepository()
    private val cuentaRepository = FakeCuentaRepository()
    private val asignarPesajesPendientes = AsignarPesajesPendientes(
        pesajePendienteRepository,
        animalRepository,
        registroRepository,
        cuentaRepository,
        FakeTransaccion(),
        clock = { 9L },
    )

    private fun conUsuarioYAnimal(fechaBaja: Long? = null) {
        cuentaRepository.usuario.value = Usuario("usuario-conciliador", "perfil_local", "Beto", Rol.OPERARIO)
        animalRepository.set(
            Animal("animal-1", "perfil_local", "Manchas", "Holstein", Sexo.HEMBRA, Etapa.VACA, 1L, proposito = Proposito.LECHE, fechaBaja = fechaBaja),
        )
    }

    private fun pendiente(id: String, peso: Float = 100f, timestamp: Long = 7L) = PesajePendiente(
        idPesajePendiente = id,
        idLectura = "lectura-$id",
        codigo = "FFEEDD00",
        peso = peso,
        timestamp = timestamp,
        idUsuario = "usuario-receptor",
        motivo = MotivoPesajePendiente.CODIGO_SIN_CHAPETA_ACTIVA,
    ).also(pesajePendienteRepository::set)

    private suspend fun registros() = registroRepository.observeByAnimal("animal-1").first()

    @Test
    fun cadaPendienteCreaSuRegistroConSuIdLecturaSuHoraYSuUsuarioYQuedaConciliadoComoAsignado() = runTest {
        conUsuarioYAnimal()
        pendiente("p1", peso = 100f, timestamp = 7L)
        pendiente("p2", peso = 200f, timestamp = 8L)

        val resultado = asignarPesajesPendientes(listOf("p1", "p2"), "animal-1")

        assertEquals(Resultado.Asignados, resultado)
        val registros = registros().sortedBy { it.timestamp }
        assertEquals(listOf("lectura-p1", "lectura-p2"), registros.map { it.idLectura })
        assertEquals(listOf(100f, 200f), registros.map { it.peso })
        assertEquals(listOf(7L, 8L), registros.map { it.timestamp })
        assertTrue("el Registro conserva al Usuario que recibió el Pesaje", registros.all { it.idUsuario == "usuario-receptor" })
        val conciliacion = Conciliacion(ResolucionPesajePendiente.ASIGNADO, "usuario-conciliador", 9L)
        assertEquals(listOf(conciliacion, conciliacion), pesajePendienteRepository.todos.value.map { it.conciliacion })
    }

    @Test
    fun siElAnimalEstaDadoDeBajaNoSeGuardaNingunoYLosPendientesSiguenSinResolver() = runTest {
        conUsuarioYAnimal(fechaBaja = 5L)
        pendiente("p1")

        assertEquals(Resultado.AnimalNoDisponible, asignarPesajesPendientes(listOf("p1"), "animal-1"))
        assertEquals(Resultado.AnimalNoDisponible, asignarPesajesPendientes(listOf("p1"), "no-existe"))

        assertTrue(registros().isEmpty())
        assertNull(pesajePendienteRepository.todos.value.single().conciliacion)
    }

    @Test
    fun siAlgunPendienteYaEstaResueltoNoSeGuardaNingunoDelLote() = runTest {
        conUsuarioYAnimal()
        pendiente("p1")
        pendiente("p2").also {
            pesajePendienteRepository.conciliar("p2", Conciliacion(ResolucionPesajePendiente.DESCARTADO, "otro", 1L))
        }

        assertEquals(Resultado.PendienteNoDisponible, asignarPesajesPendientes(listOf("p1", "p2"), "animal-1"))
        assertEquals(Resultado.PendienteNoDisponible, asignarPesajesPendientes(listOf("p1", "no-existe"), "animal-1"))

        assertTrue(registros().isEmpty())
        assertNull(pesajePendienteRepository.todos.value.first { it.idPesajePendiente == "p1" }.conciliacion)
    }

    @Test
    fun unIdRepetidoSeAsignaUnaSolaVez() = runTest {
        conUsuarioYAnimal()
        pendiente("p1")

        assertEquals(Resultado.Asignados, asignarPesajesPendientes(listOf("p1", "p1"), "animal-1"))

        assertEquals(1, registros().size)
    }
}
