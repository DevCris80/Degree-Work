package com.devcris80.prototipo.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.devcris80.prototipo.data.local.AppDatabase
import com.devcris80.prototipo.data.local.entity.AnimalEntity
import com.devcris80.prototipo.data.local.entity.PerfilFincaEntity
import com.devcris80.prototipo.data.local.entity.SiblingEntity
import com.devcris80.prototipo.domain.model.EntidadConRevisiones
import com.devcris80.prototipo.domain.model.FilaEnConflicto
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomConflictoRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repositorio: RoomConflictoRepository

    @Before
    fun crearBase() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .build()
        repositorio = RoomConflictoRepository(db)
    }

    @After
    fun cerrarBase() = db.close()

    @Test
    fun unaFilaConUnSiblingSinResolverEstaEnConflicto() = runTest {
        db.siblingDao().insert(sibling(idVersion = "version-b", idFila = "animal-1"))

        assertTrue(repositorio.observeEnConflicto(EntidadConRevisiones.ANIMAL, "animal-1").first())
        assertEquals(
            listOf("version-b"),
            repositorio.observeSiblings(EntidadConRevisiones.ANIMAL, "animal-1").first().map { it.idVersion },
        )
    }

    @Test
    fun unaFilaSinSiblingsNoEstaEnConflicto() = runTest {
        db.siblingDao().insert(sibling(idVersion = "version-b", idFila = "animal-1"))

        assertFalse(repositorio.observeEnConflicto(EntidadConRevisiones.ANIMAL, "animal-2").first())
        // El mismo id en otra entidad es otra fila.
        assertFalse(repositorio.observeEnConflicto(EntidadConRevisiones.EVENTO, "animal-1").first())
    }

    @Test
    fun unSiblingQueLlegaDosVecesSeGuardaUnaSola() = runTest {
        db.siblingDao().insert(sibling(idVersion = "version-b", idFila = "animal-1"))
        db.siblingDao().insert(sibling(idVersion = "version-b", idFila = "animal-1"))

        assertEquals(1, repositorio.observeSiblings(EntidadConRevisiones.ANIMAL, "animal-1").first().size)
    }

    private suspend fun animalConVigente(raza: String = "Brahman") {
        db.perfilFincaDao().guardar(PerfilFincaEntity("finca-1", "Finca", sincronizado = true, fechaModificacion = 0))
        db.animalDao().insert(animal(raza).copy(sincronizado = true, rev = 2))
    }

    private fun animal(raza: String) = AnimalEntity(
        idAnimal = "animal-1",
        idPerfilFinca = "finca-1",
        nombre = "Lucero",
        raza = raza,
        sexo = "Hembra",
        etapa = "Vaca",
        fechaNacimiento = 0,
        proposito = "Leche",
        fechaModificacion = 0,
        idVersion = "version-vigente",
    )

    private fun siblingConRaza(idVersion: String, raza: String) =
        sibling(idVersion, "animal-1").copy(contenido = animal(raza).aFilaDelServidor().toString())

    @Test
    fun elConflictoTraeLaVigenteYCadaSiblingComoFilaCompleta() = runTest {
        animalConVigente(raza = "Brahman")
        db.siblingDao().insert(siblingConRaza("version-b", raza = "Gyr"))

        val conflicto = repositorio.observeConflicto(EntidadConRevisiones.ANIMAL, "animal-1").first()!!

        assertEquals("Brahman", (conflicto.vigente as FilaEnConflicto.DeAnimal).animal.raza)
        val sibling = conflicto.siblings.single()
        assertEquals("version-b", sibling.sibling.idVersion)
        assertEquals("Gyr", (sibling.fila as FilaEnConflicto.DeAnimal).animal.raza)
    }

    @Test
    fun unaFilaSinSiblingsNoTieneConflicto() = runTest {
        animalConVigente()

        assertNull(repositorio.observeConflicto(EntidadConRevisiones.ANIMAL, "animal-1").first())
    }

    @Test
    fun elegirUnSiblingEscribeSuFilaComoEdicionSobreLaVigenteYQuitaLaMarca() = runTest {
        animalConVigente(raza = "Brahman")
        db.siblingDao().insert(siblingConRaza("version-b", raza = "Gyr"))
        val repositorio = RoomConflictoRepository(db, clock = { 100 }, nuevoIdVersion = { "version-resolucion" })

        repositorio.resolver(EntidadConRevisiones.ANIMAL, "animal-1", "version-b", listOf("version-b"))

        val guardado = db.animalDao().findById("animal-1")!!
        assertEquals("Gyr", guardado.raza)
        assertFalse(guardado.sincronizado)
        assertEquals(2, guardado.rev)
        assertEquals("la edición parte de la vigente", 2, guardado.revBase)
        assertEquals("version-resolucion", guardado.idVersion)
        assertEquals(100L, guardado.fechaModificacion)
        assertFalse(repositorio.observeEnConflicto(EntidadConRevisiones.ANIMAL, "animal-1").first())
        assertEquals(listOf("version-b"), db.resolucionDao().idsDeLaFila("animal", "animal-1"))
        // El sibling sigue en su tabla, oculto, hasta que el servidor acepte la edición.
        db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM sibling").use {
            it.moveToFirst()
            assertEquals(1, it.getInt(0))
        }
    }

    @Test
    fun resolverUnaFilaQueYaNoExisteDevuelveFalsoYNoEscribeNada() = runTest {
        db.siblingDao().insert(siblingConRaza("version-b", raza = "Gyr"))

        val resuelto = repositorio.resolver(EntidadConRevisiones.ANIMAL, "animal-1", "version-b", listOf("version-b"))

        assertFalse(resuelto)
        assertEquals(emptyList<String>(), db.resolucionDao().idsDeLaFila("animal", "animal-1"))
    }

    @Test
    fun unSiblingQueNoSePuedeLeerNoSeMuestraNiSePuedeElegir() = runTest {
        animalConVigente()
        // Le faltan casi todas las columnas de la fila.
        db.siblingDao().insert(sibling(idVersion = "version-roto", idFila = "animal-1"))

        assertNull(repositorio.observeConflicto(EntidadConRevisiones.ANIMAL, "animal-1").first())
        val resuelto = repositorio.resolver(EntidadConRevisiones.ANIMAL, "animal-1", "version-roto", listOf("version-roto"))

        assertFalse(resuelto)
        assertEquals("Brahman", db.animalDao().findById("animal-1")!!.raza)
        assertTrue(db.animalDao().findById("animal-1")!!.sincronizado)
    }

    @Test
    fun conservarLaVigenteTambienEsUnaEdicionNuevaQueNombraLosSiblings() = runTest {
        animalConVigente(raza = "Brahman")
        db.siblingDao().insert(siblingConRaza("version-b", raza = "Gyr"))
        val repositorio = RoomConflictoRepository(db, clock = { 100 }, nuevoIdVersion = { "version-resolucion" })

        repositorio.resolver(EntidadConRevisiones.ANIMAL, "animal-1", null, listOf("version-b"))

        val guardado = db.animalDao().findById("animal-1")!!
        assertEquals("Brahman", guardado.raza)
        assertFalse(guardado.sincronizado)
        assertEquals(2, guardado.revBase)
        assertEquals("version-resolucion", guardado.idVersion)
        assertEquals(listOf("version-b"), db.resolucionDao().idsDeLaFila("animal", "animal-1"))
    }

    @Test
    fun unSiblingQueNoEstabaEntreLosResueltosSigueEnConflicto() = runTest {
        animalConVigente()
        db.siblingDao().insert(siblingConRaza("version-b", raza = "Gyr"))
        db.siblingDao().insert(siblingConRaza("version-c", raza = "Cebú"))

        repositorio.resolver(EntidadConRevisiones.ANIMAL, "animal-1", "version-b", listOf("version-b"))

        assertTrue(repositorio.observeEnConflicto(EntidadConRevisiones.ANIMAL, "animal-1").first())
        assertEquals(
            listOf("version-c"),
            repositorio.observeSiblings(EntidadConRevisiones.ANIMAL, "animal-1").first().map { it.idVersion },
        )
    }

    @Test
    fun editarLaFilaSinResolverNoQuitaLaMarca() = runTest {
        animalConVigente()
        db.siblingDao().insert(siblingConRaza("version-b", raza = "Gyr"))

        RoomAnimalRepository(db.animalDao()).darDeBaja("animal-1", fecha = 50)

        assertTrue(repositorio.observeEnConflicto(EntidadConRevisiones.ANIMAL, "animal-1").first())
        assertEquals(emptyList<String>(), db.resolucionDao().idsDeLaFila("animal", "animal-1"))
    }

    @Test
    fun observeFilasEnConflictoListaSoloLasDeEsaEntidad() = runTest {
        db.siblingDao().insert(sibling("version-a", "animal-1"))
        db.siblingDao().insert(sibling("version-b", "animal-1"))
        db.siblingDao().insert(sibling("version-c", "animal-2"))
        db.siblingDao().insert(sibling("version-d", "evento-1").copy(entidad = "evento"))

        assertEquals(
            setOf("animal-1", "animal-2"),
            repositorio.observeFilasEnConflicto(EntidadConRevisiones.ANIMAL).first(),
        )
    }

    private fun sibling(idVersion: String, idFila: String) = SiblingEntity(
        idVersion = idVersion,
        entidad = "animal",
        idFila = idFila,
        revBase = 3,
        contenido = """{"raza":"Gyr"}""",
        idUsuario = "usuario-2",
        fechaRecepcion = 0,
    )
}
