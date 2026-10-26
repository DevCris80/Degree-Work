package com.devcris80.prototipo.data.repository

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.devcris80.prototipo.data.local.AppDatabase
import com.devcris80.prototipo.data.local.entity.PerfilFincaEntity
import com.devcris80.prototipo.data.local.entity.UsuarioEntity
import com.devcris80.prototipo.domain.model.Conciliacion
import com.devcris80.prototipo.domain.model.MotivoPesajePendiente
import com.devcris80.prototipo.domain.model.PesajePendiente
import com.devcris80.prototipo.domain.model.ResolucionPesajePendiente
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
class RoomPesajePendienteRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repositorio: RoomPesajePendienteRepository
    private var ahora = 100L

    @Before
    fun crearBase() = runTest {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .build()
        db.perfilFincaDao().guardar(PerfilFincaEntity("finca-1", "Finca de prueba", fechaModificacion = 0))
        db.usuarioDao().insert(UsuarioEntity("usuario-1", "finca-1", "Ana", fechaModificacion = 0, delDispositivo = true))
        repositorio = RoomPesajePendienteRepository(db.pesajePendienteDao(), clock = { ahora })
    }

    @After
    fun cerrarBase() = db.close()

    private fun pendiente(id: String, codigo: String = "FFEEDD00", timestamp: Long = 1L) = PesajePendiente(
        idPesajePendiente = id,
        idLectura = "lectura-$id",
        codigo = codigo,
        peso = 412.5f,
        timestamp = timestamp,
        idUsuario = "usuario-1",
        motivo = MotivoPesajePendiente.ANIMAL_DADO_DE_BAJA,
    )

    @Test
    fun guardaYLeeElPendienteConTodosSusCampos() = runTest {
        repositorio.insert(pendiente("p1"))

        assertEquals(pendiente("p1"), repositorio.findById("p1"))
        assertEquals(pendiente("p1"), repositorio.findByIdLectura("lectura-p1"))
    }

    @Test
    fun noPermiteDosPendientesConElMismoIdLectura() = runTest {
        repositorio.insert(pendiente("p1"))

        val error = runCatching { repositorio.insert(pendiente("p1").copy(idPesajePendiente = "p2")) }.exceptionOrNull()

        assertTrue("el idLectura es único: $error", error is SQLiteConstraintException)
        assertEquals(1, db.pesajePendienteDao().findPendientes().size)
    }

    @Test
    fun losSinResolverVanDelMasRecienteAlMasAntiguoYSeFiltranPorCodigo() = runTest {
        repositorio.insert(pendiente("p1", timestamp = 1L))
        repositorio.insert(pendiente("p2", timestamp = 3L))
        repositorio.insert(pendiente("p3", codigo = "OTRO", timestamp = 2L))

        assertEquals(listOf("p2", "p3", "p1"), repositorio.observeSinResolver().first().map { it.idPesajePendiente })
        assertEquals(listOf("p2", "p1"), repositorio.findSinResolverByCodigo("FFEEDD00").map { it.idPesajePendiente })
    }

    @Test
    fun conciliarGuardaQuienCuandoYComoYLoSacaDeLosSinResolver() = runTest {
        repositorio.insert(pendiente("p1"))
        val conciliacion = Conciliacion(ResolucionPesajePendiente.DESCARTADO, "usuario-1", 150L)

        repositorio.conciliar("p1", conciliacion)

        assertEquals(conciliacion, repositorio.findById("p1")?.conciliacion)
        assertTrue(repositorio.observeSinResolver().first().isEmpty())
        assertTrue(repositorio.findSinResolverByCodigo("FFEEDD00").isEmpty())
    }

    @Test
    fun conciliarEsDefinitivoUnPendienteResueltoNoCambia() = runTest {
        repositorio.insert(pendiente("p1"))
        val primera = Conciliacion(ResolucionPesajePendiente.ASIGNADO, "usuario-1", 150L)
        repositorio.conciliar("p1", primera)

        repositorio.conciliar("p1", Conciliacion(ResolucionPesajePendiente.DESCARTADO, "usuario-1", 999L))

        assertEquals(primera, repositorio.findById("p1")?.conciliacion)
    }

    @Test
    fun cadaEscrituraLocalMarcaLaFilaComoPendienteDeSubirYGuardaLaFechaDeModificacion() = runTest {
        repositorio.insert(pendiente("p1"))
        val insertado = db.pesajePendienteDao().findById("p1")!!
        assertFalse(insertado.sincronizado)
        assertEquals(100L, insertado.fechaModificacion)

        db.pesajePendienteDao().update(insertado.copy(sincronizado = true))
        ahora = 200L
        repositorio.conciliar("p1", Conciliacion(ResolucionPesajePendiente.DESCARTADO, "usuario-1", 150L))

        val conciliado = db.pesajePendienteDao().findById("p1")!!
        assertFalse(conciliado.sincronizado)
        assertEquals(200L, conciliado.fechaModificacion)
    }

    @Test
    fun siNoExisteConciliarNoHaceNada() = runTest {
        repositorio.conciliar("no-existe", Conciliacion(ResolucionPesajePendiente.DESCARTADO, "usuario-1", 1L))

        assertNull(repositorio.findById("no-existe"))
    }
}
