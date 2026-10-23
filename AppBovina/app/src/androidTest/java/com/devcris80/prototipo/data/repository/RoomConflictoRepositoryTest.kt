package com.devcris80.prototipo.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.devcris80.prototipo.data.local.AppDatabase
import com.devcris80.prototipo.data.local.entity.SiblingEntity
import com.devcris80.prototipo.domain.model.EntidadConRevisiones
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        repositorio = RoomConflictoRepository(db.siblingDao())
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
