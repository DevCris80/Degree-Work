package com.devcris80.prototipo.service

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.devcris80.prototipo.data.local.AppDatabase
import com.devcris80.prototipo.data.local.entity.PerfilFincaEntity
import com.devcris80.prototipo.data.local.entity.UsuarioEntity
import com.devcris80.prototipo.data.repository.RoomAnimalRepository
import com.devcris80.prototipo.data.repository.RoomChapetaRepository
import com.devcris80.prototipo.data.repository.RoomCuentaRepository
import com.devcris80.prototipo.data.repository.RoomPesajePendienteRepository
import com.devcris80.prototipo.data.repository.RoomRegistroRepository
import com.devcris80.prototipo.data.repository.RoomTransaccion
import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Chapeta
import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.model.Sexo
import com.devcris80.prototipo.domain.usecase.RegistrarPesaje
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.net.Socket

/**
 * RNF-07: un POST /pesaje real contra el servidor HTTP termina en un Registro o en un Pesaje pendiente
 * de Room, y reenviarlo no duplica nada (docs/contrato-pesaje.md).
 */
@RunWith(AndroidJUnit4::class)
class PesajeHttpServerIntegracionTest {
    private lateinit var db: AppDatabase
    private lateinit var servidor: PesajeHttpServer
    private lateinit var cuentas: RoomCuentaRepository
    private lateinit var animales: RoomAnimalRepository
    private lateinit var chapetas: RoomChapetaRepository
    private lateinit var registros: RoomRegistroRepository
    private lateinit var pendientes: RoomPesajePendienteRepository
    private val ahora = 1_700_000_000_000L

    @Before
    fun arrancarServidor() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .build()
        cuentas = RoomCuentaRepository(db.perfilFincaDao(), db.usuarioDao())
        animales = RoomAnimalRepository(db.animalDao())
        chapetas = RoomChapetaRepository(db.chapetaDao())
        registros = RoomRegistroRepository(db.registroDao())
        pendientes = RoomPesajePendienteRepository(db.pesajePendienteDao())
        val registrarPesaje = RegistrarPesaje(
            chapetas, animales, registros, pendientes, cuentas, RoomTransaccion(db), clock = { ahora },
        )
        // Puerto 0: el sistema elige uno libre, para no chocar con el servicio real en el 8080.
        servidor = PesajeHttpServer(0, registrarPesaje).also { it.start() }
    }

    @After
    fun pararServidor() {
        servidor.stop()
        db.close()
    }

    private fun conCuentaYAnimalConChapeta() = runBlocking {
        db.perfilFincaDao().guardar(PerfilFincaEntity("finca-1", "Finca de prueba", fechaModificacion = 0))
        db.usuarioDao().insert(UsuarioEntity("usuario-1", "finca-1", "Ana", fechaModificacion = 0, delDispositivo = true))
        animales.insert(
            Animal("animal-1", "finca-1", "Lucero", "Brahman", Sexo.HEMBRA, Etapa.VACA, 0, proposito = Proposito.LECHE),
        )
        chapetas.insert(Chapeta("chapeta-1", "04A31B2C", "animal-1", fechaAsociacion = 1L))
    }

    /** Un POST /pesaje escrito a mano sobre un socket, sin pasar por la política de tráfico HTTP de la app. */
    private fun postPesaje(cuerpo: String): Pair<Int, JSONObject> {
        Socket("127.0.0.1", servidor.listeningPort).use { socket ->
            val bytes = cuerpo.toByteArray()
            socket.getOutputStream().apply {
                write(
                    ("POST /pesaje HTTP/1.1\r\nHost: localhost\r\nContent-Type: application/json\r\n" +
                        "Content-Length: ${bytes.size}\r\nConnection: close\r\n\r\n").toByteArray(),
                )
                write(bytes)
                flush()
            }
            val respuesta = socket.getInputStream().readBytes().decodeToString()
            val estado = respuesta.lineSequence().first().split(" ")[1].toInt()
            return estado to JSONObject(respuesta.substringAfter("\r\n\r\n"))
        }
    }

    private fun pesaje(idLectura: String = "lectura-1", chip: String = "04A31B2C", peso: Double = 452.5, antiguedadMs: Long? = null) =
        JSONObject()
            .put("id_lectura", idLectura)
            .put("id_chip", chip)
            .put("peso", peso)
            .apply { antiguedadMs?.let { put("antiguedad_ms", it) } }
            .toString()

    @Test
    fun unPesajeDeUnaChapetaActivaTerminaComoRegistroEnRoomYSuReenvioNoDuplicaNada() = runBlocking {
        conCuentaYAnimalConChapeta()

        val (estado, cuerpo) = postPesaje(pesaje(antiguedadMs = 1_200))

        assertEquals(201, estado)
        assertEquals("ok", cuerpo.getString("status"))
        val registro = registros.observeByAnimal("animal-1").first().single()
        assertEquals(cuerpo.getString("id_registro"), registro.idRegistro)
        assertEquals("lectura-1", registro.idLectura)
        assertEquals(452.5f, registro.peso)
        assertEquals("usuario-1", registro.idUsuario)
        assertEquals(ahora - 1_200, registro.timestamp)

        val (estadoReintento, reintento) = postPesaje(pesaje(chip = "OTRO", peso = 10.0))

        assertEquals(201, estadoReintento)
        assertEquals(cuerpo.getString("id_registro"), reintento.getString("id_registro"))
        assertEquals(1, registros.observeByAnimal("animal-1").first().size)
    }

    @Test
    fun unChipSinChapetaTerminaComoPesajePendienteEnRoomYSuReenvioDevuelveElMismo() = runBlocking {
        conCuentaYAnimalConChapeta()

        val (estado, cuerpo) = postPesaje(pesaje(idLectura = "lectura-2", chip = "ff:ee:dd:00"))

        assertEquals(202, estado)
        assertEquals("pendiente_asociacion", cuerpo.getString("status"))
        assertEquals("CHIP_NO_ASOCIADO", cuerpo.getString("motivo"))
        val pendiente = pendientes.observeSinResolver().first().single()
        assertEquals(cuerpo.getString("id_pesaje_pendiente"), pendiente.idPesajePendiente)
        assertEquals("FFEEDD00", pendiente.codigo)

        val (estadoReintento, reintento) = postPesaje(pesaje(idLectura = "lectura-2", chip = "ff:ee:dd:00"))

        assertEquals(202, estadoReintento)
        assertEquals(cuerpo.getString("id_pesaje_pendiente"), reintento.getString("id_pesaje_pendiente"))
        assertEquals(1, pendientes.observeSinResolver().first().size)
        assertTrue(registros.observeByAnimal("animal-1").first().isEmpty())
    }

    @Test
    fun unCuerpoInvalidoRecibe400YNoGuardaNada() = runBlocking {
        conCuentaYAnimalConChapeta()

        val (faltaCampo, _) = postPesaje("""{"id_lectura":"lectura-3","peso":450}""")
        val (pesoFuera, _) = postPesaje(pesaje(idLectura = "lectura-4", peso = 1501.0))

        assertEquals(400, faltaCampo)
        assertEquals(400, pesoFuera)
        assertTrue(registros.observeByAnimal("animal-1").first().isEmpty())
        assertTrue(pendientes.observeSinResolver().first().isEmpty())
    }

    @Test
    fun sinCuentaEnElTelefonoRecibe503YNoGuardaNada() = runBlocking {
        val (estado, cuerpo) = postPesaje(pesaje())

        assertEquals(503, estado)
        assertEquals("sin sesion activa", cuerpo.getString("message"))
        assertTrue(pendientes.observeSinResolver().first().isEmpty())
    }
}
