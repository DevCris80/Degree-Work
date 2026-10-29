package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.AutorizacionDatos
import com.devcris80.prototipo.domain.model.CuentaNueva
import com.devcris80.prototipo.domain.model.FaltaParaCrear
import com.devcris80.prototipo.domain.repository.ResultadoCreacionCuenta
import com.devcris80.prototipo.testing.FakeSesionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CrearCuentaTest {
    private val sesionRepository = FakeSesionRepository()
    private val crearCuenta = CrearCuenta(sesionRepository)

    private fun cuenta(
        nombre: String = "Ana",
        nombreFinca: String = "La Esperanza",
        correo: String = "ana@finca.co",
        contrasena: String = "secreta",
        confirmacion: String = contrasena,
        aceptaAutorizacion: Boolean = true,
        conCodigo: Boolean = false,
        codigoInvitacion: String = "",
    ) = CuentaNueva(
        nombre, nombreFinca, correo, contrasena, confirmacion, aceptaAutorizacion, conCodigo, codigoInvitacion,
    )

    @Test
    fun conCodigoSinElCodigoNoLlegaAlServidorAunqueFalteElNombreDeFinca() = runTest {
        val sinCodigo = cuenta(nombreFinca = "", conCodigo = true, codigoInvitacion = "  ")

        assertEquals(CrearCuenta.Resultado.Incompleta(FaltaParaCrear.DATOS), crearCuenta(sinCodigo))
        assertTrue(sesionRepository.uniones.isEmpty())
    }

    @Test
    fun conCodigoNoPideElNombreDeLaFinca() = runTest {
        val resultado = crearCuenta(cuenta(nombreFinca = "", conCodigo = true, codigoInvitacion = "ABCD2345"))

        assertEquals(CrearCuenta.Resultado.DelServidor(ResultadoCreacionCuenta.Exito), resultado)
    }

    @Test
    fun conCodigoSinAceptarLaAutorizacionNoLlegaAlServidor() = runTest {
        val resultado = crearCuenta(cuenta(aceptaAutorizacion = false, conCodigo = true, codigoInvitacion = "ABCD2345"))

        assertEquals(CrearCuenta.Resultado.Incompleta(FaltaParaCrear.AUTORIZACION), resultado)
        assertTrue(sesionRepository.uniones.isEmpty())
    }

    @Test
    fun conCodigoPideUnirseALaFincaYNoCrearOtra() = runTest {
        crearCuenta(
            cuenta(nombre = " Luis ", correo = " luis@finca.co ", conCodigo = true, codigoInvitacion = " abcd2345 "),
        )

        assertEquals(
            FakeSesionRepository.UnionAFinca("Luis", "abcd2345", "luis@finca.co", "secreta", AutorizacionDatos.VERSION),
            sesionRepository.uniones.single(),
        )
        assertTrue(sesionRepository.creaciones.isEmpty())
    }

    @Test
    fun conCodigoDevuelveLoQueRespondeElServidor() = runTest {
        sesionRepository.resultadoCreacion = ResultadoCreacionCuenta.CodigoRechazado

        assertEquals(
            CrearCuenta.Resultado.DelServidor(ResultadoCreacionCuenta.CodigoRechazado),
            crearCuenta(cuenta(conCodigo = true, codigoInvitacion = "ABCD2345")),
        )
    }

    @Test
    fun sinAceptarLaAutorizacionNoSeCreaNadaNiSeLlamaAlServidor() = runTest {
        val resultado = crearCuenta(cuenta(aceptaAutorizacion = false))

        assertEquals(CrearCuenta.Resultado.Incompleta(FaltaParaCrear.AUTORIZACION), resultado)
        assertTrue(sesionRepository.creaciones.isEmpty())
    }

    @Test
    fun unaContrasenaDeMenosDe6CaracteresOUnaConfirmacionDistintaNoLleganAlServidor() = runTest {
        assertEquals(
            CrearCuenta.Resultado.Incompleta(FaltaParaCrear.CONTRASENA_CORTA),
            crearCuenta(cuenta(contrasena = "12345")),
        )
        assertEquals(
            CrearCuenta.Resultado.Incompleta(FaltaParaCrear.CONFIRMACION_DISTINTA),
            crearCuenta(cuenta(contrasena = "secreta", confirmacion = "secretA")),
        )
        assertTrue(sesionRepository.creaciones.isEmpty())
    }

    @Test
    fun sinNombreFincaOCorreoNoLlegaAlServidor() = runTest {
        for (incompleta in listOf(cuenta(nombre = " "), cuenta(nombreFinca = ""), cuenta(correo = "  "))) {
            assertEquals(CrearCuenta.Resultado.Incompleta(FaltaParaCrear.DATOS), crearCuenta(incompleta))
        }
        assertTrue(sesionRepository.creaciones.isEmpty())
    }

    @Test
    fun conTodoAceptadoPideCrearLaCuentaConLaVersionDelTextoYLosDatosSinEspaciosDeMas() = runTest {
        val resultado = crearCuenta(cuenta(nombre = " Ana ", nombreFinca = " La Esperanza ", correo = " ana@finca.co "))

        assertEquals(CrearCuenta.Resultado.DelServidor(ResultadoCreacionCuenta.Exito), resultado)
        assertEquals(
            FakeSesionRepository.CreacionDeCuenta("Ana", "La Esperanza", "ana@finca.co", "secreta", AutorizacionDatos.VERSION),
            sesionRepository.creaciones.single(),
        )
    }

    @Test
    fun laContrasenaLlegaTalCualAunqueTengaEspacios() = runTest {
        crearCuenta(cuenta(contrasena = " con espacio "))

        assertEquals(" con espacio ", sesionRepository.creaciones.single().contrasena)
    }

    @Test
    fun devuelveLoQueRespondeElServidor() = runTest {
        sesionRepository.resultadoCreacion = ResultadoCreacionCuenta.CorreoYaRegistrado

        assertEquals(
            CrearCuenta.Resultado.DelServidor(ResultadoCreacionCuenta.CorreoYaRegistrado),
            crearCuenta(cuenta()),
        )
    }
}
