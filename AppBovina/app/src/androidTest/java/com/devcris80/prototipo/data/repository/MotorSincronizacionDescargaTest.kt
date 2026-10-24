package com.devcris80.prototipo.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.devcris80.prototipo.data.local.AppDatabase
import com.devcris80.prototipo.data.local.entity.ChapetaEntity
import com.devcris80.prototipo.data.local.entity.PerfilFincaEntity
import com.devcris80.prototipo.data.local.entity.UsuarioEntity
import com.devcris80.prototipo.domain.model.EntidadConRevisiones
import com.devcris80.prototipo.domain.repository.ResultadoSincronizacion
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/** La descarga de los cambios de los demás celulares (CU-20 paso 7, DS-02), contra el servidor falso. */
@RunWith(AndroidJUnit4::class)
class MotorSincronizacionDescargaTest {
    private lateinit var db: AppDatabase
    private val servidor = ServidorFalso()

    private fun motor(tamanoPagina: Int = 100, margen: Long = 0) =
        MotorSincronizacion(servidor, db, clock = { 9_000 }, tamanoPagina = tamanoPagina, margenSolapamiento = margen)

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
    fun loQueSubioOtroCelularLlegaConSuUsuarioYQuedaSincronizado() = runTest {
        servidor.filas += CambioDelServidor("animal", 11, filaAnimal("animal-1", rev = 2, nombre = "Lucero"))
        servidor.filas += CambioDelServidor("evento", 12, filaEvento("evento-1", idAnimal = "animal-1", idUsuario = "usuario-2"))
        servidor.referencias = jsonArrayOf(
            buildJsonObject {
                put("entidad", "usuario")
                put("fila", filaUsuario("usuario-2", "Operario"))
            },
        )

        motor().sincronizar()

        val animal = db.animalDao().findById("animal-1")!!
        assertEquals("Lucero", animal.nombre)
        assertEquals(2, animal.rev)
        assertTrue(animal.sincronizado)
        assertEquals("sin escritura local", "", animal.idVersion)
        val evento = db.eventoDao().findById("evento-1")!!
        assertEquals("usuario-2", evento.idUsuario)
        assertTrue(evento.sincronizado)
        assertEquals("el Usuario del dispositivo sigue siendo el suyo", "usuario-1", db.usuarioDao().getOnce()!!.idUsuario)
        assertFalse(db.usuarioDao().findById("usuario-2")!!.delDispositivo)
        val estado = db.estadoSyncDao().observe().first()!!
        assertEquals(12L, estado.cursorDescarga)
        assertEquals(9_000L, estado.fechaUltimaSincronizacion)
    }

    @Test
    fun unaCriaQueLlegaAntesQueSuMadreSeGuardaConElla() = runTest {
        servidor.filas += CambioDelServidor("animal", 11, filaAnimal("cria", idMadre = "madre"))
        servidor.filas += CambioDelServidor("animal", 12, filaAnimal("madre", rev = 3))

        motor().sincronizar()

        assertEquals("madre", db.animalDao().findById("cria")!!.idMadre)
    }

    @Test
    fun unaBajaYUnaChapetaLiberadaEnElServidorSeReflejanEnElCelular() = runTest {
        servidor.filas += CambioDelServidor("animal", 11, filaAnimal("animal-1"))
        servidor.filas += CambioDelServidor("chapeta", 12, filaChapeta("chapeta-1", "animal-1", "FP001"))
        motor().sincronizar()
        servidor.filas.clear()
        servidor.filas += CambioDelServidor("animal", 21, filaAnimal("animal-1", rev = 2, fechaBaja = "2026-10-24T12:00:00Z"))
        servidor.filas += CambioDelServidor(
            "chapeta",
            22,
            filaChapeta("chapeta-1", "animal-1", "FP001", fechaDesasociacion = "2026-10-24T12:00:00Z"),
        )

        motor().sincronizar()

        val animal = db.animalDao().findById("animal-1")!!
        assertNotNull(animal.fechaBaja)
        assertEquals(2, animal.rev)
        assertNotNull(db.chapetaDao().findById("chapeta-1")!!.fechaDesasociacion)
        assertNull(db.chapetaDao().findActivaByCodigo("FP001"))
    }

    @Test
    fun unaEdicionLocalHechaMientrasDescargaNoSeSobrescribe() = runTest {
        servidor.filas += CambioDelServidor("animal", 11, filaAnimal("animal-1"))
        motor().sincronizar()
        servidor.filas += CambioDelServidor("animal", 12, filaAnimal("animal-1", rev = 2, nombre = "Del servidor"))
        servidor.antesDeDescargar = {
            runBlocking { RoomAnimalRepository(db.animalDao(), nuevoIdVersion = { "version-local" }).darDeBaja("animal-1", 50) }
        }

        motor().sincronizar()

        val animal = db.animalDao().findById("animal-1")!!
        assertFalse("sigue pendiente de subir", animal.sincronizado)
        assertEquals(50L, animal.fechaBaja)
        assertEquals("animal-1", animal.nombre)
        assertEquals("la edición parte de la revisión que tenía", 1, animal.revBase)
    }

    @Test
    fun unaSegundaDescargaSinCambiosSoloPideElMargen() = runTest {
        servidor.filas += CambioDelServidor("animal", 11, filaAnimal("animal-1"))
        servidor.filas += CambioDelServidor("animal", 30, filaAnimal("animal-2"))
        motor(margen = 5).sincronizar()

        motor(margen = 5).sincronizar()

        assertEquals(listOf(0L, 25L), servidor.descargas.map { it.first })
        assertEquals("el cursor no retrocede", 30L, db.estadoSyncDao().getOnce()!!.cursorDescarga)
    }

    @Test
    fun unaDescargaCortadaSeRetomaDesdeElUltimoCursorGuardado() = runTest {
        servidor.filas += CambioDelServidor("animal", 11, filaAnimal("animal-1"))
        servidor.filas += CambioDelServidor("animal", 12, filaAnimal("animal-2"))
        servidor.filas += CambioDelServidor("animal", 13, filaAnimal("animal-3"))
        servidor.antesDeDescargar = { if (servidor.descargas.size > 1) throw IOException("sin señal") }

        assertEquals(ResultadoSincronizacion.SinConexion, motor(tamanoPagina = 2).sincronizar())
        assertNotNull(db.animalDao().findById("animal-2"))
        assertNull(db.animalDao().findById("animal-3"))
        val cortada = db.estadoSyncDao().getOnce()!!
        assertEquals(12L, cortada.cursorDescarga)
        assertNull("no terminó", cortada.fechaUltimaSincronizacion)

        servidor.antesDeDescargar = null
        servidor.descargas.clear()
        motor(tamanoPagina = 2).sincronizar()

        assertEquals(listOf(12L to 2), servidor.descargas)
        assertNotNull(db.animalDao().findById("animal-3"))
        assertEquals(9_000L, db.estadoSyncDao().getOnce()!!.fechaUltimaSincronizacion)
    }

    @Test
    fun unSiblingSinResolverDejaLaFilaEnConflictoHastaQueLlegaResuelto() = runTest {
        servidor.filas += CambioDelServidor("animal", 11, filaAnimal("animal-1", rev = 2))
        servidor.filas += CambioDelServidor("sibling", 12, filaSibling("version-otro", "animal-1"))
        val conflictos = RoomConflictoRepository(db.siblingDao())

        motor().sincronizar()

        assertTrue(conflictos.observeEnConflicto(EntidadConRevisiones.ANIMAL, "animal-1").first())
        val sibling = conflictos.observeSiblings(EntidadConRevisiones.ANIMAL, "animal-1").first().single()
        assertEquals("version-otro", sibling.idVersion)
        assertTrue(sibling.contenido.contains("\"raza\":\"Gyr\""))

        servidor.filas += CambioDelServidor("sibling", 13, filaSibling("version-otro", "animal-1", resueltoPor = "version-resolucion"))
        motor().sincronizar()

        assertFalse(conflictos.observeEnConflicto(EntidadConRevisiones.ANIMAL, "animal-1").first())
    }

    @Test
    fun unaChapetaNuevaQueChocaConLaDelServidorPasaALaBandeja() = runTest {
        servidor.filas += CambioDelServidor("animal", 11, filaAnimal("animal-1"))
        servidor.filas += CambioDelServidor("animal", 12, filaAnimal("animal-2"))
        motor().sincronizar()
        servidor.filas += CambioDelServidor("chapeta", 13, filaChapeta("chapeta-servidor", "animal-1", "FP001"))
        // En este celular, alguien le pone el mismo código a otro Animal mientras descarga.
        servidor.antesDeDescargar = {
            runBlocking {
                db.chapetaDao().insert(
                    ChapetaEntity("chapeta-local", "FP001", "animal-2", fechaAsociacion = 0, fechaModificacion = 0),
                )
            }
        }

        motor().sincronizar()

        assertEquals("chapeta-servidor", db.chapetaDao().findActivaByCodigo("FP001")!!.idChapeta)
        assertNull(db.chapetaDao().findById("chapeta-local"))
        val rechazo = db.rechazoDao().observeAbiertos().first().single()
        assertEquals("chapeta-local", rechazo.idFila)
        assertTrue(rechazo.motivo.startsWith("23505: "))
    }
}

private fun jsonArrayOf(vararg elementos: JsonObject) = JsonArray(elementos.toList())

internal fun filaAnimal(
    idAnimal: String,
    rev: Int = 1,
    nombre: String = idAnimal,
    idMadre: String? = null,
    fechaBaja: String? = null,
) = buildJsonObject {
    put("id_animal", idAnimal)
    put("id_perfil_finca", "finca-1")
    put("id_padre", null as String?)
    put("id_madre", idMadre)
    put("nombre", nombre)
    put("raza", "Holstein")
    put("sexo", "Hembra")
    put("etapa", "Vaca")
    put("fecha_nacimiento", "2024-01-01T00:00:00Z")
    put("fecha_nacimiento_es_estimada", false)
    put("proposito", "Leche")
    put("fecha_baja", fechaBaja)
    put("fecha_modificacion", "2026-10-24T10:00:00Z")
    put("rev", rev)
}

internal fun filaEvento(idEvento: String, idAnimal: String, idUsuario: String = "usuario-1", rev: Int = 1) = buildJsonObject {
    put("id_evento", idEvento)
    put("id_animal", idAnimal)
    put("id_usuario", idUsuario)
    put("id_perfil_finca", "finca-1")
    put("tipo_evento", "Vacunación")
    put("fecha", "2026-10-20T08:00:00Z")
    put("detalle", "Aftosa")
    put("fecha_baja", null as String?)
    put("fecha_modificacion", "2026-10-24T10:00:00Z")
    put("rev", rev)
}

internal fun filaUsuario(idUsuario: String, nombre: String, rol: String = "Operario") = buildJsonObject {
    put("id_usuario", idUsuario)
    put("id_perfil_finca", "finca-1")
    put("nombre", nombre)
    put("rol", rol)
    put("fecha_modificacion", "2026-10-24T10:00:00Z")
}

internal fun filaChapeta(idChapeta: String, idAnimal: String, codigo: String, fechaDesasociacion: String? = null) =
    buildJsonObject {
        put("id_chapeta", idChapeta)
        put("codigo", codigo)
        put("id_animal", idAnimal)
        put("id_perfil_finca", "finca-1")
        put("fecha_asociacion", "2026-10-01T00:00:00Z")
        put("fecha_desasociacion", fechaDesasociacion)
        put("fecha_modificacion", "2026-10-24T10:00:00Z")
    }

internal fun filaSibling(idVersion: String, idFila: String, resueltoPor: String? = null) = buildJsonObject {
    put("id_version", idVersion)
    put("entidad", "animal")
    put("id_fila", idFila)
    put("id_perfil_finca", "finca-1")
    put("rev_base", 1)
    put("contenido", buildJsonObject { put("raza", "Gyr") })
    put("id_usuario", "usuario-1")
    put("fecha_recepcion", "2026-10-24T10:00:00Z")
    put("resuelto_por", resueltoPor)
    put("fecha_resolucion", resueltoPor?.let { "2026-10-24T11:00:00Z" })
}
