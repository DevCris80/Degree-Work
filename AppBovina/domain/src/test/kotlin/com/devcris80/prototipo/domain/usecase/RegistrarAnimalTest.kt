package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.MotivoPesajePendiente
import com.devcris80.prototipo.domain.model.PesajePendiente
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.model.Sexo
import com.devcris80.prototipo.domain.usecase.RegistrarAnimal.Resultado
import com.devcris80.prototipo.testing.FakeAnimalRepository
import com.devcris80.prototipo.testing.FakeChapetaRepository
import com.devcris80.prototipo.testing.FakePesajePendienteRepository
import com.devcris80.prototipo.testing.FakeTransaccion
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RegistrarAnimalTest {
    private val animalRepository = FakeAnimalRepository()
    private val chapetaRepository = FakeChapetaRepository()
    private val pesajePendienteRepository = FakePesajePendienteRepository()
    private val registrarAnimal = RegistrarAnimal(
        animalRepository,
        chapetaRepository,
        pesajePendienteRepository,
        FakeTransaccion(),
        clock = { 5L },
    )

    private fun animal(id: String = "animal-1", sexo: Sexo = Sexo.HEMBRA, idPadre: String? = null, idMadre: String? = null) =
        Animal(
            idAnimal = id,
            idPerfilFinca = "perfil_local",
            nombre = "Animal $id",
            raza = "Holstein",
            sexo = sexo,
            etapa = if (sexo == Sexo.HEMBRA) Etapa.VACA else Etapa.TORO,
            fechaNacimiento = 1L,
            proposito = Proposito.LECHE,
            idPadre = idPadre,
            idMadre = idMadre,
        )

    private fun pendiente(id: String, codigo: String) = PesajePendiente(
        id, "lectura-$id", codigo, 100f, 1L, "usuario-1", MotivoPesajePendiente.CODIGO_SIN_CHAPETA_ACTIVA,
    ).also(pesajePendienteRepository::set)

    @Test
    fun guardaElAnimalYSuChapetaActivaConElCodigoNormalizado() = runTest {
        val resultado = registrarAnimal(animal(), " 04:a1-b2 c3 ")

        assertEquals(Resultado.Exito("animal-1", emptyList()), resultado)
        assertEquals(listOf(animal()), animalRepository.insertados)
        val chapeta = chapetaRepository.observeActivaByAnimal("animal-1").first()!!
        assertEquals("04A1B2C3", chapeta.codigo)
        assertEquals(5L, chapeta.fechaAsociacion)
    }

    @Test
    fun sinCodigoGuardaSoloElAnimal() = runTest {
        assertEquals(Resultado.Exito("animal-1", emptyList()), registrarAnimal(animal(), null))
        assertEquals(Resultado.Exito("animal-2", emptyList()), registrarAnimal(animal("animal-2"), "   "))

        assertEquals(2, animalRepository.insertados.size)
        assertTrue(chapetaRepository.observeActivas().first().isEmpty())
    }

    @Test
    fun unCodigoConOtraChapetaActivaNoGuardaNada() = runTest {
        registrarAnimal(animal("animal-1"), "AAAA")

        val resultado = registrarAnimal(animal("animal-2"), "aa-aa")

        assertEquals(Resultado.CodigoYaActivo("AAAA"), resultado)
        assertEquals(listOf("animal-1"), animalRepository.insertados.map { it.idAnimal })
    }

    @Test
    fun devuelveLosPesajesPendientesDeEseCodigoSinAsignarlos() = runTest {
        val delCodigo = pendiente("p1", "AAAA")
        pendiente("p2", "OTRO")

        val resultado = registrarAnimal(animal(), "aaaa") as Resultado.Exito

        assertEquals(listOf(delCodigo), resultado.pesajesPendientes)
        assertTrue("asignarlos lo decide una persona", pesajePendienteRepository.todos.value.all { it.conciliacion == null })
    }

    @Test
    fun elPadreDebeSerMachoYLaMadreHembraYSiNoNoSeGuardaNada() = runTest {
        animalRepository.set(animal("macho", Sexo.MACHO))
        animalRepository.set(animal("hembra", Sexo.HEMBRA))

        assertEquals(Resultado.Exito("cria-1", emptyList()), registrarAnimal(animal("cria-1", idPadre = "macho", idMadre = "hembra"), null))
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { registrarAnimal(animal("cria-2", idPadre = "hembra"), "BBBB") }
        }
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { registrarAnimal(animal("cria-3", idMadre = "no-existe"), "CCCC") }
        }

        assertEquals(listOf("cria-1"), animalRepository.insertados.map { it.idAnimal })
        assertTrue(chapetaRepository.observeActivas().first().isEmpty())
    }
}
