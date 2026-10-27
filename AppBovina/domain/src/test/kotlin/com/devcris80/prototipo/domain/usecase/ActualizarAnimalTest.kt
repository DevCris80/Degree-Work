package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.model.Sexo
import com.devcris80.prototipo.testing.FakeAnimalRepository
import com.devcris80.prototipo.testing.FakeTransaccion
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActualizarAnimalTest {
    private val animalRepository = FakeAnimalRepository()
    private val actualizarAnimal = ActualizarAnimal(animalRepository, FakeTransaccion())

    private fun toro() = Animal(
        idAnimal = "toro-1",
        idPerfilFinca = "perfil_local",
        nombre = "Capitán",
        raza = "Brahman",
        sexo = Sexo.MACHO,
        etapa = Etapa.TORO,
        fechaNacimiento = 1L,
        proposito = Proposito.CARNE,
    )

    private fun cria() = Animal(
        idAnimal = "cria-1",
        idPerfilFinca = "perfil_local",
        nombre = "Lucero",
        raza = "Brahman",
        sexo = Sexo.MACHO,
        etapa = Etapa.NOVILLO,
        fechaNacimiento = 2L,
        proposito = Proposito.CARNE,
        idPadre = "toro-1",
    )

    @Test
    fun unPadreConCriasNoCambiaDeSexo() = runTest {
        animalRepository.set(toro())
        animalRepository.set(cria())

        val resultado = actualizarAnimal(toro().copy(sexo = Sexo.HEMBRA, etapa = Etapa.VACA))

        assertEquals(ActualizarAnimal.Resultado.SexoConCrias, resultado)
        assertTrue(animalRepository.actualizados.isEmpty())
    }

    @Test
    fun unPadreConCriasSigueEditandoSusOtrosDatos() = runTest {
        animalRepository.set(toro())
        animalRepository.set(cria())

        val resultado = actualizarAnimal(toro().copy(raza = "Cebú"))

        assertEquals(ActualizarAnimal.Resultado.Actualizado, resultado)
        assertEquals(listOf(toro().copy(raza = "Cebú")), animalRepository.actualizados)
    }

    @Test
    fun unAnimalSinCriasCambiaDeSexoConNormalidad() = runTest {
        animalRepository.set(toro())

        val resultado = actualizarAnimal(toro().copy(sexo = Sexo.HEMBRA, etapa = Etapa.VACA))

        assertEquals(ActualizarAnimal.Resultado.Actualizado, resultado)
        assertEquals(Sexo.HEMBRA, animalRepository.actualizados.single().sexo)
    }

    @Test
    fun unaFotoQueBajoMientrasSeEditabaSeConservaSiLaPersonaNoCambioLaFoto() = runTest {
        // El formulario se abrió con la foto A; mientras tanto, la sincronización bajó la B de otro celular.
        animalRepository.set(toro().copy(fotoUri = "content://foto-b"))

        val resultado = actualizarAnimal(toro().copy(fotoUri = "content://foto-a", raza = "Cebú"), fotoCambiada = false)

        assertEquals(ActualizarAnimal.Resultado.Actualizado, resultado)
        assertEquals(listOf(toro().copy(fotoUri = "content://foto-b", raza = "Cebú")), animalRepository.actualizados)
    }

    @Test
    fun unaFotoNuevaDelFormularioReemplazaLaGuardada() = runTest {
        animalRepository.set(toro().copy(fotoUri = "content://foto-b"))

        actualizarAnimal(toro().copy(fotoUri = "content://foto-nueva"), fotoCambiada = true)

        assertEquals(listOf(toro().copy(fotoUri = "content://foto-nueva")), animalRepository.actualizados)
    }
}
