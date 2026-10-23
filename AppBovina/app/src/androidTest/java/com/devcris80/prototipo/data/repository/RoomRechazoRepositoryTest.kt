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

    private fun rechazo(idRechazo: String, fechaRechazo: Long, fechaCorreccion: Long?) = RechazoEntity(
        idRechazo = idRechazo,
        entidad = "chapeta",
        idFila = "chapeta-1",
        contenido = """{"codigo":"04A31B2C"}""",
        motivo = "Chapeta duplicada",
        fechaRechazo = fechaRechazo,
        fechaCorreccion = fechaCorreccion,
    )
}
