package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Chapeta
import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.model.Sexo
import com.devcris80.prototipo.testing.FakeAnimalRepository
import com.devcris80.prototipo.testing.FakeChapetaRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ResolverDestinoEscaneoTest {
    private val chapetaRepository = FakeChapetaRepository()
    private val animalRepository = FakeAnimalRepository()
    private val resolverDestinoEscaneo = ResolverDestinoEscaneo(chapetaRepository, animalRepository)

    private fun animal(id: String, nombre: String, fechaBaja: Long? = null) = Animal(
        id, "perfil_local", nombre, "Holstein", Sexo.HEMBRA, Etapa.VACA, 1L,
        proposito = Proposito.LECHE, fechaBaja = fechaBaja,
    ).also(animalRepository::set)

    private fun chapeta(id: String, codigo: String, idAnimal: String, fechaDesasociacion: Long? = null, fechaAsociacion: Long = 1L) =
        Chapeta(id, codigo, idAnimal, fechaAsociacion, fechaDesasociacion).also(chapetaRepository::set)

    @Test
    fun unaChapetaActivaDeUnAnimalActivoAbreElAnimal() = runTest {
        animal("a1", "Manchas")
        chapeta("c1", "04A1B2C3", "a1")

        assertEquals(DestinoEscaneo.AbrirAnimal("a1"), resolverDestinoEscaneo("04:a1-b2 c3"))
    }

    @Test
    fun unaChapetaActivaDeUnAnimalDadoDeBajaPideLiberarla() = runTest {
        animal("a1", "Canela", fechaBaja = 5L)
        chapeta("c1", "04A1B2C3", "a1")

        assertEquals(DestinoEscaneo.AnimalDadoDeBaja("c1", "Canela", "04A1B2C3"), resolverDestinoEscaneo("04A1B2C3"))
    }

    @Test
    fun unCodigoLiberadoRegistraAvisandoDelUltimoAnimalQueLoTuvo() = runTest {
        animal("viejo", "Canela")
        animal("nuevo", "Manchas")
        chapeta("c1", "04A1B2C3", "viejo", fechaDesasociacion = 3L, fechaAsociacion = 1L)
        chapeta("c2", "04A1B2C3", "nuevo", fechaDesasociacion = 9L, fechaAsociacion = 5L)

        assertEquals(DestinoEscaneo.RegistrarConAviso("04A1B2C3", "Manchas"), resolverDestinoEscaneo("04A1B2C3"))
    }

    @Test
    fun unCodigoNuncaVistoRegistraUnAnimalNuevo() = runTest {
        assertEquals(DestinoEscaneo.RegistrarNuevo("FFEEDD00"), resolverDestinoEscaneo("ff:ee:dd:00"))
    }
}
