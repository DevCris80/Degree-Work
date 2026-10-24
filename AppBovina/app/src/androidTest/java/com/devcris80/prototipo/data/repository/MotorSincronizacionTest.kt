package com.devcris80.prototipo.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.devcris80.prototipo.data.local.AppDatabase
import com.devcris80.prototipo.data.local.entity.AnimalEntity
import com.devcris80.prototipo.data.local.entity.ChapetaEntity
import com.devcris80.prototipo.data.local.entity.EventoEntity
import com.devcris80.prototipo.data.local.entity.PerfilFincaEntity
import com.devcris80.prototipo.data.local.entity.RegistroEntity
import com.devcris80.prototipo.data.local.entity.ResolucionEntity
import com.devcris80.prototipo.data.local.entity.UsuarioEntity
import com.devcris80.prototipo.data.remote.ServidorSincronizacion
import com.devcris80.prototipo.domain.model.EntidadConRevisiones
import com.devcris80.prototipo.domain.model.EntidadSincronizada
import com.devcris80.prototipo.domain.repository.EstadoSincronizacion
import com.devcris80.prototipo.domain.repository.ResultadoSincronizacion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

/** La subida de cambios (CU-20, DS-01) contra un servidor falso que habla el contrato de supabase/README.md. */
@RunWith(AndroidJUnit4::class)
class MotorSincronizacionTest {
    private lateinit var db: AppDatabase
    private val servidor = ServidorFalso()

    private fun motor(tamanoLote: Int = 25) =
        MotorSincronizacion(servidor, db, programarTrabajo = {}, clock = { 1_000 }, tamanoLote = tamanoLote)

    @Before
    fun crearBase() = runTest {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .addCallback(AppDatabase.CALLBACK_INDICES_CHAPETA)
            .build()
        // La finca y el Usuario llegan del servidor al iniciar sesión: ya están sincronizados.
        db.perfilFincaDao().guardar(PerfilFincaEntity("finca-1", "Finca", sincronizado = true, fechaModificacion = 0))
        db.usuarioDao().insert(
            UsuarioEntity("usuario-1", "finca-1", "Ganadero", sincronizado = true, fechaModificacion = 0, delDispositivo = true),
        )
    }

    @After
    fun cerrarBase() = db.close()

    @Test
    fun veinteAnimalesYCincuentaPesajesSubenDePadresAHijosYQuedanSincronizados() = runTest {
        // La madre se editó después de registrar a su cría: su cambio es el más reciente, y aun así sube primero.
        db.animalDao().insert(animal("madre", fechaModificacion = 30))
        db.animalDao().insert(animal("cria", idMadre = "madre", fechaModificacion = 20))
        repeat(18) { db.animalDao().insert(animal("animal-$it", fechaModificacion = 10)) }
        repeat(50) { db.registroDao().insert(registro("registro-$it", idAnimal = "animal-${it % 18}")) }
        db.chapetaDao().insert(chapeta("chapeta-1", idAnimal = "madre"))

        val resultado = motor(tamanoLote = 7).sincronizar()

        assertEquals(ResultadoSincronizacion.Completa(aceptados = 71, enConflicto = 0, rechazados = 0), resultado)
        assertTrue(servidor.lotes.all { it.size <= 7 })
        val enviados = servidor.lotes.flatten().map { it.jsonObject }
        assertEquals(
            "de padres a hijos",
            listOf("animal", "chapeta", "registro"),
            enviados.map { it.texto("entidad") }.distinct(),
        )
        val ids = enviados.map { it.getValue("fila").jsonObject.texto("id_animal") }
        assertTrue("la madre antes que su cría", ids.indexOf("madre") < ids.indexOf("cria"))
        assertEquals(emptyList<AnimalEntity>(), db.animalDao().findPendientes())
        assertEquals(emptyList<RegistroEntity>(), db.registroDao().findPendientes())
        assertEquals(1, db.animalDao().findById("cria")!!.rev)
    }

    @Test
    fun elLoteLlevaLaFilaConLasColumnasDelServidor() = runTest {
        db.animalDao().insert(animal("animal-1", idMadre = null, fechaModificacion = 20).copy(fotoUri = "content://foto"))

        motor().sincronizar()

        val cambio = servidor.lotes.single().single().jsonObject
        assertEquals("animal", cambio.texto("entidad"))
        assertEquals("version-animal-1", cambio.texto("id_version"))
        assertEquals(JsonNull, cambio["rev_base"])
        val fila = cambio.getValue("fila").jsonObject
        assertEquals("animal-1", fila.texto("id_animal"))
        assertEquals("finca-1", fila.texto("id_perfil_finca"))
        assertEquals("Hembra", fila.texto("sexo"))
        assertEquals("1970-01-01T00:00:00.020Z", fila.texto("fecha_modificacion"))
        assertFalse("la foto no viaja en la fila", "foto_uri" in fila)
    }

    @Test
    fun unaFilaEditadaMientrasSubiaQuedaPendienteSobreLaRevisionAceptada() = runTest {
        db.animalDao().insert(animal("animal-1").copy(sincronizado = true, rev = 3))
        val animales = RoomAnimalRepository(db.animalDao(), nuevoIdVersion = { "version-local-1" })
        animales.darDeBaja("animal-1", fecha = 50)
        servidor.responder = { lote ->
            // La persona vuelve a editar el Animal mientras el lote viaja.
            RoomAnimalRepository(db.animalDao(), nuevoIdVersion = { "version-local-2" }).darDeBaja("animal-1", 60)
            aceptarTodo(lote)
        }

        motor().sincronizar()

        val guardado = db.animalDao().findById("animal-1")!!
        assertFalse(guardado.sincronizado)
        assertEquals("version-local-2", guardado.idVersion)
        assertEquals(60L, guardado.fechaBaja)
        assertEquals("la edición nueva parte de la revisión aceptada", 4, guardado.revBase)
        assertEquals(4, guardado.rev)
    }

    @Test
    fun unSiblingDejaLaFilaConLaVigenteYGuardaLaEdicionLocal() = runTest {
        db.animalDao().insert(
            animal("animal-1").copy(raza = "Gyr", rev = 1, revBase = 1, idVersion = "version-local", fotoUri = "content://foto"),
        )
        servidor.responder = { lote ->
            val cambio = lote.single().jsonObject
            val vigente = JsonObject(cambio.getValue("fila").jsonObject + mapOf("raza" to JsonPrimitive("Brahman rojo"), "rev" to JsonPrimitive(2)))
            JsonArray(listOf(resultado(cambio, "sibling", rev = 2, fila = vigente)))
        }

        val resultado = motor().sincronizar()

        assertEquals(ResultadoSincronizacion.Completa(aceptados = 0, enConflicto = 1, rechazados = 0), resultado)
        val guardado = db.animalDao().findById("animal-1")!!
        assertEquals("Brahman rojo", guardado.raza)
        assertEquals(2, guardado.rev)
        assertTrue(guardado.sincronizado)
        assertEquals("content://foto", guardado.fotoUri)
        val conflictos = RoomConflictoRepository(db)
        assertTrue(conflictos.observeEnConflicto(EntidadConRevisiones.ANIMAL, "animal-1").first())
        val sibling = conflictos.observeSiblings(EntidadConRevisiones.ANIMAL, "animal-1").first().single()
        assertEquals("version-local", sibling.idVersion)
        assertEquals(1, sibling.revBase)
        assertTrue(sibling.contenido.contains("\"raza\":\"Gyr\""))
    }

    @Test
    fun laEdicionQueResuelveNombraAlServidorSusSiblingsYLosOlvidaAlSubir() = runTest {
        db.animalDao().insert(animal("animal-1").copy(rev = 2, revBase = 2))
        db.resolucionDao().insert(
            listOf(
                ResolucionEntity("version-b", "animal", "animal-1"),
                ResolucionEntity("version-c", "animal", "animal-1"),
                ResolucionEntity("version-otra-fila", "animal", "animal-2"),
            ),
        )

        motor().sincronizar()

        val cambio = servidor.lotes.single().single().jsonObject
        assertEquals(2, cambio.getValue("rev_base").jsonPrimitive.intOrNull)
        assertEquals(listOf("version-b", "version-c"), cambio.getValue("resuelve").jsonArray.map { it.jsonPrimitive.content })
        assertEquals(emptyList<String>(), db.resolucionDao().idsDeLaFila("animal", "animal-1"))
        assertEquals(listOf("version-otra-fila"), db.resolucionDao().idsDeLaFila("animal", "animal-2"))
    }

    @Test
    fun unaEdicionSinSiblingsPorResolverNoLlevaResuelve() = runTest {
        db.animalDao().insert(animal("animal-1"))

        motor().sincronizar()

        assertFalse("resuelve" in servidor.lotes.single().single().jsonObject)
    }

    @Test
    fun unaChapetaRechazadaQueElServidorNoTieneSaleALaBandejaYElRestoDelLoteSeAcepta() = runTest {
        db.animalDao().insert(animal("animal-1").copy(sincronizado = true, rev = 1))
        db.animalDao().insert(animal("animal-2").copy(sincronizado = true, rev = 1))
        db.chapetaDao().insert(chapeta("chapeta-1", idAnimal = "animal-1", codigo = "04A31B2C"))
        db.chapetaDao().insert(chapeta("chapeta-2", idAnimal = "animal-2", codigo = "04A31B2D"))
        servidor.responder = { lote ->
            JsonArray(
                lote.map {
                    val cambio = it.jsonObject
                    if (cambio.getValue("fila").jsonObject.texto("id_chapeta") == "chapeta-1") {
                        resultado(cambio, "rechazada", codigo = "23505", motivo = "Chapeta duplicada")
                    } else {
                        aceptada(cambio)
                    }
                },
            )
        }

        val resultado = motor().sincronizar()

        assertEquals(ResultadoSincronizacion.Completa(aceptados = 1, enConflicto = 0, rechazados = 1), resultado)
        assertNull(db.chapetaDao().findById("chapeta-1"))
        assertTrue(db.chapetaDao().findById("chapeta-2")!!.sincronizado)
        val rechazo = RoomRechazoRepository(db.rechazoDao()).observeAbiertos().first().single()
        assertEquals(EntidadSincronizada.CHAPETA, rechazo.entidad)
        assertEquals("chapeta-1", rechazo.idFila)
        assertEquals("23505: Chapeta duplicada", rechazo.motivo)
        assertTrue(rechazo.contenido.contains("\"codigo\":\"04A31B2C\""))

        servidor.lotes.clear()
        motor().sincronizar()
        assertEquals("el rechazo no se reenvía", emptyList<JsonArray>(), servidor.lotes)
    }

    @Test
    fun unaModificacionRechazadaVuelveALaVersionDelServidorYQuedaEnLaBandeja() = runTest {
        db.animalDao().insert(animal("animal-1").copy(sincronizado = true, rev = 3))
        RoomAnimalRepository(db.animalDao()).darDeBaja("animal-1", fecha = 50)
        servidor.responder = { lote ->
            val cambio = lote.single().jsonObject
            val delServidor = JsonObject(
                cambio.getValue("fila").jsonObject + mapOf("fecha_baja" to JsonNull, "rev" to JsonPrimitive(3)),
            )
            JsonArray(listOf(resultado(cambio, "rechazada", codigo = "42501", motivo = "Un Operario no puede dar de baja un Animal", fila = delServidor)))
        }

        motor().sincronizar()

        val guardado = db.animalDao().findById("animal-1")!!
        assertNull(guardado.fechaBaja)
        assertTrue(guardado.sincronizado)
        assertEquals(3, guardado.rev)
        val rechazo = db.rechazoDao().observeAbiertos().first().single()
        assertEquals("animal-1", rechazo.idFila)
        assertTrue(rechazo.motivo.startsWith("42501: "))
    }

    @Test
    fun unaChapetaQueVuelveAQuedarActivaPorUnRechazoSacaALaBandejaALaNuevaConSuCodigo() = runTest {
        db.animalDao().insert(animal("animal-1").copy(sincronizado = true, rev = 1))
        db.animalDao().insert(animal("animal-2").copy(sincronizado = true, rev = 1))
        db.chapetaDao().insert(chapeta("chapeta-1", idAnimal = "animal-1", codigo = "FP001").copy(sincronizado = true))
        // Un Operario libera la Chapeta y le pone el mismo código a otro Animal.
        db.chapetaDao().desasociar("chapeta-1", fecha = 10, fechaModificacion = 10)
        db.chapetaDao().insert(chapeta("chapeta-2", idAnimal = "animal-2", codigo = "FP001").copy(fechaModificacion = 20))
        servidor.responder = { lote ->
            JsonArray(
                lote.map {
                    val cambio = it.jsonObject
                    val fila = cambio.getValue("fila").jsonObject
                    val delServidor = JsonObject(fila + ("fecha_desasociacion" to JsonNull))
                    if (fila.texto("id_chapeta") == "chapeta-1") {
                        resultado(cambio, "rechazada", codigo = "42501", motivo = "Un Operario no puede liberar una Chapeta", fila = delServidor)
                    } else {
                        resultado(cambio, "rechazada", codigo = "23505", motivo = "Chapeta duplicada")
                    }
                },
            )
        }

        motor().sincronizar()

        assertEquals("chapeta-1", db.chapetaDao().findActivaByCodigo("FP001")!!.idChapeta)
        assertNull(db.chapetaDao().findById("chapeta-2"))
        assertEquals(
            setOf("chapeta-1", "chapeta-2"),
            db.rechazoDao().observeAbiertos().first().map { it.idFila }.toSet(),
        )
    }

    @Test
    fun unAnimalRechazadoQueElServidorNoTieneSacaASusDependientesALaBandeja() = runTest {
        db.animalDao().insert(animal("madre"))
        db.animalDao().insert(animal("cria", idMadre = "madre"))
        db.chapetaDao().insert(chapeta("chapeta-1", idAnimal = "madre"))
        db.eventoDao().insert(evento("evento-1", idAnimal = "cria"))
        db.registroDao().insert(registro("registro-1", idAnimal = "madre"))
        servidor.responder = { lote ->
            JsonArray(lote.map { resultado(it.jsonObject, "rechazada", codigo = "23514", motivo = "Datos inválidos") })
        }

        motor().sincronizar()

        assertNull(db.animalDao().findById("madre"))
        assertNull(db.animalDao().findById("cria"))
        assertNull(db.chapetaDao().findById("chapeta-1"))
        assertNull(db.eventoDao().findById("evento-1"))
        assertNull(db.registroDao().findById("registro-1"))
        val rechazos = db.rechazoDao().observeAbiertos().first()
        assertEquals(
            setOf("madre", "cria", "chapeta-1", "evento-1", "registro-1"),
            rechazos.map { it.idFila }.toSet(),
        )
        assertTrue(rechazos.single { it.idFila == "evento-1" }.motivo.startsWith("23514: "))
        assertEquals("las dependientes no llegan a subir", 1, servidor.lotes.size)
    }

    @Test
    fun unaCriaQueElServidorYaTieneNoSaleConSuMadreRechazada() = runTest {
        db.animalDao().insert(animal("madre"))
        // Una cría ya subida, editada para apuntar a una madre nueva.
        db.animalDao().insert(animal("cria", idMadre = "madre").copy(rev = 2, revBase = 2))
        servidor.responder = { lote ->
            JsonArray(lote.map { resultado(it.jsonObject, "rechazada", codigo = "23514", motivo = "Datos inválidos") })
        }

        motor().sincronizar()

        val cria = db.animalDao().findById("cria")!!
        assertTrue("su cambio queda en la bandeja y no se reenvía", cria.sincronizado)
        assertEquals(setOf("madre", "cria"), db.rechazoDao().observeAbiertos().first().map { it.idFila }.toSet())
    }

    @Test
    fun unRechazoDeUnaFilaEditadaMientrasSubiaGuardaSuUltimaVersionYSacaASusDependientes() = runTest {
        db.animalDao().insert(animal("animal-1"))
        db.registroDao().insert(registro("registro-1", idAnimal = "animal-1"))
        servidor.responder = { lote ->
            RoomAnimalRepository(db.animalDao(), nuevoIdVersion = { "version-local-2" }).darDeBaja("animal-1", 60)
            JsonArray(lote.map { resultado(it.jsonObject, "rechazada", codigo = "23514", motivo = "Datos inválidos") })
        }

        motor().sincronizar()

        assertNull(db.animalDao().findById("animal-1"))
        assertNull(db.registroDao().findById("registro-1"))
        val rechazos = db.rechazoDao().observeAbiertos().first()
        assertTrue(rechazos.single { it.idFila == "animal-1" }.contenido.contains("\"fecha_baja\":\"1970-01-01T00:00:00.060Z\""))
        assertEquals("las dependientes no llegan a subir", 1, servidor.lotes.size)
    }

    @Test
    fun sinSenalAMitadLosLotesConfirmadosQuedanYElRestoSeReenvia() = runTest {
        repeat(3) { db.animalDao().insert(animal("animal-$it", fechaModificacion = it.toLong())) }
        servidor.responder = { lote ->
            if (servidor.lotes.size > 1) throw IOException("sin señal")
            aceptarTodo(lote)
        }

        assertEquals(ResultadoSincronizacion.SinConexion, motor(tamanoLote = 2).sincronizar())
        assertEquals(listOf("animal-2"), db.animalDao().findPendientes().map { it.idAnimal })

        servidor.lotes.clear()
        servidor.responder = ::aceptarTodo
        motor(tamanoLote = 2).sincronizar()
        assertEquals(1, servidor.lotes.single().size)
        assertEquals(emptyList<AnimalEntity>(), db.animalDao().findPendientes())
    }

    @Test
    fun losCambiosSinSubirSonLasFilasPendientesYLaFechaQuedaAlTerminar() = runTest {
        db.animalDao().insert(animal("animal-1"))
        db.registroDao().insert(registro("registro-1", idAnimal = "animal-1"))
        val motor = motor()
        assertEquals(EstadoSincronizacion(cambiosSinSubir = 2, ultimaSincronizacion = null), motor.observeEstado().first())

        motor.sincronizar()

        assertEquals(EstadoSincronizacion(cambiosSinSubir = 0, ultimaSincronizacion = 1_000), motor.observeEstado().first())
    }

    @Test
    fun dosSincronizacionesALaVezNoSeSolapanNiSubenDosVecesLoMismo() = runTest {
        db.animalDao().insert(animal("animal-1"))
        val subiendo = AtomicInteger()
        val maximo = AtomicInteger()
        servidor.responder = { lote ->
            maximo.accumulateAndGet(subiendo.incrementAndGet(), ::maxOf)
            delay(200)
            subiendo.decrementAndGet()
            aceptarTodo(lote)
        }
        val motor = motor()

        // En hilos de verdad, como el botón y el trabajo en segundo plano.
        withContext(Dispatchers.Default) { List(2) { async { motor.sincronizar() } }.awaitAll() }

        assertEquals(1, maximo.get())
        assertEquals("la segunda encuentra el Animal ya sincronizado", 1, servidor.lotes.size)
        assertEquals(2, servidor.descargas.size)
    }

    private fun animal(idAnimal: String, idMadre: String? = null, fechaModificacion: Long = 0) = AnimalEntity(
        idAnimal = idAnimal,
        idPerfilFinca = "finca-1",
        nombre = idAnimal,
        raza = "Brahman",
        sexo = "Hembra",
        etapa = "Vaca",
        fechaNacimiento = 0,
        proposito = "Leche",
        idMadre = idMadre,
        fechaModificacion = fechaModificacion,
        idVersion = "version-$idAnimal",
    )

    private fun chapeta(idChapeta: String, idAnimal: String, codigo: String = "04A31B2C") = ChapetaEntity(
        idChapeta = idChapeta,
        codigo = codigo,
        idAnimal = idAnimal,
        fechaAsociacion = 0,
        fechaModificacion = 0,
    )

    private fun evento(idEvento: String, idAnimal: String) = EventoEntity(
        idEvento = idEvento,
        idAnimal = idAnimal,
        idUsuario = "usuario-1",
        tipoEvento = "Vacunación",
        fecha = 0,
        detalle = "Aftosa",
        fechaModificacion = 0,
        idVersion = "version-$idEvento",
    )

    private fun registro(idRegistro: String, idAnimal: String) = RegistroEntity(
        idRegistro = idRegistro,
        idAnimal = idAnimal,
        idUsuario = "usuario-1",
        peso = 410.5f,
        timestamp = 0,
        fechaModificacion = 0,
        idVersion = "version-$idRegistro",
    )
}
