package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Chapeta
import com.devcris80.prototipo.domain.model.Conciliacion
import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.MotivoPesajePendiente
import com.devcris80.prototipo.domain.model.Pesaje
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.model.Registro
import com.devcris80.prototipo.domain.model.ResolucionPesajePendiente
import com.devcris80.prototipo.domain.model.Rol
import com.devcris80.prototipo.domain.model.Sexo
import com.devcris80.prototipo.domain.model.Usuario
import com.devcris80.prototipo.domain.usecase.RegistrarPesaje.MotivoInvalido
import com.devcris80.prototipo.domain.usecase.RegistrarPesaje.Resultado
import com.devcris80.prototipo.testing.FakeAnimalRepository
import com.devcris80.prototipo.testing.FakeChapetaRepository
import com.devcris80.prototipo.testing.FakeCuentaRepository
import com.devcris80.prototipo.testing.FakePesajePendienteRepository
import com.devcris80.prototipo.testing.FakeRegistroRepository
import com.devcris80.prototipo.testing.FakeTransaccion
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegistrarPesajeTest {
    private val chapetaRepository = FakeChapetaRepository()
    private val animalRepository = FakeAnimalRepository()
    private val registroRepository = FakeRegistroRepository()
    private val pesajePendienteRepository = FakePesajePendienteRepository()
    private val cuentaRepository = FakeCuentaRepository()
    private val registrarPesaje = RegistrarPesaje(
        chapetaRepository,
        animalRepository,
        registroRepository,
        pesajePendienteRepository,
        cuentaRepository,
        FakeTransaccion(),
        clock = { RECIBIDO_EN },
    )

    private fun conUsuario() {
        cuentaRepository.usuario.value = Usuario("usuario-1", "perfil_local", "Ana", Rol.GANADERO)
    }

    private fun conAnimalConChapeta(codigo: String = "04A1B2C3", fechaBaja: Long? = null) {
        animalRepository.set(
            Animal(
                idAnimal = "animal-1",
                idPerfilFinca = "perfil_local",
                nombre = "Manchas",
                raza = "Holstein",
                sexo = Sexo.HEMBRA,
                etapa = Etapa.VACA,
                fechaNacimiento = 1L,
                proposito = Proposito.LECHE,
                fechaBaja = fechaBaja,
            ),
        )
        chapetaRepository.set(Chapeta("chapeta-1", codigo, "animal-1", fechaAsociacion = 1L))
    }

    private fun pesaje(
        idLectura: String = "lectura-1",
        codigo: String = "04A1B2C3",
        peso: Float = 450.5f,
        antiguedadMs: Long = 0,
    ) = Pesaje(idLectura, codigo, peso, antiguedadMs)

    private suspend fun registros() = registroRepository.observeByAnimal("animal-1").first()

    @Test
    fun unCodigoConChapetaActivaDeUnAnimalActivoQuedaComoRegistroAnombreDelUsuario() = runTest {
        conUsuario()
        conAnimalConChapeta()

        val resultado = registrarPesaje(pesaje())

        val registro = registros().single()
        assertEquals(Resultado.Registrado(registro.idRegistro), resultado)
        assertEquals("animal-1", registro.idAnimal)
        assertEquals("usuario-1", registro.idUsuario)
        assertEquals("lectura-1", registro.idLectura)
        assertEquals(450.5f, registro.peso)
    }

    @Test
    fun laHoraGuardadaEsLaDeRecepcionMenosLaAntiguedadQueReportaElEsp32() = runTest {
        conUsuario()
        conAnimalConChapeta()

        registrarPesaje(pesaje(antiguedadMs = 1_200))

        assertEquals(RECIBIDO_EN - 1_200, registros().single().timestamp)
    }

    @Test
    fun elCodigoSeNormalizaAntesDeBuscarLaChapeta() = runTest {
        conUsuario()
        conAnimalConChapeta(codigo = "04A1B2C3")

        val resultado = registrarPesaje(pesaje(codigo = " 04:a1-b2 c3 "))

        assertTrue(resultado is Resultado.Registrado)
    }

    @Test
    fun reenviarElMismoPesajeNoCreaNadaNuevoYDevuelveElMismoRegistro() = runTest {
        conUsuario()
        conAnimalConChapeta()
        val primero = registrarPesaje(pesaje())

        val reintento = registrarPesaje(pesaje(codigo = "OTRO", peso = 100f))

        assertEquals(primero, reintento)
        assertEquals(1, registros().size)
        assertTrue(pesajePendienteRepository.todos.value.isEmpty())
    }

    @Test
    fun uncodigoSinChapetaActivaQuedaComoPesajePendienteYNoCreaAnimalesNiChapetas() = runTest {
        conUsuario()

        val resultado = registrarPesaje(pesaje(codigo = "FFEEDD00"))

        val pendiente = pesajePendienteRepository.todos.value.single()
        assertEquals(Resultado.Pendiente(pendiente.idPesajePendiente, MotivoPesajePendiente.CODIGO_SIN_CHAPETA_ACTIVA), resultado)
        assertEquals("FFEEDD00", pendiente.codigo)
        assertEquals("usuario-1", pendiente.idUsuario)
        assertTrue(chapetaRepository.observeActivas().first().isEmpty())
        assertTrue(animalRepository.activos.value.isEmpty())
    }

    @Test
    fun laHoraDeUnPesajePendienteTambienEsLaDeRecepcionMenosLaAntiguedad() = runTest {
        conUsuario()

        registrarPesaje(pesaje(codigo = "FFEEDD00", antiguedadMs = 1_200))

        assertEquals(RECIBIDO_EN - 1_200, pesajePendienteRepository.todos.value.single().timestamp)
    }

    @Test
    fun reenviarUnPesajePendienteSinConciliarDevuelveElMismoPendienteYNoCreaOtro() = runTest {
        conUsuario()
        val primero = registrarPesaje(pesaje(codigo = "FFEEDD00"))

        val reintento = registrarPesaje(pesaje(codigo = "OTRO", peso = 100f))

        assertEquals(primero, reintento)
        assertEquals(1, pesajePendienteRepository.todos.value.size)
    }

    @Test
    fun laChapetaDeUnAnimalDadoDeBajaQuedaComoPendienteConEseMotivo() = runTest {
        conUsuario()
        conAnimalConChapeta(fechaBaja = 5L)

        val resultado = registrarPesaje(pesaje())

        assertEquals(
            MotivoPesajePendiente.ANIMAL_DADO_DE_BAJA,
            (resultado as Resultado.Pendiente).motivo,
        )
        assertTrue(registros().isEmpty())
    }

    @Test
    fun elReintentoDeUnPesajePendienteDevuelveElMismoPendienteAunqueYaSeHayaConciliado() = runTest {
        conUsuario()
        val primero = registrarPesaje(pesaje(codigo = "FFEEDD00"))
        val pendiente = pesajePendienteRepository.todos.value.single()
        // Alguien lo asigna a un Animal: además del Registro, el pendiente queda conciliado.
        conAnimalConChapeta(codigo = "FFEEDD00")
        registroRepository.insert(Registro("registro-1", "animal-1", "usuario-1", "lectura-1", 450.5f, 1L))
        pesajePendienteRepository.conciliar(
            pendiente.idPesajePendiente,
            Conciliacion(ResolucionPesajePendiente.ASIGNADO, "usuario-1", 9L),
        )

        val reintento = registrarPesaje(pesaje(codigo = "FFEEDD00"))

        assertEquals(primero, reintento)
        assertEquals(1, pesajePendienteRepository.todos.value.size)
    }

    @Test
    fun unPesoFueraDeRangoEsInvalidoYNoGuardaNada() = runTest {
        conUsuario()
        conAnimalConChapeta()

        for (peso in listOf(0f, -1f, 1500.1f, Float.NaN)) {
            assertEquals(Resultado.Invalido(MotivoInvalido.PESO_FUERA_DE_RANGO), registrarPesaje(pesaje(peso = peso)))
        }

        assertTrue(registros().isEmpty())
        assertTrue(pesajePendienteRepository.todos.value.isEmpty())
    }

    @Test
    fun elPesoMaximoSiEsValido() = runTest {
        conUsuario()
        conAnimalConChapeta()

        assertTrue(registrarPesaje(pesaje(peso = Pesaje.PESO_MAXIMO_KG)) is Resultado.Registrado)
    }

    @Test
    fun unaAntiguedadNegativaOMayorQueLaHoraDeRecepcionEsInvalida() = runTest {
        conUsuario()
        conAnimalConChapeta()

        assertEquals(Resultado.Invalido(MotivoInvalido.ANTIGUEDAD_FUERA_DE_RANGO), registrarPesaje(pesaje(antiguedadMs = -1)))
        assertEquals(
            Resultado.Invalido(MotivoInvalido.ANTIGUEDAD_FUERA_DE_RANGO),
            registrarPesaje(pesaje(antiguedadMs = RECIBIDO_EN + 1)),
        )
        assertTrue(registros().isEmpty())
    }

    @Test
    fun sinUsuarioEnElDispositivoNoSeGuardaNada() = runTest {
        conAnimalConChapeta()

        assertEquals(Resultado.SinUsuario, registrarPesaje(pesaje()))

        assertTrue(registros().isEmpty())
        assertTrue(pesajePendienteRepository.todos.value.isEmpty())
    }

    private companion object {
        const val RECIBIDO_EN = 1_000_000L
    }
}
