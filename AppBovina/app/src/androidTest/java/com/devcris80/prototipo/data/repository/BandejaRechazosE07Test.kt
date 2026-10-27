package com.devcris80.prototipo.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.devcris80.prototipo.data.local.AppDatabase
import com.devcris80.prototipo.data.local.entity.AnimalEntity
import com.devcris80.prototipo.data.local.entity.ChapetaEntity
import com.devcris80.prototipo.data.local.entity.PerfilFincaEntity
import com.devcris80.prototipo.data.local.entity.UsuarioEntity
import com.devcris80.prototipo.domain.model.AccionSugerida
import com.devcris80.prototipo.domain.model.MotivoRechazo
import com.devcris80.prototipo.domain.usecase.ObservarBandejaRechazos
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * El escenario E-07 (CU-22): dos celulares sin señal asocian el mismo código a Animales distintos. Este es el segundo
 * en sincronizar: el servidor rechaza su Chapeta por unicidad y la del otro celular baja en la misma sincronización.
 */
@RunWith(AndroidJUnit4::class)
class BandejaRechazosE07Test {
    private lateinit var db: AppDatabase
    private val servidor = ServidorFalso()

    @Before
    fun crearBase() = runTest {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .addCallback(AppDatabase.CALLBACK_INDICES_CHAPETA)
            .build()
        db.perfilFincaDao().guardar(PerfilFincaEntity("finca-1", "Finca", sincronizado = true, fechaModificacion = 0))
        db.usuarioDao().insert(
            UsuarioEntity("usuario-1", "finca-1", "Ganadero", sincronizado = true, fechaModificacion = 0, delDispositivo = true),
        )
    }

    @After
    fun cerrarBase() = db.close()

    @Test
    fun elSegundoCelularVeElRechazoConElNombreDelAnimalQueYaTieneElCodigoYCerrarloNoReenviaNada() = runTest {
        db.animalDao().insert(lucero())
        db.chapetaDao().insert(
            ChapetaEntity("chapeta-local", "04A31B2C", "lucero", fechaAsociacion = 10, fechaModificacion = 10),
        )
        servidor.responder = { lote ->
            JsonArray(
                lote.map {
                    val cambio = it.jsonObject
                    if (cambio.texto("entidad") == "chapeta") {
                        resultado(cambio, "rechazada", codigo = "23505", motivo = "duplicate key value violates unique constraint")
                    } else {
                        aceptada(cambio)
                    }
                },
            )
        }
        servidor.filas += CambioDelServidor("animal", 1, filaAnimal("tormenta", nombre = "Tormenta"))
        servidor.filas += CambioDelServidor("chapeta", 2, filaChapeta("chapeta-otro", "tormenta", "04A31B2C"))
        val motor = MotorSincronizacion(servidor, db, AlmacenFotosFalso(), FotosLocalesFalsas(), programarTrabajo = {})
        val rechazos = RoomRechazoRepository(db.rechazoDao())
        val bandeja = ObservarBandejaRechazos(rechazos, RoomAnimalRepository(db.animalDao()), RoomChapetaRepository(db.chapetaDao()))

        motor.sincronizar()

        val enBandeja = bandeja().first().single()
        assertEquals("Lucero", enBandeja.nombreAnimal)
        assertEquals(MotivoRechazo.CodigoEnOtroAnimal(nombreOtroAnimal = "Tormenta"), enBandeja.motivo)
        assertEquals(AccionSugerida.ASOCIAR_OTRO_CODIGO, enBandeja.accion)

        rechazos.cerrar(enBandeja.rechazo.idRechazo)
        servidor.lotes.clear()
        motor.sincronizar()

        assertEquals(emptyList<Any>(), bandeja().first())
        assertEquals("su cambio no se reenvía", emptyList<JsonArray>(), servidor.lotes)
        db.openHelper.readableDatabase.query("SELECT COUNT(*) FROM rechazo WHERE fechaCorreccion IS NOT NULL").use {
            it.moveToNext()
            assertEquals("cerrado, se conserva", 1, it.getInt(0))
        }
    }

    private fun lucero() = AnimalEntity(
        idAnimal = "lucero",
        idPerfilFinca = "finca-1",
        nombre = "Lucero",
        raza = "Brahman",
        sexo = "Hembra",
        etapa = "Vaca",
        fechaNacimiento = 0,
        proposito = "Leche",
        sincronizado = true,
        fechaModificacion = 0,
        rev = 1,
        revBase = 1,
        idVersion = "version-lucero",
    )
}
