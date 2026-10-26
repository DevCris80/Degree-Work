package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Chapeta
import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.MotivoPesajePendiente
import com.devcris80.prototipo.domain.model.PesajePendiente
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.model.Sexo
import com.devcris80.prototipo.testing.FakeAnimalRepository
import com.devcris80.prototipo.testing.FakeChapetaRepository
import com.devcris80.prototipo.testing.FakePesajePendienteRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObservarPesajesPendientesTest {
    private val pesajePendienteRepository = FakePesajePendienteRepository()
    private val chapetaRepository = FakeChapetaRepository()
    private val animalRepository = FakeAnimalRepository()
    private val observarPesajesPendientes =
        ObservarPesajesPendientes(pesajePendienteRepository, chapetaRepository, animalRepository)

    private fun animal(id: String, fechaBaja: Long? = null) = Animal(
        id, "perfil_local", "Animal $id", "Holstein", Sexo.HEMBRA, Etapa.VACA, 1L,
        proposito = Proposito.LECHE, fechaBaja = fechaBaja,
    )

    private fun pendiente(id: String, codigo: String, timestamp: Long, motivo: MotivoPesajePendiente) =
        PesajePendiente(id, "lectura-$id", codigo, 100f, timestamp, "usuario-1", motivo)
            .also(pesajePendienteRepository::set)

    @Test
    fun elEstadoDelCodigoSaleDeLaChapetaDeHoyYNoDelMotivoConQueLlego() = runTest {
        val activo = animal("activo")
        val dadoDeBaja = animal("baja", fechaBaja = 5L)
        animalRepository.activos.value = listOf(activo)
        chapetaRepository.set(Chapeta("c-activo", "AAAA", "activo", fechaAsociacion = 1L))
        chapetaRepository.set(Chapeta("c-baja", "BBBB", dadoDeBaja.idAnimal, fechaAsociacion = 1L))
        // Llegó sin chapeta, pero hoy el código ya la tiene: manda el estado actual.
        pendiente("p1", "AAAA", timestamp = 3L, motivo = MotivoPesajePendiente.CODIGO_SIN_CHAPETA_ACTIVA)
        pendiente("p2", "BBBB", timestamp = 2L, motivo = MotivoPesajePendiente.ANIMAL_DADO_DE_BAJA)
        pendiente("p3", "CCCC", timestamp = 1L, motivo = MotivoPesajePendiente.CODIGO_SIN_CHAPETA_ACTIVA)

        val lista = observarPesajesPendientes().first()

        assertEquals(listOf("p1", "p2", "p3"), lista.map { it.pesajePendiente.idPesajePendiente })
        assertEquals(EstadoCodigo.ChapetaDeAnimalActivo(activo), lista[0].estadoCodigo)
        assertEquals(EstadoCodigo.ChapetaDeAnimalDadoDeBaja, lista[1].estadoCodigo)
        assertEquals(EstadoCodigo.SinChapetaActiva, lista[2].estadoCodigo)
    }

    @Test
    fun losPendientesYaResueltosNoAparecen() = runTest {
        pendiente("p1", "AAAA", timestamp = 1L, motivo = MotivoPesajePendiente.CODIGO_SIN_CHAPETA_ACTIVA).also {
            pesajePendienteRepository.conciliar(
                "p1",
                com.devcris80.prototipo.domain.model.Conciliacion(
                    com.devcris80.prototipo.domain.model.ResolucionPesajePendiente.DESCARTADO,
                    "usuario-1",
                    2L,
                ),
            )
        }

        assertEquals(emptyList<PesajePendientePorConciliar>(), observarPesajesPendientes().first())
    }
}
