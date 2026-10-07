package com.devcris80.prototipo.data

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ChapetaDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var chapetaDao: ChapetaDao
    private lateinit var idAnimal: String

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).addCallback(AppDatabase.CALLBACK_INDICES_CHAPETA).allowMainThreadQueries().build()
        chapetaDao = db.chapetaDao()
        db.perfilFincaDao().guardar(PerfilFinca(nombreFinca = "Finca test"))
        idAnimal = UUID.randomUUID().toString()
        db.animalDao().insert(
            Animal(
                idAnimal = idAnimal,
                idPerfilFinca = PERFIL_FINCA_ID,
                nombre = "Manchas",
                raza = "Holstein",
                sexo = "Hembra",
                etapa = "Vaca",
                fechaNacimiento = 1_700_000_000_000L,
                proposito = "Leche",
            ),
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun chapeta(codigo: String, fechaDesasociacion: Long? = null) = Chapeta(
        idChapeta = UUID.randomUUID().toString(),
        codigo = codigo,
        idAnimal = idAnimal,
        fechaAsociacion = 1L,
        fechaDesasociacion = fechaDesasociacion,
    )

    @Test
    fun segundaChapetaActivaConMismoCodigo_falla() = runTest {
        chapetaDao.insert(chapeta("ABC123"))

        assertThrows(SQLiteConstraintException::class.java) {
            runBlocking { chapetaDao.insert(chapeta("ABC123")) }
        }
    }

    @Test
    fun codigoLiberado_puedeAsociarseDeNuevo() = runTest {
        val original = chapeta("ABC123")
        chapetaDao.insert(original)
        chapetaDao.desasociar(original.idChapeta, 2L)

        chapetaDao.insert(chapeta("ABC123"))

        assertNotNull(chapetaDao.findActivaByCodigo("ABC123"))
    }

    @Test
    fun findActivaByCodigo_ignoraChapetasDesasociadas() = runTest {
        val original = chapeta("ABC123")
        chapetaDao.insert(original)
        chapetaDao.desasociar(original.idChapeta, 2L)

        assertNull(chapetaDao.findActivaByCodigo("ABC123"))
    }
}
