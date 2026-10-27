package com.devcris80.prototipo.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.devcris80.prototipo.data.local.AppDatabase
import com.devcris80.prototipo.data.local.entity.AnimalEntity
import com.devcris80.prototipo.data.local.entity.PerfilFincaEntity
import com.devcris80.prototipo.data.local.entity.UsuarioEntity
import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.model.Sexo
import com.devcris80.prototipo.domain.repository.ResultadoSincronizacion
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Las fotos de los Animales (RF-38, CU-20 pasos 6 y 8): suben y bajan al final de la sincronización, por separado,
 * contra un bucket falso que comparten dos celulares de la misma finca.
 */
@RunWith(AndroidJUnit4::class)
class MotorSincronizacionFotosTest {
    private val almacen = AlmacenFotosFalso()
    private lateinit var celularA: Celular
    private lateinit var celularB: Celular

    @Before
    fun crearCelulares() = runTest {
        celularA = Celular()
        celularB = Celular()
    }

    @After
    fun cerrarBases() {
        celularA.db.close()
        celularB.db.close()
    }

    @Test
    fun unaFotoTomadaEnUnCelularApareceEnOtroDeLaMismaFinca() = runTest {
        celularA.animales.insert(lucero(fotoUri = "content://camara/lucero.jpg"))
        celularB.db.animalDao().insert(sincronizado("lucero"))

        celularA.sincronizar()
        celularB.sincronizar()

        assertEquals(listOf("finca-1/lucero.webp"), almacen.subidas)
        val enB = celularB.animal("lucero")
        assertEquals("bajada://lucero/1", enB.fotoUri)
        assertEquals("webp de content://camara/lucero.jpg", String(celularB.fotos.guardadas.getValue("bajada://lucero/1")))
        assertFalse(enB.fotoPorSubir)
    }

    @Test
    fun laFotoQueEsteCelularSubioNoVuelveABajar() = runTest {
        celularA.animales.insert(lucero(fotoUri = "content://camara/lucero.jpg"))

        celularA.sincronizar()
        celularA.sincronizar()

        assertEquals("sube una sola vez", 1, almacen.subidas.size)
        assertEquals(emptyList<String>(), almacen.descargas)
        val enA = celularA.animal("lucero")
        assertEquals("content://camara/lucero.jpg", enA.fotoUri)
        assertEquals("version-1", enA.fotoVersion)
        assertFalse(enA.fotoPorSubir)
    }

    @Test
    fun unaFotoCambiadaEnOtroCelularReemplazaLaDeEste() = runTest {
        celularA.db.animalDao().insert(sincronizado("lucero").copy(fotoUri = "bajada://lucero/vieja", fotoVersion = "version-1"))
        almacen.archivos["finca-1/lucero.webp"] = "la nueva".toByteArray() to "version-2"

        celularA.sincronizar()

        val enA = celularA.animal("lucero")
        assertEquals("bajada://lucero/1", enA.fotoUri)
        assertEquals("version-2", enA.fotoVersion)
        assertEquals(emptyList<String>(), almacen.subidas)
    }

    @Test
    fun unaFotoEditadaQueNoSubioNoLaPisaLaDelServidor() = runTest {
        celularA.db.animalDao().insert(sincronizado("lucero").copy(fotoUri = "bajada://lucero/vieja", fotoVersion = "version-1"))
        almacen.archivos["finca-1/lucero.webp"] = "la de otro celular".toByteArray() to "version-2"
        almacen.fallaAlSubir = IllegalStateException("Storage respondió 503")
        celularA.animales.update(lucero(fotoUri = "content://camara/nueva.jpg"))

        celularA.sincronizar()

        val enA = celularA.animal("lucero")
        assertEquals("content://camara/nueva.jpg", enA.fotoUri)
        assertTrue("sube en la próxima sincronización", enA.fotoPorSubir)
        assertEquals(emptyList<String>(), almacen.descargas)
    }

    @Test
    fun sinSenalParaLasFotosLosDatosQuedanSincronizados() = runTest {
        celularA.animales.insert(lucero(fotoUri = "content://camara/lucero.jpg"))
        almacen.falla = IOException("Sin señal")

        val resultado = celularA.sincronizar()

        assertEquals(ResultadoSincronizacion.Completa(aceptados = 1, enConflicto = 0, rechazados = 0), resultado)
        val enA = celularA.animal("lucero")
        assertTrue(enA.sincronizado)
        assertTrue("la foto sube en la próxima sincronización", enA.fotoPorSubir)
        val estado = celularA.estado()
        assertEquals("no quedan datos por subir", 0, estado.cambiosSinSubir)
        assertEquals("pero sí la foto, que el trabajo en segundo plano reintenta", 1, estado.fotosSinSubir)

        almacen.falla = null
        celularA.sincronizar()

        assertEquals(0, celularA.estado().fotosSinSubir)
    }

    @Test
    fun unaFotoQueYaNoSePuedeLeerDejaDeEstarPorSubir() = runTest {
        celularA.fotos.ilegibles += "content://galeria/lucero.jpg"
        celularA.animales.insert(lucero(fotoUri = "content://galeria/lucero.jpg"))

        celularA.sincronizar()

        assertEquals(emptyList<String>(), almacen.subidas)
        assertFalse("no se reintenta en cada sincronización", celularA.animal("lucero").fotoPorSubir)
    }

    /** Un celular de la finca, con su base, su servidor de datos y sus archivos; el bucket es el de la prueba. */
    private inner class Celular {
        val db: AppDatabase = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .addCallback(AppDatabase.CALLBACK_INDICES_CHAPETA)
            .build()
        val fotos = FotosLocalesFalsas()
        val animales = RoomAnimalRepository(db.animalDao())
        private val motor = MotorSincronizacion(ServidorFalso(), db, almacen, fotos, programarTrabajo = {}, clock = { 1_000 })

        init {
            runBlocking {
                db.perfilFincaDao().guardar(PerfilFincaEntity("finca-1", "Finca", sincronizado = true, fechaModificacion = 0))
                db.usuarioDao().insert(
                    UsuarioEntity("usuario-1", "finca-1", "Ganadero", sincronizado = true, fechaModificacion = 0, delDispositivo = true),
                )
            }
        }

        suspend fun sincronizar() = motor.sincronizar()

        suspend fun estado() = motor.observeEstado().first()

        suspend fun animal(idAnimal: String) = checkNotNull(db.animalDao().findById(idAnimal))
    }

    private fun lucero(fotoUri: String?) =
        Animal("lucero", "finca-1", "Lucero", "Brahman", Sexo.HEMBRA, Etapa.VACA, 0L, proposito = Proposito.LECHE, fotoUri = fotoUri)

    private fun sincronizado(idAnimal: String) = AnimalEntity(
        idAnimal = idAnimal,
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
        idVersion = "version-$idAnimal",
    )
}
