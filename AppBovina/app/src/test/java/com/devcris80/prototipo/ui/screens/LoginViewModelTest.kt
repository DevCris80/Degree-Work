package com.devcris80.prototipo.ui.screens

import com.devcris80.prototipo.domain.model.CuentaGuardada
import com.devcris80.prototipo.domain.model.Rol
import com.devcris80.prototipo.domain.repository.ResultadoInicioSesion
import com.devcris80.prototipo.domain.repository.ResultadoRecuperacion
import com.devcris80.prototipo.domain.usecase.QuitarCuentaGuardada
import com.devcris80.prototipo.testing.FakeCuentasGuardadasRepository
import com.devcris80.prototipo.testing.FakeSesionRepository
import com.devcris80.prototipo.testing.FakeSincronizacionRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LoginViewModelTest {
    private val sesionRepository = FakeSesionRepository()
    private val sincronizacionRepository = FakeSincronizacionRepository()
    private val cuentasGuardadas = FakeCuentasGuardadasRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun crearViewModel() =
        LoginViewModel(sesionRepository, sincronizacionRepository, cuentasGuardadas, QuitarCuentaGuardada(cuentasGuardadas))

    private fun llenado() = crearViewModel().apply {
        onCorreoCambiado("ana@finca.co")
        onContrasenaCambiada("secreta")
    }

    private fun cuenta(
        idUsuario: String = "u-1",
        correo: String? = "luis@otra-finca.co",
        cambiosSinSubir: Int = 0,
        fotosSinSubir: Int = 0,
        rechazosAbiertos: Int = 0,
    ) = CuentaGuardada(
        idUsuario = idUsuario,
        correo = correo,
        nombreUsuario = "Luis",
        rol = Rol.GANADERO,
        nombreFinca = "La Esperanza",
        cambiosSinSubir = cambiosSinSubir,
        fotosSinSubir = fotosSinSubir,
        rechazosAbiertos = rechazosAbiertos,
    )

    @Test
    fun sinCorreoOContrasena_noSePuedeIniciar() {
        val viewModel = crearViewModel()
        viewModel.onCorreoCambiado("ana@finca.co")

        assertFalse(viewModel.estado.value.puedeIniciar)
    }

    @Test
    fun credencialesValidas_inicianSesionProgramanLaSincronizacionYEmitenElEvento() = runTest {
        val viewModel = llenado()

        viewModel.onIniciarSesion()

        assertEquals(LoginEvento.SesionIniciada, viewModel.eventos.first())
        assertEquals(listOf("ana@finca.co" to "secreta"), sesionRepository.intentos)
        assertNull(viewModel.estado.value.error)
        assertEquals("trae lo que ya tiene la finca", 1, sincronizacionRepository.programaciones)
    }

    @Test
    fun credencialesIncorrectas_muestranErrorSinNavegar() {
        sesionRepository.resultado = ResultadoInicioSesion.CredencialesInvalidas
        val viewModel = llenado()

        viewModel.onIniciarSesion()

        assertEquals("Correo o contraseña incorrectos.", viewModel.estado.value.error)
        assertFalse(viewModel.estado.value.cargando)
        assertEquals(0, sincronizacionRepository.programaciones)
    }

    @Test
    fun sinSenal_avisaQueHaceFaltaConexion() {
        sesionRepository.resultado = ResultadoInicioSesion.SinConexion
        val viewModel = llenado()

        viewModel.onIniciarSesion()

        assertTrue(viewModel.estado.value.error!!.contains("conexión"))
    }

    @Test
    fun unErrorDelServidor_muestraUnMensajeGenerico() {
        sesionRepository.resultado = ResultadoInicioSesion.NoSePudo
        val viewModel = llenado()

        viewModel.onIniciarSesion()

        assertTrue(viewModel.estado.value.error!!.contains("No se pudo iniciar sesión"))
        assertFalse(viewModel.estado.value.cargando)
    }

    @Test
    fun siLaCuentaGuardadaYaNoTieneFinca_avisaQueSusDatosSeBorraron() {
        sesionRepository.resultado = ResultadoInicioSesion.CuentaQuitada
        val viewModel = llenado()

        viewModel.onIniciarSesion()

        assertTrue(viewModel.estado.value.error!!.contains("Borramos sus datos"))
        assertEquals(0, sincronizacionRepository.programaciones)
    }

    @Test
    fun siElCelularBorroUnaCuentaConLaSesionAbierta_laPantallaLoDiceUnaSolaVez() = runTest {
        sesionRepository.cerrarSesionYBorrarCuenta()

        val primera = crearViewModel()
        val segunda = crearViewModel()

        assertTrue(primera.estado.value.error!!.contains("Borramos sus datos"))
        assertNull("el aviso se da una sola vez", segunda.estado.value.error)
    }

    @Test
    fun lasCuentasGuardadasDelCelularSeMuestranEnLaPantalla() {
        val viewModel = crearViewModel()

        cuentasGuardadas.cuentas.value = listOf(cuenta())

        assertEquals(listOf(cuenta()), viewModel.estado.value.cuentasGuardadas)
    }

    @Test
    fun tocarUnaCuentaGuardada_rellenaElCorreoYPideLaContrasena() {
        val viewModel = crearViewModel().apply { onContrasenaCambiada("otra") }

        viewModel.onCuentaGuardadaClick(cuenta(correo = "luis@otra-finca.co"))

        assertEquals("luis@otra-finca.co", viewModel.estado.value.correo)
        assertEquals("la contraseña no se arrastra de otra cuenta", "", viewModel.estado.value.contrasena)
        assertFalse(viewModel.estado.value.puedeIniciar)
    }

    @Test
    fun unaCuentaSinCorreoConocido_noRellenaNada() {
        val viewModel = llenado()

        viewModel.onCuentaGuardadaClick(cuenta(correo = null))

        assertEquals("ana@finca.co", viewModel.estado.value.correo)
    }

    @Test
    fun quitarUnaCuentaSinNadaPorSubir_laBorraDelCelular() = runTest {
        cuentasGuardadas.cuentas.value = listOf(cuenta())
        val viewModel = crearViewModel()

        viewModel.onQuitarCuentaClick(cuenta())
        viewModel.onConfirmarQuitarCuenta()

        assertEquals(listOf("u-1"), cuentasGuardadas.quitadas)
        assertNull(viewModel.estado.value.quitarCuenta)
        assertTrue(viewModel.estado.value.cuentasGuardadas.isEmpty())
    }

    @Test
    fun conAlgoPorSubir_noSeOfreceQuitarla() = runTest {
        val conPendientes = listOf(
            cuenta(cambiosSinSubir = 1),
            cuenta(fotosSinSubir = 1),
            cuenta(rechazosAbiertos = 1),
        )
        conPendientes.forEach { pendiente ->
            cuentasGuardadas.cuentas.value = listOf(pendiente)
            val viewModel = crearViewModel()

            viewModel.onQuitarCuentaClick(pendiente)
            viewModel.onConfirmarQuitarCuenta()

            assertFalse(viewModel.estado.value.quitarCuenta!!.puedeQuitar)
            assertTrue(cuentasGuardadas.quitadas.isEmpty())
        }
    }

    @Test
    fun siLaCuentaTerminaDeSubirConElDialogoAbierto_yaSePuedeQuitar() {
        cuentasGuardadas.cuentas.value = listOf(cuenta(cambiosSinSubir = 2))
        val viewModel = crearViewModel()
        viewModel.onQuitarCuentaClick(cuenta(cambiosSinSubir = 2))

        cuentasGuardadas.cuentas.value = listOf(cuenta())

        assertTrue(viewModel.estado.value.quitarCuenta!!.puedeQuitar)
    }

    @Test
    fun cancelarElDialogoNoQuitaNada() {
        cuentasGuardadas.cuentas.value = listOf(cuenta())
        val viewModel = crearViewModel()
        viewModel.onQuitarCuentaClick(cuenta())

        viewModel.onCancelarQuitarCuenta()

        assertNull(viewModel.estado.value.quitarCuenta)
        assertTrue(cuentasGuardadas.quitadas.isEmpty())
    }

    @Test
    fun olvideMiContrasena_abreElDialogoConElCorreoQueYaEstabaEscrito() {
        val viewModel = llenado()

        viewModel.onOlvideContrasena()

        assertEquals("ana@finca.co", viewModel.estado.value.recuperacion?.correo)
    }

    @Test
    fun enviarLaRecuperacion_pideElCorreoSinEspaciosYConfirmaSinRevelarSiLaCuentaExiste() = runTest {
        val viewModel = crearViewModel()
        viewModel.onOlvideContrasena()
        viewModel.onRecuperacionCorreoCambiado("  ana@finca.co ")

        viewModel.onEnviarRecuperacion()

        assertEquals(listOf("ana@finca.co"), sesionRepository.recuperaciones)
        val recuperacion = viewModel.estado.value.recuperacion!!
        assertTrue(recuperacion.enviado)
        assertFalse(recuperacion.enviando)
        assertEquals(
            "Si el correo está registrado, te enviamos un enlace para recuperar la contraseña.",
            recuperacion.mensaje,
        )
    }

    @Test
    fun sinCorreoNoSeEnviaLaRecuperacion() {
        val viewModel = crearViewModel()
        viewModel.onOlvideContrasena()

        viewModel.onEnviarRecuperacion()

        assertTrue(sesionRepository.recuperaciones.isEmpty())
        assertFalse(viewModel.estado.value.recuperacion!!.puedeEnviar)
    }

    @Test
    fun sinSenal_laRecuperacionLoIndicaYNoDaPorEnviado() {
        sesionRepository.resultadoRecuperacion = ResultadoRecuperacion.SinConexion
        val viewModel = llenado()
        viewModel.onOlvideContrasena()

        viewModel.onEnviarRecuperacion()

        val recuperacion = viewModel.estado.value.recuperacion!!
        assertFalse(recuperacion.enviado)
        assertTrue(recuperacion.mensaje!!.contains("señal"))
    }

    @Test
    fun siElServidorFalla_laRecuperacionPermiteReintentar() {
        sesionRepository.resultadoRecuperacion = ResultadoRecuperacion.NoSePudo
        val viewModel = llenado()
        viewModel.onOlvideContrasena()

        viewModel.onEnviarRecuperacion()

        val recuperacion = viewModel.estado.value.recuperacion!!
        assertFalse(recuperacion.enviado)
        assertTrue(recuperacion.puedeEnviar)
        assertTrue(recuperacion.mensaje!!.contains("Inténtalo"))
    }

    @Test
    fun siSeCierraElDialogoConUnEnvioEnCursoSuResultadoNoLlegaAlDialogoNuevo() = runTest {
        val enCurso = CompletableDeferred<Unit>()
        sesionRepository.pausaRecuperacion = enCurso
        val viewModel = llenado()
        viewModel.onOlvideContrasena()
        viewModel.onEnviarRecuperacion()
        viewModel.onCerrarRecuperacion()
        viewModel.onOlvideContrasena()

        enCurso.complete(Unit)

        val recuperacion = viewModel.estado.value.recuperacion!!
        assertFalse(recuperacion.enviado)
        assertNull(recuperacion.mensaje)
    }

    @Test
    fun cerrarElDialogoLoDescarta() {
        val viewModel = llenado()
        viewModel.onOlvideContrasena()

        viewModel.onCerrarRecuperacion()

        assertNull(viewModel.estado.value.recuperacion)
    }
}
