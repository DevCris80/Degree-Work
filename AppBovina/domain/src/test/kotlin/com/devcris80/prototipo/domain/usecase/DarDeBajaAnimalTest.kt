package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.model.Rol
import com.devcris80.prototipo.domain.model.Sexo
import com.devcris80.prototipo.domain.model.Usuario
import com.devcris80.prototipo.testing.FakeAnimalRepository
import com.devcris80.prototipo.testing.FakeCuentaRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DarDeBajaAnimalTest {
    private val animalRepository = FakeAnimalRepository()
    private val cuentaRepository = FakeCuentaRepository()
    private val darDeBajaAnimal = DarDeBajaAnimal(animalRepository, cuentaRepository, clock = { 5L })

    private fun conUsuario(rol: Rol) {
        cuentaRepository.usuario.value = Usuario("usuario-1", "perfil_local", "Ana", rol)
        animalRepository.set(
            Animal(
                idAnimal = "animal-1",
                idPerfilFinca = "perfil_local",
                nombre = "Manchas",
                raza = "Holstein",
                sexo = Sexo.HEMBRA,
                etapa = Etapa.VACA,
                fechaNacimiento = 1L,
                proposito = Proposito.LECHE,
            ),
        )
    }

    @Test
    fun elGanaderoDaDeBajaUnAnimal() = runTest {
        conUsuario(Rol.GANADERO)

        val resultado = darDeBajaAnimal("animal-1")

        assertEquals(DarDeBajaAnimal.Resultado.Realizado, resultado)
        assertEquals(listOf("animal-1" to 5L), animalRepository.bajas)
    }

    @Test
    fun elOperarioNoDaDeBajaUnAnimalAunqueSeLlameAlCasoDeUso() = runTest {
        conUsuario(Rol.OPERARIO)

        val resultado = darDeBajaAnimal("animal-1")

        assertEquals(DarDeBajaAnimal.Resultado.NoPermitido, resultado)
        assertTrue(animalRepository.bajas.isEmpty())
    }

    @Test
    fun sinUsuarioEnElDispositivoNoSeDaDeBaja() = runTest {
        animalRepository.set(
            Animal(
                idAnimal = "animal-1",
                idPerfilFinca = "perfil_local",
                nombre = "Manchas",
                raza = "Holstein",
                sexo = Sexo.HEMBRA,
                etapa = Etapa.VACA,
                fechaNacimiento = 1L,
                proposito = Proposito.LECHE,
            ),
        )

        assertEquals(DarDeBajaAnimal.Resultado.NoPermitido, darDeBajaAnimal("animal-1"))
        assertTrue(animalRepository.bajas.isEmpty())
    }
}
