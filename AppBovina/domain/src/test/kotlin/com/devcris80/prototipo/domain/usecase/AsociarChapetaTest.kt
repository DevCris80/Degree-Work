package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Chapeta
import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.MotivoPesajePendiente
import com.devcris80.prototipo.domain.model.PesajePendiente
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.model.Sexo
import com.devcris80.prototipo.domain.usecase.AsociarChapeta.Resultado
import com.devcris80.prototipo.testing.FakeAnimalRepository
import com.devcris80.prototipo.testing.FakeChapetaRepository
import com.devcris80.prototipo.testing.FakePesajePendienteRepository
import com.devcris80.prototipo.testing.FakeTransaccion
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AsociarChapetaTest {
    private val animalRepository = FakeAnimalRepository()
    private val chapetaRepository = FakeChapetaRepository()
    private val pesajePendienteRepository = FakePesajePendienteRepository()
    private val asociarChapeta = AsociarChapeta(
        animalRepository,
        chapetaRepository,
        pesajePendienteRepository,
        FakeTransaccion(),
        clock = { 9L },
    )

    private fun animal(id: String = "animal-1", activo: Boolean = true) = Animal(
        idAnimal = id,
        idPerfilFinca = "perfil_local",
        nombre = "Animal $id",
        raza = "Holstein",
        sexo = Sexo.HEMBRA,
        etapa = Etapa.VACA,
        fechaNacimiento = 1L,
        proposito = Proposito.LECHE,
        fechaBaja = if (activo) null else 3L,
    ).also { animalRepository.set(it) }

    private fun chapeta(id: String, codigo: String, idAnimal: String, fechaDesasociacion: Long? = null) =
        Chapeta(id, codigo, idAnimal, fechaAsociacion = 1L, fechaDesasociacion = fechaDesasociacion)
            .also(chapetaRepository::set)

    private fun pendiente(id: String, codigo: String) = PesajePendiente(
        id, "lectura-$id", codigo, 100f, 1L, "usuario-1", MotivoPesajePendiente.CODIGO_SIN_CHAPETA_ACTIVA,
    ).also(pesajePendienteRepository::set)

    @Test
    fun asociaElCodigoNormalizadoComoChapetaActivaDelAnimal() = runTest {
        animal()

        val resultado = asociarChapeta("animal-1", " 04:a1-b2 c3 ")

        assertEquals(Resultado.Asociada(emptyList()), resultado)
        val chapeta = chapetaRepository.observeActivaByAnimal("animal-1").first()!!
        assertEquals("04A1B2C3", chapeta.codigo)
        assertEquals(9L, chapeta.fechaAsociacion)
        assertNull(chapeta.fechaDesasociacion)
    }

    @Test
    fun aUnAnimalConLaChapetaLiberadaLeAsociaUnCodigoNuevoYConservaElHistorial() = runTest {
        animal()
        chapeta("vieja", "AAAA", "animal-1", fechaDesasociacion = 5L)

        val resultado = asociarChapeta("animal-1", "BBBB")

        assertEquals(Resultado.Asociada(emptyList()), resultado)
        assertEquals("BBBB", chapetaRepository.findActivaByAnimal("animal-1")!!.codigo)
        assertEquals(5L, chapetaRepository.chapeta("vieja")!!.fechaDesasociacion)
    }

    @Test
    fun unCodigoConChapetaActivaEnOtroAnimalNoSeAsocia() = runTest {
        animal("animal-1")
        animal("animal-2")
        chapeta("c-2", "AAAA", "animal-2")

        val resultado = asociarChapeta("animal-1", "aa-aa")

        assertEquals(Resultado.CodigoYaActivo("AAAA"), resultado)
        assertNull(chapetaRepository.findActivaByAnimal("animal-1"))
    }

    @Test
    fun unAnimalConChapetaActivaNoRecibeOtra() = runTest {
        animal()
        chapeta("c-1", "AAAA", "animal-1")

        val resultado = asociarChapeta("animal-1", "BBBB")

        assertEquals(Resultado.AnimalYaTieneChapeta, resultado)
        assertNull(chapetaRepository.findActivaByCodigo("BBBB"))
    }

    @Test
    fun unAnimalDadoDeBajaOInexistenteNoRecibeChapeta() = runTest {
        animal("baja", activo = false)

        assertEquals(Resultado.AnimalNoDisponible, asociarChapeta("baja", "AAAA"))
        assertEquals(Resultado.AnimalNoDisponible, asociarChapeta("no-existe", "AAAA"))
        assertNull(chapetaRepository.findActivaByCodigo("AAAA"))
    }

    @Test
    fun unCodigoVacioNoSeAsocia() = runTest {
        animal()

        assertEquals(Resultado.CodigoVacio, asociarChapeta("animal-1", " : - "))
        assertNull(chapetaRepository.findActivaByAnimal("animal-1"))
    }

    @Test
    fun devuelveLosPesajesPendientesSinResolverDelCodigoSinAsignarlos() = runTest {
        animal()
        val uno = pendiente("p-1", "AAAA")
        val dos = pendiente("p-2", "AAAA")
        pendiente("p-3", "OTRO")

        val resultado = asociarChapeta("animal-1", "aaaa")

        assertEquals(Resultado.Asociada(listOf(uno, dos)), resultado)
        assertEquals(listOf(uno, dos), pesajePendienteRepository.todos.value.filter { it.codigo == "AAAA" })
    }
}
