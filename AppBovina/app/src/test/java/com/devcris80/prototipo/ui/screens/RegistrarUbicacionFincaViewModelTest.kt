package com.devcris80.prototipo.ui.screens

import com.devcris80.prototipo.domain.model.PerfilFinca
import com.devcris80.prototipo.testing.FakeCuentaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RegistrarUbicacionFincaViewModelTest {
    private val cuentaRepository = FakeCuentaRepository()

    private fun crearViewModel(perfil: PerfilFinca? = PerfilFinca("finca-1", "La Esperanza")): RegistrarUbicacionFincaViewModel {
        cuentaRepository.perfil.value = perfil
        return RegistrarUbicacionFincaViewModel(cuentaRepository)
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun alAbrirMuestraLaFincaYLasCoordenadasQueYaTiene() {
        val viewModel = crearViewModel(PerfilFinca("finca-1", "La Esperanza", latitud = 4.6097, longitud = -74.0817))

        val estado = viewModel.estado.value
        assertEquals("La Esperanza", estado.nombreFinca)
        assertEquals("4.6097", estado.latitud)
        assertEquals("-74.0817", estado.longitud)
    }

    @Test
    fun unaFincaSinUbicacionEmpiezaConLosCamposVacios() {
        val estado = crearViewModel().estado.value

        assertEquals("", estado.latitud)
        assertEquals("", estado.longitud)
    }

    @Test
    fun guardarConCoordenadasValidasLasDejaEnLaFincaYConservaSusDemasDatos() {
        val viewModel = crearViewModel()
        viewModel.onLatitudCambiada("4.6097")
        viewModel.onLongitudCambiada("-74.0817")

        viewModel.onGuardarUbicacion()

        assertEquals(listOf(PerfilFinca("finca-1", "La Esperanza", latitud = 4.6097, longitud = -74.0817)), cuentaRepository.guardadas)
        assertTrue(viewModel.estado.value.guardado)
        assertFalse(viewModel.estado.value.cargando)
        assertNull(viewModel.estado.value.error)
    }

    @Test
    fun laComaDecimalDelTecladoSeAcepta() {
        val viewModel = crearViewModel()
        viewModel.onLatitudCambiada("4,6097")
        viewModel.onLongitudCambiada(" -74,0817 ")

        viewModel.onGuardarUbicacion()

        assertEquals(4.6097, cuentaRepository.guardadas.single().latitud!!, 0.0)
        assertEquals(-74.0817, cuentaRepository.guardadas.single().longitud!!, 0.0)
    }

    @Test
    fun losExtremosDelGloboSonCoordenadasValidas() {
        val viewModel = crearViewModel()
        viewModel.onLatitudCambiada("-90")
        viewModel.onLongitudCambiada("180")

        viewModel.onGuardarUbicacion()

        assertEquals(PerfilFinca("finca-1", "La Esperanza", latitud = -90.0, longitud = 180.0), cuentaRepository.guardadas.single())
    }

    @Test
    fun sinLatitudOSinLongitudNoGuardaYAvisa() {
        val viewModel = crearViewModel()
        viewModel.onLatitudCambiada("4.6")

        viewModel.onGuardarUbicacion()

        assertEquals("Ingresa la latitud y la longitud.", viewModel.estado.value.error)
        assertTrue(cuentaRepository.guardadas.isEmpty())
        assertFalse(viewModel.estado.value.guardado)
    }

    @Test
    fun unaCoordenadaQueNoEsUnNumeroNoGuarda() {
        for (invalida in listOf("abc", "4.6.1", "NaN", "Infinity")) {
            val viewModel = crearViewModel()
            viewModel.onLatitudCambiada(invalida)
            viewModel.onLongitudCambiada("-74.0")

            viewModel.onGuardarUbicacion()

            assertNotNull("$invalida debió dar error", viewModel.estado.value.error)
            assertTrue("$invalida no debió guardarse", cuentaRepository.guardadas.isEmpty())
        }
    }

    @Test
    fun unaLatitudFueraDeRangoNoGuarda() {
        val viewModel = crearViewModel()
        viewModel.onLatitudCambiada("90.1")
        viewModel.onLongitudCambiada("0")

        viewModel.onGuardarUbicacion()

        assertEquals("La latitud debe estar entre -90 y 90.", viewModel.estado.value.error)
        assertTrue(cuentaRepository.guardadas.isEmpty())
    }

    @Test
    fun unaLongitudFueraDeRangoNoGuarda() {
        val viewModel = crearViewModel()
        viewModel.onLatitudCambiada("0")
        viewModel.onLongitudCambiada("-180.5")

        viewModel.onGuardarUbicacion()

        assertEquals("La longitud debe estar entre -180 y 180.", viewModel.estado.value.error)
        assertTrue(cuentaRepository.guardadas.isEmpty())
    }

    @Test
    fun sinPerfilDeFincaNoGuardaYAvisa() {
        val viewModel = crearViewModel(perfil = null)
        viewModel.onLatitudCambiada("4.6")
        viewModel.onLongitudCambiada("-74.0")

        viewModel.onGuardarUbicacion()

        assertEquals("No hay un perfil de finca guardado.", viewModel.estado.value.error)
        assertFalse(viewModel.estado.value.cargando)
        assertFalse(viewModel.estado.value.guardado)
        assertTrue(cuentaRepository.guardadas.isEmpty())
    }

    @Test
    fun laPosicionDelGpsLlenaLosCamposYSePuedeGuardar() {
        val viewModel = crearViewModel()
        viewModel.onBuscandoUbicacion()
        assertTrue(viewModel.estado.value.buscandoUbicacion)

        viewModel.onUbicacionDelGps(4.6097, -74.0817)

        assertFalse(viewModel.estado.value.buscandoUbicacion)
        assertEquals("4.6097", viewModel.estado.value.latitud)
        assertEquals("-74.0817", viewModel.estado.value.longitud)

        viewModel.onGuardarUbicacion()

        assertEquals(PerfilFinca("finca-1", "La Esperanza", latitud = 4.6097, longitud = -74.0817), cuentaRepository.guardadas.single())
    }

    @Test
    fun conElPermisoNegadoAvisaYDejaIngresarLasCoordenadasAMano() {
        val viewModel = crearViewModel()
        viewModel.onBuscandoUbicacion()

        viewModel.onPermisoNegado()

        assertFalse(viewModel.estado.value.buscandoUbicacion)
        assertEquals("Sin permiso de ubicación. Ingresa las coordenadas a mano.", viewModel.estado.value.error)

        viewModel.onLatitudCambiada("4.6097")
        viewModel.onLongitudCambiada("-74.0817")
        assertNull("escribir quita el aviso", viewModel.estado.value.error)

        viewModel.onGuardarUbicacion()

        assertEquals(4.6097, cuentaRepository.guardadas.single().latitud!!, 0.0)
    }

    @Test
    fun siElGpsNoRespondeAvisaYConservaLoQueHabiaEscrito() {
        val viewModel = crearViewModel(PerfilFinca("finca-1", "La Esperanza", latitud = 4.6, longitud = -74.0))
        viewModel.onBuscandoUbicacion()

        viewModel.onUbicacionNoDisponible()

        val estado = viewModel.estado.value
        assertFalse(estado.buscandoUbicacion)
        assertNotNull(estado.error)
        assertEquals("4.6", estado.latitud)
        assertEquals("-74.0", estado.longitud)
    }
}
