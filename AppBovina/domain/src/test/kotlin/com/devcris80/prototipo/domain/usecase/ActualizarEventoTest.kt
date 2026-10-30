package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.Evento
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.model.Sexo
import com.devcris80.prototipo.domain.model.TipoEvento
import com.devcris80.prototipo.domain.usecase.ActualizarEvento.Resultado
import com.devcris80.prototipo.testing.FakeAnimalRepository
import com.devcris80.prototipo.testing.FakeEventoRepository
import com.devcris80.prototipo.testing.FakeTransaccion
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActualizarEventoTest {
    private val animalRepository = FakeAnimalRepository()
    private val eventoRepository = FakeEventoRepository()
    private val actualizarEvento = ActualizarEvento(animalRepository, eventoRepository, FakeTransaccion())

    private fun conAnimal(fechaBaja: Long? = null) = animalRepository.set(
        Animal("vaca-1", "perfil_local", "Lola", "Holstein", Sexo.HEMBRA, Etapa.VACA, 1L, proposito = Proposito.LECHE, fechaBaja = fechaBaja),
    )

    private fun conVacunacion(fechaBaja: Long? = null) =
        Evento(
            idEvento = "evento-1",
            idAnimal = "vaca-1",
            idUsuario = "usuario-1",
            tipoEvento = TipoEvento.VACUNACION.nombre,
            fecha = 1_000L,
            detalle = "Vacunación del ciclo I",
            fechaBaja = fechaBaja,
            biologico = "Aftosa",
            dosis = "2 ml",
            via = "Subcutánea",
        ).also { eventoRepository.set("vaca-1", listOf(it)) }

    private suspend fun evento() = eventoRepository.observeByAnimal("vaca-1").first().single()

    @Test
    fun cambiaElTituloLaFechaYLosCamposDeLaVacunacionYElHistorialMuestraLosValoresNuevos() = runTest {
        conAnimal()
        conVacunacion()

        val resultado = actualizarEvento(
            "evento-1",
            detalle = "  Vacunación del ciclo II ",
            fecha = 2_000L,
            biologico = "Brucelosis",
            dosis = "3 ml",
            via = "Intramuscular",
        )

        assertEquals(Resultado.Actualizado, resultado)
        val editado = evento()
        assertEquals("Vacunación del ciclo II", editado.detalle)
        assertEquals(2_000L, editado.fecha)
        assertEquals("Brucelosis", editado.biologico)
        assertEquals("3 ml", editado.dosis)
        assertEquals("Intramuscular", editado.via)
        assertEquals("vaca-1", editado.idAnimal)
        assertEquals("usuario-1", editado.idUsuario)
        assertEquals(TipoEvento.VACUNACION.nombre, editado.tipoEvento)
    }

    @Test
    fun sinTituloOSinBiologicoEnUnaVacunacionNoSeGuardaNada() = runTest {
        conAnimal()
        val original = conVacunacion()

        assertEquals(Resultado.FaltanDatos, actualizarEvento("evento-1", detalle = "   ", fecha = 2_000L, biologico = "Aftosa"))
        assertEquals(Resultado.FaltanDatos, actualizarEvento("evento-1", detalle = "Ciclo II", fecha = 2_000L, biologico = "  "))
        assertEquals(Resultado.FaltanDatos, actualizarEvento("evento-1", detalle = "Ciclo II", fecha = 2_000L, biologico = null))

        assertEquals(original, evento())
    }

    @Test
    fun unEventoInexistenteDadoDeBajaOCuyoAnimalEstaDeBajaNoSeEdita() = runTest {
        conAnimal()
        conVacunacion(fechaBaja = 9L)
        assertEquals(Resultado.NoDisponible, actualizarEvento("evento-1", "Ciclo II", 2_000L, biologico = "Aftosa"))
        assertEquals(Resultado.NoDisponible, actualizarEvento("no-existe", "Ciclo II", 2_000L, biologico = "Aftosa"))

        conAnimal(fechaBaja = 9L)
        val original = conVacunacion()
        assertEquals(Resultado.NoDisponible, actualizarEvento("evento-1", "Ciclo II", 2_000L, biologico = "Aftosa"))
        assertEquals(original, eventoRepository.findById("evento-1"))
    }

    @Test
    fun unPartoSeEditaSinBiologicoYConservaSuCriaSinTocarALaCria() = runTest {
        conAnimal()
        val cria = Animal("cria-1", "perfil_local", "Lucero", "Holstein", Sexo.HEMBRA, Etapa.TERNERA, 1L, proposito = Proposito.LECHE, idMadre = "vaca-1")
        animalRepository.set(cria)
        eventoRepository.set(
            "vaca-1",
            listOf(
                Evento("evento-1", "vaca-1", "usuario-1", TipoEvento.PARTO.nombre, 1_000L, "Parto de Lola", idCriaResultante = "cria-1"),
            ),
        )

        val resultado = actualizarEvento("evento-1", "Parto de Lola (corregido)", 3_000L, biologico = "Aftosa", dosis = "2 ml", via = "Oral")

        assertEquals(Resultado.Actualizado, resultado)
        val editado = evento()
        assertEquals("Parto de Lola (corregido)", editado.detalle)
        assertEquals(3_000L, editado.fecha)
        assertEquals("cria-1", editado.idCriaResultante)
        assertNull(editado.biologico)
        assertNull(editado.dosis)
        assertNull(editado.via)
        assertEquals(cria, animalRepository.findById("cria-1"))
    }

    @Test
    fun unEventoDeOtroTipoConservaSusCamposAlEditarlo() = runTest {
        conAnimal()
        eventoRepository.set(
            "vaca-1",
            listOf(
                Evento(
                    "evento-1", "vaca-1", "usuario-1", "Tratamiento", 1_000L, "Antibiótico",
                    biologico = "Penicilina", dosis = "5 ml", via = "Intramuscular",
                ),
            ),
        )

        val resultado = actualizarEvento("evento-1", "Antibiótico corregido", 2_000L)

        assertEquals(Resultado.Actualizado, resultado)
        val editado = evento()
        assertEquals("Antibiótico corregido", editado.detalle)
        assertEquals("Penicilina", editado.biologico)
        assertEquals("5 ml", editado.dosis)
        assertEquals("Intramuscular", editado.via)
    }
}
