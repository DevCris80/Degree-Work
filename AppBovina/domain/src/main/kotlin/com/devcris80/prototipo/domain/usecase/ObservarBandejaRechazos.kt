package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.AccionSugerida
import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Chapeta
import com.devcris80.prototipo.domain.model.EntidadSincronizada
import com.devcris80.prototipo.domain.model.MotivoRechazo
import com.devcris80.prototipo.domain.model.Rechazo
import com.devcris80.prototipo.domain.model.RechazoEnBandeja
import com.devcris80.prototipo.domain.repository.AnimalRepository
import com.devcris80.prototipo.domain.repository.ChapetaRepository
import com.devcris80.prototipo.domain.repository.RechazoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * La bandeja de rechazos (CU-22, RN-24): los Rechazos abiertos, del más reciente al más antiguo, cada uno con su
 * motivo y la acción sugerida según la entidad y el código de error del servidor (supabase/README.md). Se actualiza
 * cuando llegan Animales o Chapetas: en E-07, la Chapeta del otro celular baja después del rechazo.
 */
class ObservarBandejaRechazos(
    private val rechazoRepository: RechazoRepository,
    private val animalRepository: AnimalRepository,
    private val chapetaRepository: ChapetaRepository,
) {
    operator fun invoke(): Flow<List<RechazoEnBandeja>> = combine(
        rechazoRepository.observeAbiertos(),
        animalRepository.observeActivos(),
        chapetaRepository.observeActivas(),
    ) { rechazos, animales, chapetas ->
        val nombres = animales.associate { it.idAnimal to it.nombre }
        rechazos.map { it.enBandeja(nombres, chapetas) }
    }

    private fun Rechazo.enBandeja(nombres: Map<String, String>, chapetas: List<Chapeta>): RechazoEnBandeja {
        val motivo = motivo(nombres, chapetas)
        return RechazoEnBandeja(
            rechazo = this,
            nombreAnimal = idAnimal?.let(nombres::get),
            motivo = motivo,
            accion = when (motivo) {
                is MotivoRechazo.CodigoEnOtroAnimal -> AccionSugerida.ASOCIAR_OTRO_CODIGO
                MotivoRechazo.YaConciliado -> AccionSugerida.ACEPTAR_LA_DEL_SERVIDOR
                else -> AccionSugerida.CERRAR
            },
        )
    }

    private fun Rechazo.motivo(nombres: Map<String, String>, chapetas: List<Chapeta>): MotivoRechazo = when {
        porSuAnimal -> MotivoRechazo.SeRechazoSuAnimal
        entidad == EntidadSincronizada.CHAPETA && codigo == UNICIDAD -> {
            val otra = chapetas.firstOrNull { it.codigo == codigoChapeta && it.idAnimal != idAnimal }
            MotivoRechazo.CodigoEnOtroAnimal(otra?.idAnimal?.let(nombres::get))
        }
        entidad == EntidadSincronizada.PESAJE_PENDIENTE && codigo == ESTADO_DEFINITIVO -> MotivoRechazo.YaConciliado
        entidad == EntidadSincronizada.FOTO -> if (codigo == "403") MotivoRechazo.SinPermiso else MotivoRechazo.FotoNoAceptada
        codigo == PERMISO -> MotivoRechazo.SinPermiso
        else -> MotivoRechazo.Otro(motivo.substringAfter(": "))
    }

    private companion object {
        const val UNICIDAD = "23505"
        const val ESTADO_DEFINITIVO = "55000"
        const val PERMISO = "42501"
    }
}
