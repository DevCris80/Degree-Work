package com.devcris80.prototipo.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.devcris80.prototipo.data.local.AppDatabase
import com.devcris80.prototipo.domain.model.Rol
import com.devcris80.prototipo.domain.model.Usuario
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomCuentaRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repositorio: RoomCuentaRepository
    private var ahora = 100L

    @Before
    fun crearBase() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .build()
        repositorio = RoomCuentaRepository(db.perfilFincaDao(), db.usuarioDao(), clock = { ahora })
    }

    @After
    fun cerrarBase() = db.close()

    @Test
    fun sinCuentaNoHayFincaNiUsuario() = runTest {
        assertNull(repositorio.getPerfilFinca())
        assertNull(repositorio.getUsuario())
        assertNull(repositorio.observeUsuario().first())
    }

    @Test
    fun crearLaFincaLaGuardaPendienteDeSubirConSuFechaDeModificacion() = runTest {
        val perfil = repositorio.crearPerfilFinca("La Esperanza")

        assertEquals(perfil, repositorio.getPerfilFinca())
        assertEquals("La Esperanza", repositorio.observePerfilFinca().first()?.nombreFinca)
        val fila = db.perfilFincaDao().getOnce()!!
        assertFalse(fila.sincronizado)
        assertEquals(100L, fila.fechaModificacion)
    }

    @Test
    fun hayUnaSolaFincaPorDispositivoSiYaExisteSeActualizaConservandoSuId() = runTest {
        val primera = repositorio.crearPerfilFinca("La Esperanza")
        db.perfilFincaDao().guardar(db.perfilFincaDao().getOnce()!!.copy(sincronizado = true))
        ahora = 200L

        val segunda = repositorio.crearPerfilFinca("El Retiro")

        assertEquals(primera.idPerfilFinca, segunda.idPerfilFinca)
        assertEquals(1, db.perfilFincaDao().findPendientes().size)
        assertEquals("El Retiro", repositorio.getPerfilFinca()?.nombreFinca)
        assertEquals(200L, db.perfilFincaDao().getOnce()!!.fechaModificacion)
    }

    @Test
    fun elUsuarioDelDispositivoSeGuardaConSuRolYSuFinca() = runTest {
        val perfil = repositorio.crearPerfilFinca("La Esperanza")
        val usuario = Usuario("usuario-1", perfil.idPerfilFinca, "Ana", Rol.OPERARIO)

        repositorio.insertUsuario(usuario)

        assertEquals(usuario, repositorio.getUsuario())
        assertEquals(usuario, repositorio.observeUsuario().first())
        val fila = db.usuarioDao().getOnce()!!
        assertFalse(fila.sincronizado)
        assertEquals(100L, fila.fechaModificacion)
    }
}
