package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.model.Rol
import com.devcris80.prototipo.domain.model.Sexo
import com.devcris80.prototipo.domain.model.TipoEvento
import com.devcris80.prototipo.domain.model.Usuario
import com.devcris80.prototipo.testing.FakeAnimalRepository
import com.devcris80.prototipo.testing.FakeCuentaRepository
import com.devcris80.prototipo.testing.FakeEventoRepository
import com.devcris80.prototipo.testing.FakeTransaccion
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RegistrarEventoTest {
    private val animalRepository = FakeAnimalRepository()
    private val eventoRepository = FakeEventoRepository()
    private val cuentaRepository = FakeCuentaRepository()
    private val registrarEvento =
        RegistrarEvento(animalRepository, eventoRepository, cuentaRepository, FakeTransaccion())

    private fun vaca() = Animal(
        idAnimal = "vaca-1",
        idPerfilFinca = "perfil_local",
        nombre = "Lola",
        raza = "Holstein",
        sexo = Sexo.HEMBRA,
        etapa = Etapa.VACA,
        fechaNacimiento = 1L,
        proposito = Proposito.LECHE,
    )

    private fun toro() = vaca().copy(
        idAnimal = "toro-1",
        nombre = "Capitán",
        sexo = Sexo.MACHO,
        etapa = Etapa.TORO,
    )

    private fun cria() = vaca().copy(idAnimal = "cria-1", nombre = "Lucero", etapa = Etapa.TERNERA)

    private suspend fun eventosDe(idAnimal: String) = eventoRepository.observeByAnimal(idAnimal).first()

    private fun hayUsuario() {
        cuentaRepository.usuario.value = Usuario("usuario-1", "perfil_local", "Ana", Rol.OPERARIO)
    }

    @Test
    fun registraUnaVacunacionFuturaANombreDelUsuarioConSusCampos() = runTest {
        hayUsuario()
        animalRepository.set(vaca())

        val resultado = registrarEvento(
            idAnimal = "vaca-1",
            tipo = TipoEvento.VACUNACION,
            detalle = "Vacunación del ciclo I",
            fecha = 5_000L,
            biologico = "Aftosa",
            dosis = "2 ml",
            via = "Subcutánea",
        )

        val evento = eventosDe("vaca-1").single()
        assertEquals(RegistrarEvento.Resultado.Registrado(evento.idEvento), resultado)
        assertEquals("usuario-1", evento.idUsuario)
        assertEquals("Vacunación", evento.tipoEvento)
        assertEquals("Vacunación del ciclo I", evento.detalle)
        assertEquals(5_000L, evento.fecha)
        assertEquals("Aftosa", evento.biologico)
        assertEquals("2 ml", evento.dosis)
        assertEquals("Subcutánea", evento.via)
        assertNull(evento.idCriaResultante)
    }

    @Test
    fun sinUsuarioEnElDispositivoNoRegistraNada() = runTest {
        animalRepository.set(vaca())

        val resultado = registrarEvento("vaca-1", TipoEvento.VACUNACION, "Vacuna", fecha = 1L)

        assertEquals(RegistrarEvento.Resultado.SinUsuario, resultado)
        assertTrue(eventosDe("vaca-1").isEmpty())
    }

    @Test
    fun unAnimalQueNoExisteONoEstaActivoNoRecibeEventos() = runTest {
        hayUsuario()
        animalRepository.set(vaca().copy(idAnimal = "vaca-baja", fechaBaja = 9L))

        val inexistente = registrarEvento("no-existe", TipoEvento.VACUNACION, "Vacuna", fecha = 1L)
        val dadoDeBaja = registrarEvento("vaca-baja", TipoEvento.VACUNACION, "Vacuna", fecha = 1L)

        assertEquals(RegistrarEvento.Resultado.AnimalNoDisponible, inexistente)
        assertEquals(RegistrarEvento.Resultado.AnimalNoDisponible, dadoDeBaja)
        assertTrue(eventosDe("no-existe").isEmpty())
        assertTrue(eventosDe("vaca-baja").isEmpty())
    }

    @Test
    fun unPartoDeUnaHembraDejaALaCriaConEsaMadreYGuardaSoloLoDelParto() = runTest {
        hayUsuario()
        animalRepository.set(vaca())
        animalRepository.set(cria())

        val resultado = registrarEvento(
            idAnimal = "vaca-1",
            tipo = TipoEvento.PARTO,
            detalle = "Parto sin complicaciones",
            fecha = 1L,
            biologico = "Aftosa",
            idCriaResultante = "cria-1",
        )

        val evento = eventosDe("vaca-1").single()
        assertEquals(RegistrarEvento.Resultado.Registrado(evento.idEvento), resultado)
        assertEquals("Parto", evento.tipoEvento)
        assertEquals("cria-1", evento.idCriaResultante)
        assertNull(evento.biologico)
        assertEquals(listOf(cria().copy(idMadre = "vaca-1")), animalRepository.actualizados)
    }

    @Test
    fun unPartoDeUnMachoDejaALaCriaConEsePadre() = runTest {
        hayUsuario()
        animalRepository.set(toro())
        animalRepository.set(cria())

        registrarEvento("toro-1", TipoEvento.PARTO, "Monta confirmada", fecha = 1L, idCriaResultante = "cria-1")

        assertEquals(listOf(cria().copy(idPadre = "toro-1")), animalRepository.actualizados)
    }

    @Test
    fun unPartoSinCriaNoEditaNingunAnimal() = runTest {
        hayUsuario()
        animalRepository.set(vaca())

        registrarEvento("vaca-1", TipoEvento.PARTO, "Parto", fecha = 1L)

        assertNull(eventosDe("vaca-1").single().idCriaResultante)
        assertTrue(animalRepository.actualizados.isEmpty())
    }

    @Test
    fun unaCriaQueNoExisteONoEsOtroAnimalNoRegistraElParto() = runTest {
        hayUsuario()
        animalRepository.set(vaca())

        val inexistente = registrarEvento("vaca-1", TipoEvento.PARTO, "Parto", fecha = 1L, idCriaResultante = "no-existe")
        val ellaMisma = registrarEvento("vaca-1", TipoEvento.PARTO, "Parto", fecha = 1L, idCriaResultante = "vaca-1")

        assertEquals(RegistrarEvento.Resultado.CriaNoValida, inexistente)
        assertEquals(RegistrarEvento.Resultado.CriaNoValida, ellaMisma)
        assertTrue(eventosDe("vaca-1").isEmpty())
        assertTrue(animalRepository.actualizados.isEmpty())
    }

    @Test
    fun unaVacunacionIgnoraLaCriaYLaCriaYaEnlazadaNoSeEscribeDeNuevo() = runTest {
        hayUsuario()
        animalRepository.set(vaca())
        animalRepository.set(cria().copy(idMadre = "vaca-1"))

        registrarEvento("vaca-1", TipoEvento.VACUNACION, "Vacuna", fecha = 1L, idCriaResultante = "cria-1")
        registrarEvento("vaca-1", TipoEvento.PARTO, "Parto", fecha = 2L, idCriaResultante = "cria-1")

        assertTrue(eventosDe("vaca-1").first { it.tipoEvento == "Vacunación" }.idCriaResultante == null)
        assertTrue(animalRepository.actualizados.isEmpty())
    }
}
