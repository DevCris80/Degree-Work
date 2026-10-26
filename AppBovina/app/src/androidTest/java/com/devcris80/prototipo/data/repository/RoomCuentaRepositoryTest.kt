package com.devcris80.prototipo.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.devcris80.prototipo.data.local.AppDatabase
import com.devcris80.prototipo.data.local.entity.PerfilFincaEntity
import com.devcris80.prototipo.data.local.entity.UsuarioEntity
import com.devcris80.prototipo.domain.model.PerfilFinca
import com.devcris80.prototipo.domain.model.Rol
import com.devcris80.prototipo.domain.model.Usuario
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** La finca y el Usuario del dispositivo, como los dejan crear la cuenta e iniciar sesión (SupabaseSesionRepository). */
@RunWith(AndroidJUnit4::class)
class RoomCuentaRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repositorio: RoomCuentaRepository

    @Before
    fun crearBase() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .build()
        repositorio = RoomCuentaRepository(db.perfilFincaDao(), db.usuarioDao())
    }

    @After
    fun cerrarBase() = db.close()

    @Test
    fun sinCuentaNoHayFincaNiUsuario() = runTest {
        assertNull(repositorio.getPerfilFinca())
        assertNull(repositorio.observePerfilFinca().first())
        assertNull(repositorio.getUsuario())
        assertNull(repositorio.observeUsuario().first())
    }

    @Test
    fun laFincaYElUsuarioDelDispositivoSeLeenConSusDatos() = runTest {
        db.perfilFincaDao().guardar(PerfilFincaEntity("finca-1", "La Esperanza", sincronizado = true, fechaModificacion = 5))
        db.usuarioDao().guardar(UsuarioEntity("usuario-1", "finca-1", "Ana", "Operario", true, 5, delDispositivo = true))

        assertEquals(PerfilFinca("finca-1", "La Esperanza"), repositorio.getPerfilFinca())
        assertEquals(PerfilFinca("finca-1", "La Esperanza"), repositorio.observePerfilFinca().first())
        val usuario = Usuario("usuario-1", "finca-1", "Ana", Rol.OPERARIO)
        assertEquals(usuario, repositorio.getUsuario())
        assertEquals(usuario, repositorio.observeUsuario().first())
    }

    @Test
    fun losDemasUsuariosDeLaFincaNoSonElDelDispositivo() = runTest {
        db.perfilFincaDao().guardar(PerfilFincaEntity("finca-1", "La Esperanza", fechaModificacion = 5))
        db.usuarioDao().guardar(UsuarioEntity("otro", "finca-1", "Beto", "Ganadero", true, 5, delDispositivo = false))

        assertNull(repositorio.getUsuario())
    }
}
