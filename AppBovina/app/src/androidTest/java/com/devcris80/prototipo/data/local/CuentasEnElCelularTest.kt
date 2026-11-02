package com.devcris80.prototipo.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.devcris80.prototipo.data.local.entity.AnimalEntity
import com.devcris80.prototipo.data.local.entity.PerfilFincaEntity
import com.devcris80.prototipo.data.local.entity.RechazoEntity
import com.devcris80.prototipo.data.local.entity.UsuarioEntity
import com.devcris80.prototipo.data.repository.FotosLocalesFalsas
import com.devcris80.prototipo.data.repository.RoomCuentasGuardadasRepository
import com.devcris80.prototipo.domain.model.Rol
import com.devcris80.prototipo.domain.repository.ResultadoQuitarCuenta
import com.devcris80.prototipo.domain.usecase.QuitarCuentaGuardada
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Las cuentas guardadas del celular, cada una en su base (ADR-0006), con archivos reales. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class CuentasEnElCelularTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val fotos = FotosLocalesFalsas()
    private val abiertas = mutableListOf<CuentasEnElCelular>()

    @Before
    fun empezarLimpio() = limpiar()

    @After
    fun terminarLimpio() = limpiar()

    private fun limpiar() {
        abiertas.forEach { cuentas ->
            cuentas.entradas.value.forEach {
                runCatching { cuentas.base(it.archivo).close() }
                context.deleteDatabase(it.archivo)
            }
        }
        abiertas.clear()
        // commit() espera las escrituras que apply() dejó pendientes: si no, una de ellas recrea el archivo ya borrado.
        context.getSharedPreferences("cuentas_guardadas", Context.MODE_PRIVATE).edit().clear().commit()
        context.deleteSharedPreferences("cuentas_guardadas")
        context.deleteDatabase("app-bovina.db")
    }

    private fun cuentasEnElCelular(
        sesion: MutableStateFlow<Boolean> = MutableStateFlow(false),
        alcance: CoroutineScope = TestScope(),
    ) = CuentasEnElCelular(context, fotos, sesion, alcance).also { abiertas += it }

    private suspend fun guardarCuenta(
        cuentas: CuentasEnElCelular,
        idUsuario: String,
        finca: String,
        sincronizado: Boolean = true,
    ): CuentasEnElCelular.Entrada {
        val entrada = cuentas.abrirCuenta(idUsuario, "$idUsuario@finca.co")
        val base = cuentas.base(entrada.archivo)
        base.perfilFincaDao().guardar(PerfilFincaEntity("finca-$idUsuario", finca, sincronizado, fechaModificacion = 1))
        base.usuarioDao().guardar(UsuarioEntity(idUsuario, "finca-$idUsuario", "Nombre $idUsuario", "Ganadero", true, 1, true))
        return entrada
    }

    @Test
    fun sinNadaGuardado_noHayCuentasNiActiva_yLaBaseActivaEstaVacia() = runTest {
        val cuentas = cuentasEnElCelular()

        assertTrue(cuentas.entradas.value.isEmpty())
        assertNull(cuentas.activa.value)
        assertNull(cuentas.baseActiva().usuarioDao().getOnce())
    }

    @Test
    fun cadaCuentaTieneSuPropiaBase_yEntrarConOtraNoTocaLosDatosDeLaPrimera() = runTest {
        val cuentas = cuentasEnElCelular()

        val a = guardarCuenta(cuentas, "u-a", "La Esperanza")
        val b = guardarCuenta(cuentas, "u-b", "El Paraíso")

        assertNotEquals(a.archivo, b.archivo)
        assertEquals("La Esperanza", cuentas.base(a.archivo).perfilFincaDao().getOnce()?.nombreFinca)
        assertEquals("El Paraíso", cuentas.base(b.archivo).perfilFincaDao().getOnce()?.nombreFinca)
        assertEquals("u-a", cuentas.base(a.archivo).usuarioDao().getOnce()?.idUsuario)
        assertEquals("u-b", cuentas.base(b.archivo).usuarioDao().getOnce()?.idUsuario)
    }

    @Test
    fun volverAEntrarConLaMismaCuenta_reusaSuBaseYActualizaElCorreo() = runTest {
        val cuentas = cuentasEnElCelular()
        val primera = guardarCuenta(cuentas, "u-a", "La Esperanza")

        val segunda = cuentas.abrirCuenta("u-a", "nuevo@finca.co")

        assertEquals(primera.archivo, segunda.archivo)
        assertEquals("nuevo@finca.co", cuentas.entradas.value.single().correo)
    }

    @Test
    fun activarYDesactivar_cambianLaBaseActiva() = runTest {
        val cuentas = cuentasEnElCelular()
        val a = guardarCuenta(cuentas, "u-a", "La Esperanza")

        cuentas.activar(a.archivo)
        assertEquals("u-a", cuentas.baseActiva().usuarioDao().getOnce()?.idUsuario)

        cuentas.desactivar()
        assertNull(cuentas.activa.value)
        assertNull(cuentas.baseActiva().usuarioDao().getOnce())
    }

    @Test
    fun lasCuentasYLaActivaSobrevivenAReiniciarLaApp() = runTest {
        val cuentas = cuentasEnElCelular()
        val a = guardarCuenta(cuentas, "u-a", "La Esperanza")
        cuentas.activar(a.archivo)

        val despues = cuentasEnElCelular()

        assertEquals(listOf(a), despues.entradas.value)
        assertEquals(a.archivo, despues.activa.value)
    }

    @Test
    fun laBaseDeUnCelularQueYaTeniaUnaFinca_seAdoptaSinMigrarDatos() = runTest {
        AppDatabase.crear(context, "app-bovina.db").run {
            perfilFincaDao().guardar(PerfilFincaEntity("finca-1", "La Esperanza", true, fechaModificacion = 1))
            usuarioDao().guardar(UsuarioEntity("u-1", "finca-1", "Ana", "Ganadero", true, 1, true))
            close()
        }

        val conSesion = cuentasEnElCelular(MutableStateFlow(true))
        assertEquals("app-bovina.db", conSesion.activa.value)
        assertNull("hasta abrirla no se sabe de quién es", conSesion.entradas.value.single().idUsuario)
        conSesion.resolverIdentidades()
        assertEquals("u-1", conSesion.entradas.value.single().idUsuario)
        assertEquals("Ana", conSesion.baseActiva().usuarioDao().getOnce()?.nombre)
    }

    @Test
    fun laBaseAdoptadaSinSesionQuedaGuardadaPeroNoAbierta() = runTest {
        AppDatabase.crear(context, "app-bovina.db").run {
            perfilFincaDao().guardar(PerfilFincaEntity("finca-1", "La Esperanza", true, fechaModificacion = 1))
            usuarioDao().guardar(UsuarioEntity("u-1", "finca-1", "Ana", "Ganadero", true, 1, true))
            close()
        }

        val cuentas = cuentasEnElCelular()

        assertNull(cuentas.activa.value)
        assertEquals("u-1", cuentas.entradaDe("u-1")?.idUsuario)
    }

    @Test
    fun entrarConLaCuentaAdoptada_reusaSuBaseEnVezDeCrearOtra() = runTest {
        AppDatabase.crear(context, "app-bovina.db").run {
            perfilFincaDao().guardar(PerfilFincaEntity("finca-1", "La Esperanza", true, fechaModificacion = 1))
            usuarioDao().guardar(UsuarioEntity("u-1", "finca-1", "Ana", "Ganadero", true, 1, true))
            close()
        }
        val cuentas = cuentasEnElCelular()

        val entrada = cuentas.abrirCuenta("u-1", "ana@finca.co")

        assertEquals("app-bovina.db", entrada.archivo)
        assertEquals(1, cuentas.entradas.value.size)
    }

    @Test
    fun eliminarUnaCuenta_laSacaDelRegistroBorraSusFotosYSuArchivo() = runTest {
        val alcance = TestScope(testScheduler)
        val cuentas = cuentasEnElCelular(alcance = alcance)
        val a = guardarCuenta(cuentas, "u-a", "La Esperanza")
        val b = guardarCuenta(cuentas, "u-b", "El Paraíso")
        cuentas.base(a.archivo).animalDao().insert(animal("animal-a", fotoUri = "foto://a"))
        cuentas.activar(a.archivo)

        cuentas.eliminar(a.archivo)

        assertEquals(listOf(b), cuentas.entradas.value)
        assertNull("la cuenta eliminada deja de ser la abierta", cuentas.activa.value)
        assertTrue("sus fotos y su base esperan a que su pantalla deje de leerlas", fotos.borradas.isEmpty())
        assertTrue(context.getDatabasePath(a.archivo).exists())
        alcance.advanceUntilIdle()
        advanceUntilIdle()
        assertEquals(listOf("foto://a"), fotos.borradas)
        assertFalse(context.getDatabasePath(a.archivo).exists())
        assertTrue("la otra cuenta no se toca", context.getDatabasePath(b.archivo).exists())
        assertEquals("El Paraíso", cuentas.base(b.archivo).perfilFincaDao().getOnce()?.nombreFinca)
    }

    @Test
    fun lasFotosDeLasDemasCuentas_noIncluyenLasDeLaPropia() = runTest {
        val cuentas = cuentasEnElCelular()
        val a = guardarCuenta(cuentas, "u-a", "La Esperanza")
        val b = guardarCuenta(cuentas, "u-b", "El Paraíso")
        cuentas.base(a.archivo).animalDao().insert(animal("animal-a", fotoUri = "foto://a"))
        cuentas.base(b.archivo).animalDao().insert(animal("animal-b", fotoUri = "foto://b", idFinca = "finca-u-b"))

        assertEquals(setOf("foto://b"), cuentas.fotosDeLasDemasCuentas(a.archivo))
        assertEquals(setOf("foto://a", "foto://b"), cuentas.fotosDeLasDemasCuentas(null))
    }

    @Test
    fun laListaMuestraLasCuentasConLaSesionCerrada_conLoQueTienenPorSubir() = runTest {
        val cuentas = cuentasEnElCelular()
        val a = guardarCuenta(cuentas, "u-a", "La Esperanza")
        guardarCuenta(cuentas, "u-b", "El Paraíso", sincronizado = false)
        cuentas.activar(a.archivo)

        val lista = RoomCuentasGuardadasRepository(cuentas).observe().first()

        val cuenta = lista.single()
        assertEquals("u-b", cuenta.idUsuario)
        assertEquals("u-b@finca.co", cuenta.correo)
        assertEquals("El Paraíso", cuenta.nombreFinca)
        assertEquals(Rol.GANADERO, cuenta.rol)
        assertEquals(1, cuenta.cambiosSinSubir)
        assertTrue(cuenta.tienePendientes)
    }

    @Test
    fun quitarUnaCuentaConCambiosSinSubir_nolaBorra() = runTest {
        val cuentas = cuentasEnElCelular()
        val a = guardarCuenta(cuentas, "u-a", "La Esperanza", sincronizado = false)
        val repositorio = RoomCuentasGuardadasRepository(cuentas)

        val resultado = QuitarCuentaGuardada(repositorio)("u-a")

        assertEquals(ResultadoQuitarCuenta.TienePendientes, resultado)
        assertEquals(listOf(a), cuentas.entradas.value)
        assertEquals("La Esperanza", cuentas.base(a.archivo).perfilFincaDao().getOnce()?.nombreFinca)
    }

    @Test
    fun conUnaFotoPorSubirOUnRechazoAbierto_tampocoSeQuita() = runTest {
        val cuentas = cuentasEnElCelular()
        val a = guardarCuenta(cuentas, "u-a", "La Esperanza")
        val b = guardarCuenta(cuentas, "u-b", "El Paraíso")
        val repositorio = RoomCuentasGuardadasRepository(cuentas)
        cuentas.base(a.archivo).animalDao().insert(animal("animal-a", fotoUri = "foto://a").copy(fotoPorSubir = true))
        cuentas.base(b.archivo).rechazoDao().insert(rechazoAbierto("rechazo-b"))

        assertEquals(1, repositorio.pendientes("u-a"))
        assertEquals(1, repositorio.pendientes("u-b"))
        assertEquals(ResultadoQuitarCuenta.TienePendientes, QuitarCuentaGuardada(repositorio)("u-a"))
        assertEquals(ResultadoQuitarCuenta.TienePendientes, QuitarCuentaGuardada(repositorio)("u-b"))
        assertEquals(2, cuentas.entradas.value.size)
    }

    @Test
    fun unaBaseSinUsuarioPeroConCambiosSinSubir_tambienCuentaComoPendiente() = runTest {
        val cuentas = cuentasEnElCelular()
        val entrada = cuentas.abrirCuenta("u-a", "u-a@finca.co")
        cuentas.base(entrada.archivo).perfilFincaDao()
            .guardar(PerfilFincaEntity("finca-u-a", "La Esperanza", sincronizado = false, fechaModificacion = 1))
        val repositorio = RoomCuentasGuardadasRepository(cuentas)

        assertTrue("sin Usuario no sale en la lista", repositorio.observe().first().isEmpty())
        assertEquals(1, repositorio.pendientes("u-a"))
        assertEquals(ResultadoQuitarCuenta.TienePendientes, QuitarCuentaGuardada(repositorio)("u-a"))
    }

    @Test
    fun quitarUnaCuentaSinNadaPorSubir_laBorraDelCelular() = runTest {
        val alcance = TestScope(testScheduler)
        val cuentas = cuentasEnElCelular(alcance = alcance)
        val a = guardarCuenta(cuentas, "u-a", "La Esperanza")

        val resultado = QuitarCuentaGuardada(RoomCuentasGuardadasRepository(cuentas))("u-a")

        assertEquals(ResultadoQuitarCuenta.Quitada, resultado)
        assertTrue(cuentas.entradas.value.isEmpty())
        alcance.advanceUntilIdle()
        advanceUntilIdle()
        assertFalse(context.getDatabasePath(a.archivo).exists())
    }

    @Test
    fun siLaSesionSePierdeSinCerrarla_laCuentaDejaDeSerLaAbierta() = runTest {
        val sesion = MutableStateFlow(true)
        val cuentas = cuentasEnElCelular(sesion, alcance = backgroundScope)
        val a = guardarCuenta(cuentas, "u-a", "La Esperanza")
        cuentas.activar(a.archivo)
        assertTrue(cuentas.esActiva(a.archivo))

        // El servidor rechazó renovar la sesión: SesionCifrada la olvida sin pasar por cerrar sesión.
        sesion.value = false
        runCurrent()

        assertNull(cuentas.activa.value)
        assertFalse(cuentas.esActiva(a.archivo))
        assertEquals("sus datos se conservan: pudo ser una sesión vencida", listOf(a), cuentas.entradas.value)
    }

    @Test
    fun laBaseAnteriorQuitadaNoVuelveAAdoptarseAlReiniciar() = runTest {
        AppDatabase.crear(context, "app-bovina.db").run {
            perfilFincaDao().guardar(PerfilFincaEntity("finca-1", "La Esperanza", true, fechaModificacion = 1))
            usuarioDao().guardar(UsuarioEntity("u-1", "finca-1", "Ana", "Ganadero", true, 1, true))
            close()
        }
        val alcance = TestScope(testScheduler)
        val antes = cuentasEnElCelular(alcance = alcance)
        antes.eliminar("app-bovina.db")
        // La app muere antes de que se borre el archivo.

        val despues = cuentasEnElCelular()

        assertTrue(despues.entradas.value.isEmpty())
        assertFalse("el archivo sin dueño se borra al arrancar", context.getDatabasePath("app-bovina.db").exists())
    }

    @Test
    fun alArrancar_seBorranLasBasesDeCuentasQuitadasQueQuedaronSinBorrar() = runTest {
        val cuentas = cuentasEnElCelular()
        val a = guardarCuenta(cuentas, "u-a", "La Esperanza")
        AppDatabase.crear(context, "cuenta-huerfana.db").close()

        val reiniciada = cuentasEnElCelular()

        assertEquals(listOf(a), reiniciada.entradas.value)
        assertFalse(context.getDatabasePath("cuenta-huerfana.db").exists())
        assertTrue(context.getDatabasePath(a.archivo).exists())
    }

    private fun rechazoAbierto(idRechazo: String) = RechazoEntity(
        idRechazo = idRechazo,
        entidad = "animal",
        idFila = "animal-x",
        motivo = "codigo duplicado",
        contenido = "{}",
        fechaRechazo = 1L,
    )

    @Test
    fun elAvisoDeCuentaQuitadaSeDaUnaSolaVez() = runTest {
        val cuentas = cuentasEnElCelular()

        assertFalse(cuentas.consumirCuentaQuitada())
        cuentas.marcarCuentaQuitada()

        assertTrue(cuentas.consumirCuentaQuitada())
        assertFalse(cuentas.consumirCuentaQuitada())
    }

    private fun animal(idAnimal: String, fotoUri: String, idFinca: String = "finca-u-a") = AnimalEntity(
        idAnimal = idAnimal,
        idPerfilFinca = idFinca,
        nombre = idAnimal,
        raza = "Brahman",
        sexo = "Hembra",
        etapa = "Vaca",
        fechaNacimiento = 0L,
        proposito = "Carne",
        fotoUri = fotoUri,
        sincronizado = true,
        fechaModificacion = 1L,
        idVersion = "v-$idAnimal",
    )
}
