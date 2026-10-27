package com.devcris80.prototipo.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.devcris80.prototipo.data.local.AppDatabase
import com.devcris80.prototipo.data.local.entity.RechazoEntity
import com.devcris80.prototipo.domain.model.EntidadSincronizada
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
class RoomRechazoRepositoryTest {
    private lateinit var db: AppDatabase

    @Before
    fun crearBase() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .build()
    }

    @After
    fun cerrarBase() = db.close()

    @Test
    fun soloSeObservanLosRechazosQueSiguenAbiertos() = runTest {
        db.rechazoDao().insert(rechazo("rechazo-1", fechaRechazo = 10, fechaCorreccion = null))
        db.rechazoDao().insert(rechazo("rechazo-2", fechaRechazo = 20, fechaCorreccion = 30))
        db.rechazoDao().insert(rechazo("rechazo-3", fechaRechazo = 40, fechaCorreccion = null))

        val abiertos = RoomRechazoRepository(db.rechazoDao()).observeAbiertos().first()

        assertEquals(listOf("rechazo-3", "rechazo-1"), abiertos.map { it.idRechazo })
        assertEquals(EntidadSincronizada.CHAPETA, abiertos.first().entidad)
    }

    @Test
    fun unaChapetaRechazadaTraeSuCodigoDeErrorSuAnimalYSuCodigo() = runTest {
        db.rechazoDao().insert(
            rechazo("rechazo-1", fechaRechazo = 10, fechaCorreccion = null).copy(
                contenido = """{"id_chapeta":"chapeta-1","codigo":"04A31B2C","id_animal":"lucero","fecha_asociacion":"2026-10-27T10:00:00Z"}""",
            ),
        )

        val rechazo = RoomRechazoRepository(db.rechazoDao()).observeAbiertos().first().single()

        assertEquals("23505", rechazo.codigoError)
        assertEquals("duplicate key value violates unique constraint", rechazo.mensaje)
        assertEquals("lucero", rechazo.idAnimal)
        assertEquals("04A31B2C", rechazo.codigo)
        assertFalse(rechazo.salioConSuAnimal)
    }

    @Test
    fun unRegistroQueSalioConSuAnimalLoDice() = runTest {
        db.rechazoDao().insert(
            RechazoEntity(
                idRechazo = "rechazo-1",
                entidad = "registro",
                idFila = "registro-1",
                contenido = """{"id_registro":"registro-1","id_animal":"lucero","peso":410.0}""",
                motivo = "42501: Se rechazó el animal lucero. new row violates row-level security policy",
                fechaRechazo = 10,
            ),
        )

        val rechazo = RoomRechazoRepository(db.rechazoDao()).observeAbiertos().first().single()

        assertEquals(EntidadSincronizada.REGISTRO, rechazo.entidad)
        assertEquals("42501", rechazo.codigoError)
        assertEquals("lucero", rechazo.idAnimal)
        assertNull(rechazo.codigo)
        assertTrue(rechazo.salioConSuAnimal)
    }

    @Test
    fun unAnimalRechazadoGuardaSuNombreYUnContenidoIlegibleNoLoSacaDeLaBandeja() = runTest {
        db.rechazoDao().insert(
            RechazoEntity(
                idRechazo = "rechazo-1",
                entidad = "animal",
                idFila = "lucero",
                contenido = """{"id_animal":"lucero","nombre":"Lucero"}""",
                motivo = "42501: new row violates row-level security policy",
                fechaRechazo = 20,
            ),
        )
        db.rechazoDao().insert(rechazo("rechazo-2", fechaRechazo = 10, fechaCorreccion = null).copy(contenido = "no es json"))

        val abiertos = RoomRechazoRepository(db.rechazoDao()).observeAbiertos().first()

        assertEquals("Lucero", abiertos[0].nombreAnimal)
        assertEquals("rechazo-2", abiertos[1].idRechazo)
        assertNull(abiertos[1].idAnimal)
    }

    @Test
    fun unPesajePendienteRechazadoTraeSuPesoYSuCodigo() = runTest {
        db.rechazoDao().insert(
            RechazoEntity(
                idRechazo = "rechazo-1",
                entidad = "pesaje_pendiente",
                idFila = "pendiente-1",
                contenido = """{"id_pesaje_pendiente":"pendiente-1","codigo":"0BADC0DE","peso":380.5}""",
                motivo = "55000: el pesaje pendiente ya está conciliado",
                fechaRechazo = 10,
            ),
        )

        val rechazo = RoomRechazoRepository(db.rechazoDao()).observeAbiertos().first().single()

        assertEquals("0BADC0DE", rechazo.codigo)
        assertEquals(380.5, checkNotNull(rechazo.peso), 0.0)
    }

    @Test
    fun cerrarUnRechazoLoSacaDeLaBandejaYLoConserva() = runTest {
        db.rechazoDao().insert(rechazo("rechazo-1", fechaRechazo = 10, fechaCorreccion = null))
        val rechazos = RoomRechazoRepository(db.rechazoDao(), clock = { 50 })

        rechazos.cerrar("rechazo-1")

        assertEquals(emptyList<Any>(), rechazos.observeAbiertos().first())
        db.openHelper.readableDatabase.query("SELECT idRechazo, fechaCorreccion FROM rechazo").use { fila ->
            assertTrue(fila.moveToNext())
            assertEquals("rechazo-1", fila.getString(0))
            assertEquals(50L, fila.getLong(1))
        }
    }

    private fun rechazo(idRechazo: String, fechaRechazo: Long, fechaCorreccion: Long?) = RechazoEntity(
        idRechazo = idRechazo,
        entidad = "chapeta",
        idFila = "chapeta-1",
        contenido = """{"codigo":"04A31B2C"}""",
        motivo = "23505: duplicate key value violates unique constraint",
        fechaRechazo = fechaRechazo,
        fechaCorreccion = fechaCorreccion,
    )
}
