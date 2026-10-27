package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.AccionSugerida
import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Chapeta
import com.devcris80.prototipo.domain.model.EntidadSincronizada
import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.MotivoRechazo
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.model.Rechazo
import com.devcris80.prototipo.domain.model.Sexo
import com.devcris80.prototipo.testing.FakeAnimalRepository
import com.devcris80.prototipo.testing.FakeChapetaRepository
import com.devcris80.prototipo.testing.FakeRechazoRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ObservarBandejaRechazosTest {
    private val rechazos = FakeRechazoRepository()
    private val animales = FakeAnimalRepository()
    private val chapetas = FakeChapetaRepository()
    private val observarBandeja = ObservarBandejaRechazos(rechazos, animales, chapetas)

    private fun animal(id: String, nombre: String) =
        Animal(id, "finca-1", nombre, "Brahman", Sexo.HEMBRA, Etapa.VACA, 0L, proposito = Proposito.LECHE)

    private fun rechazo(
        entidad: EntidadSincronizada,
        codigo: String,
        idAnimal: String? = null,
        codigoChapeta: String? = null,
        salioConSuAnimal: Boolean = false,
        idRechazo: String = "rechazo-1",
        idFila: String = "fila-1",
        nombreAnimal: String? = null,
    ) = Rechazo(
        idRechazo = idRechazo,
        entidad = entidad,
        idFila = idFila,
        contenido = "{}",
        motivo = "$codigo: mensaje del servidor",
        fechaRechazo = 1L,
        codigoError = codigo,
        mensaje = "mensaje del servidor",
        idAnimal = idAnimal,
        nombreAnimal = nombreAnimal,
        codigoChapeta = codigoChapeta,
        salioConSuAnimal = salioConSuAnimal,
    )

    private suspend fun unico() = observarBandeja().first().single()

    @Test
    fun enE07LaChapetaRechazadaTraeElNombreDelAnimalQueYaTieneElCodigo() = runTest {
        animales.activos.value = listOf(animal("lucero", "Lucero"), animal("tormenta", "Tormenta"))
        // La del otro celular, que bajó después del rechazo.
        chapetas.set(Chapeta("chapeta-otro", "04A31B2C", "tormenta", fechaAsociacion = 5L))
        rechazos.set(rechazo(EntidadSincronizada.CHAPETA, "23505", idAnimal = "lucero", codigoChapeta = "04A31B2C"))

        val enBandeja = unico()

        assertEquals("Lucero", enBandeja.nombreAnimal)
        assertEquals(MotivoRechazo.CodigoEnOtroAnimal(nombreOtroAnimal = "Tormenta"), enBandeja.motivo)
        assertEquals(AccionSugerida.ASOCIAR_OTRO_CODIGO, enBandeja.accion)
    }

    @Test
    fun siElCodigoQuedoEnEsteMismoAnimalSoloQuedaAceptarlo() = runTest {
        animales.activos.value = listOf(animal("lucero", "Lucero"))
        chapetas.set(Chapeta("chapeta-otro", "04A31B2C", "lucero", fechaAsociacion = 5L))
        rechazos.set(rechazo(EntidadSincronizada.CHAPETA, "23505", idAnimal = "lucero", codigoChapeta = "04A31B2C"))

        val enBandeja = unico()

        assertEquals(MotivoRechazo.CodigoYaEsDeEsteAnimal, enBandeja.motivo)
        assertEquals(AccionSugerida.ACEPTAR_LA_DEL_SERVIDOR, enBandeja.accion)
    }

    @Test
    fun elRegistroDeUnPesajeQueOtroCelularYaConcilioSeAceptaComoQuedoEnElServidor() = runTest {
        // Su lectura ya tiene un Registro en la finca (RN-09, RN-21).
        rechazos.set(rechazo(EntidadSincronizada.REGISTRO, "23505", idAnimal = "lucero"))

        val enBandeja = unico()

        assertEquals(MotivoRechazo.YaConciliado, enBandeja.motivo)
        assertEquals(AccionSugerida.ACEPTAR_LA_DEL_SERVIDOR, enBandeja.accion)
    }

    @Test
    fun unAnimalRechazadoQueSalioDelCelularConservaSuNombreEnSusFilas() = runTest {
        rechazos.set(rechazo(EntidadSincronizada.ANIMAL, "42501", idAnimal = "lucero", idFila = "lucero", nombreAnimal = "Lucero"))
        rechazos.set(
            rechazo(EntidadSincronizada.REGISTRO, "42501", idAnimal = "lucero", salioConSuAnimal = true, idRechazo = "rechazo-2"),
        )

        val bandeja = observarBandeja().first()

        assertEquals(listOf("Lucero", "Lucero"), bandeja.map { it.nombreAnimal })
        assertEquals("no se puede abrir su detalle", listOf(false, false), bandeja.map { it.animalEnElCelular })
    }

    @Test
    fun unAnimalDadoDeBajaConservaSuNombre() = runTest {
        animales.set(animal("lucero", "Lucero").copy(fechaBaja = 9L))
        rechazos.set(rechazo(EntidadSincronizada.EVENTO, "42501", idAnimal = "lucero"))

        val enBandeja = unico()

        assertEquals("Lucero", enBandeja.nombreAnimal)
        assertTrue(enBandeja.animalEnElCelular)
    }

    @Test
    fun unPesajePendienteYaConciliadoEnOtroCelularSeAceptaComoQuedoEnElServidor() = runTest {
        rechazos.set(rechazo(EntidadSincronizada.PESAJE_PENDIENTE, "55000"))

        val enBandeja = unico()

        assertEquals(MotivoRechazo.YaConciliado, enBandeja.motivo)
        assertEquals(AccionSugerida.ACEPTAR_LA_DEL_SERVIDOR, enBandeja.accion)
    }

    @Test
    fun unPermisoDenegadoSeCierraSinReenviar() = runTest {
        animales.activos.value = listOf(animal("lucero", "Lucero"))
        rechazos.set(rechazo(EntidadSincronizada.EVENTO, "42501", idAnimal = "lucero"))

        val enBandeja = unico()

        assertEquals(MotivoRechazo.SinPermiso, enBandeja.motivo)
        assertEquals(AccionSugerida.CERRAR, enBandeja.accion)
        assertEquals("Lucero", enBandeja.nombreAnimal)
    }

    @Test
    fun unaFilaQueSalioConSuAnimalLoDice() = runTest {
        rechazos.set(rechazo(EntidadSincronizada.REGISTRO, "42501", idAnimal = "lucero", salioConSuAnimal = true))

        val enBandeja = unico()

        assertEquals(MotivoRechazo.SeRechazoSuAnimal, enBandeja.motivo)
        assertEquals(AccionSugerida.CERRAR, enBandeja.accion)
        assertNull("el Animal salió del celular", enBandeja.nombreAnimal)
    }

    @Test
    fun unaFotoQueStorageNoAceptaSeCierra() = runTest {
        animales.activos.value = listOf(animal("lucero", "Lucero"))
        rechazos.set(rechazo(EntidadSincronizada.FOTO, "413", idAnimal = "lucero"))

        val enBandeja = unico()

        assertEquals(MotivoRechazo.FotoNoAceptada, enBandeja.motivo)
        assertEquals(AccionSugerida.CERRAR, enBandeja.accion)
    }

    @Test
    fun otroErrorMuestraElMensajeDelServidor() = runTest {
        rechazos.set(rechazo(EntidadSincronizada.ANIMAL, "23514"))

        assertEquals(MotivoRechazo.Otro("mensaje del servidor"), unico().motivo)
    }

    @Test
    fun unRechazoCerradoSaleDeLaBandeja() = runTest {
        rechazos.set(rechazo(EntidadSincronizada.PESAJE_PENDIENTE, "55000"))

        rechazos.cerrar("rechazo-1")

        assertEquals(emptyList<Any>(), observarBandeja().first())
        assertEquals(listOf("rechazo-1"), rechazos.cerrados)
    }
}
