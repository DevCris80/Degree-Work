package com.devcris80.prototipo.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.devcris80.prototipo.data.local.AppDatabase
import com.devcris80.prototipo.data.local.entity.PerfilFincaEntity
import com.devcris80.prototipo.data.local.entity.UsuarioEntity
import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.Evento
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.model.Sexo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomEventoRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repositorio: RoomEventoRepository

    @Before
    fun crearBase() = runTest {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .build()
        db.perfilFincaDao().guardar(PerfilFincaEntity("finca-1", "Finca de prueba", fechaModificacion = 0))
        db.usuarioDao().insert(UsuarioEntity("usuario-1", "finca-1", "Ganadero", fechaModificacion = 0, delDispositivo = true))
        val animales = RoomAnimalRepository(db.animalDao())
        animales.insert(animal("activo", Sexo.HEMBRA, Etapa.VACA))
        animales.insert(animal("de-baja", Sexo.MACHO, Etapa.TORO).copy(fechaBaja = 9L))
        repositorio = RoomEventoRepository(db.eventoDao())
    }

    private fun animal(idAnimal: String, sexo: Sexo, etapa: Etapa) =
        Animal(idAnimal, "finca-1", idAnimal, "Brahman", sexo, etapa, fechaNacimiento = 0, proposito = Proposito.CARNE)

    @After
    fun cerrarBase() = db.close()

    @Test
    fun losEventosActivosDejanFueraLosDadosDeBajaPeroNoLosDeAnimalesDadosDeBaja() = runTest {
        repositorio.insert(Evento("evento-1", "activo", "usuario-1", "Vacunación", fecha = 1L, detalle = ""))
        repositorio.insert(
            Evento("evento-2", "activo", "usuario-1", "Vacunación", fecha = 3L, detalle = "", fechaBaja = 4L),
        )
        repositorio.insert(Evento("evento-3", "de-baja", "usuario-1", "Tratamiento", fecha = 2L, detalle = ""))

        assertEquals(listOf("evento-3", "evento-1"), repositorio.observeActivos().first().map { it.idEvento })
    }

    @Test
    fun losCamposDeCadaTipoSeGuardanYSeLeen() = runTest {
        repositorio.insert(
            Evento(
                "evento-1", "activo", "usuario-1", "Vacunación", fecha = 1L, detalle = "Ciclo I",
                biologico = "Aftosa", dosis = "2 ml", via = "Subcutánea",
            ),
        )
        repositorio.insert(
            Evento("evento-2", "activo", "usuario-1", "Parto", fecha = 2L, detalle = "Parto", idCriaResultante = "cria-1"),
        )

        val eventos = repositorio.observeByAnimal("activo").first().associateBy { it.idEvento }

        assertEquals("Aftosa", eventos.getValue("evento-1").biologico)
        assertEquals("2 ml", eventos.getValue("evento-1").dosis)
        assertEquals("Subcutánea", eventos.getValue("evento-1").via)
        assertEquals("cria-1", eventos.getValue("evento-2").idCriaResultante)
    }

    @Test
    fun editarUnEventoSincronizadoCambiaSusCamposLoDejaPendienteYConservaAnimalAutorTipoYCria() = runTest {
        db.eventoDao().insert(
            Evento(
                "evento-1", "activo", "usuario-1", "Parto", fecha = 1L, detalle = "Parto", idCriaResultante = "cria-1",
                biologico = "Aftosa",
            ).toEntity(fechaModificacion = 0, idVersion = "del-servidor").copy(rev = 3, sincronizado = true),
        )
        val repositorio = RoomEventoRepository(db.eventoDao(), clock = { 100 }, nuevoIdVersion = { "version-1" })

        repositorio.update(
            Evento(
                "evento-1", "de-baja", "otro", "Vacunación", fecha = 2L, detalle = "Parto corregido", idCriaResultante = "otra-cria",
                biologico = "Brucelosis", dosis = "3 ml", via = "Oral",
            ),
        )

        val editado = db.eventoDao().findById("evento-1")!!
        assertEquals("Parto corregido", editado.detalle)
        assertEquals(2L, editado.fecha)
        assertEquals("Brucelosis", editado.biologico)
        assertEquals("3 ml", editado.dosis)
        assertEquals("Oral", editado.via)
        assertEquals("activo", editado.idAnimal)
        assertEquals("usuario-1", editado.idUsuario)
        assertEquals("Parto", editado.tipoEvento)
        assertEquals("cria-1", editado.idCriaResultante)
        assertEquals(false, editado.sincronizado)
        assertEquals(3, editado.revBase)
        assertEquals("version-1", editado.idVersion)
        assertEquals(100L, editado.fechaModificacion)
    }

    @Test
    fun unEventoDadoDeBajaSaleDeLasListasPeroSigueEnLaBaseYPendienteDeSubir() = runTest {
        db.eventoDao().insert(
            Evento("evento-1", "activo", "usuario-1", "Vacunación", fecha = 1L, detalle = "Ciclo I")
                .toEntity(fechaModificacion = 0, idVersion = "del-servidor").copy(rev = 2, sincronizado = true),
        )
        val repositorio = RoomEventoRepository(db.eventoDao(), clock = { 100 }, nuevoIdVersion = { "version-1" })

        repositorio.darDeBaja("evento-1", fecha = 50)

        assertEquals(emptyList<Evento>(), repositorio.observeByAnimal("activo").first())
        assertEquals(emptyList<Evento>(), repositorio.observeActivos().first())
        val guardado = db.eventoDao().findById("evento-1")!!
        assertEquals(50L, guardado.fechaBaja)
        assertEquals(false, guardado.sincronizado)
        assertEquals(2, guardado.revBase)
        assertEquals("version-1", guardado.idVersion)
        assertEquals("Ciclo I", repositorio.findById("evento-1")!!.detalle)
    }
}
