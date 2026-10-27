package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.AccionSugerida
import com.devcris80.prototipo.domain.model.Chapeta
import com.devcris80.prototipo.domain.model.EntidadSincronizada
import com.devcris80.prototipo.domain.model.MotivoRechazo
import com.devcris80.prototipo.domain.model.Rechazo
import com.devcris80.prototipo.domain.model.RechazoEnBandeja
import com.devcris80.prototipo.domain.model.Sexo
import com.devcris80.prototipo.domain.repository.AnimalRepository
import com.devcris80.prototipo.domain.repository.ChapetaRepository
import com.devcris80.prototipo.domain.repository.RechazoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * La bandeja de rechazos (CU-22, RN-24): los Rechazos abiertos, del más reciente al más antiguo, cada uno con su
 * motivo y la acción sugerida según la entidad y el código de error del servidor (supabase/README.md). Se actualiza
 * cuando llegan Animales o Chapetas: en E-07, la Chapeta del otro celular baja después del rechazo.
 *
 * El nombre de un Animal sale de los del celular, también los dados de baja, o de su propio Rechazo si salió del
 * celular con él (ADR-0004, regla 7).
 */
class ObservarBandejaRechazos(
    private val rechazoRepository: RechazoRepository,
    private val animalRepository: AnimalRepository,
    private val chapetaRepository: ChapetaRepository,
) {
    operator fun invoke(): Flow<List<RechazoEnBandeja>> = combine(
        rechazoRepository.observeAbiertos(),
        animalRepository.observePorSexo(Sexo.MACHO),
        animalRepository.observePorSexo(Sexo.HEMBRA),
        chapetaRepository.observeActivas(),
    ) { rechazos, machos, hembras, chapetas ->
        val rechazados = rechazos.filter { it.entidad == EntidadSincronizada.ANIMAL }
            .mapNotNull { rechazo -> rechazo.nombreAnimal?.let { rechazo.idFila to it } }
        val nombres = rechazados.toMap() + (machos + hembras).associate { it.idAnimal to it.nombre }
        val enElCelular = (machos + hembras).map { it.idAnimal }.toSet()
        rechazos.map { it.enBandeja(nombres, enElCelular, chapetas) }
    }

    private fun Rechazo.enBandeja(
        nombres: Map<String, String>,
        enElCelular: Set<String>,
        chapetas: List<Chapeta>,
    ): RechazoEnBandeja {
        val motivo = motivo(nombres, chapetas)
        return RechazoEnBandeja(
            rechazo = this,
            nombreAnimal = idAnimal?.let(nombres::get),
            animalEnElCelular = idAnimal in enElCelular,
            motivo = motivo,
            accion = when (motivo) {
                is MotivoRechazo.CodigoEnOtroAnimal -> AccionSugerida.ASOCIAR_OTRO_CODIGO
                MotivoRechazo.CodigoYaEsDeEsteAnimal, MotivoRechazo.YaConciliado -> AccionSugerida.ACEPTAR_LA_DEL_SERVIDOR
                else -> AccionSugerida.CERRAR
            },
        )
    }

    private fun Rechazo.motivo(nombres: Map<String, String>, chapetas: List<Chapeta>): MotivoRechazo = when {
        salioConSuAnimal -> MotivoRechazo.SeRechazoSuAnimal
        entidad == EntidadSincronizada.CHAPETA && codigoError == UNICIDAD -> {
            val activa = chapetas.firstOrNull { it.codigo == codigoChapeta }
            if (activa != null && activa.idAnimal == idAnimal) {
                MotivoRechazo.CodigoYaEsDeEsteAnimal
            } else {
                MotivoRechazo.CodigoEnOtroAnimal(activa?.idAnimal?.let(nombres::get))
            }
        }
        // Dos celulares concilian el mismo pesaje: el Registro repite su lectura y el pendiente ya está resuelto.
        entidad == EntidadSincronizada.PESAJE_PENDIENTE && codigoError == ESTADO_DEFINITIVO -> MotivoRechazo.YaConciliado
        entidad in CON_LECTURA && codigoError == UNICIDAD -> MotivoRechazo.YaConciliado
        entidad == EntidadSincronizada.FOTO ->
            if (codigoError == FOTO_SIN_PERMISO) MotivoRechazo.SinPermiso else MotivoRechazo.FotoNoAceptada
        codigoError == PERMISO -> MotivoRechazo.SinPermiso
        else -> MotivoRechazo.Otro(mensaje)
    }

    private companion object {
        const val UNICIDAD = "23505"
        const val ESTADO_DEFINITIVO = "55000"
        const val PERMISO = "42501"
        const val FOTO_SIN_PERMISO = "403"
        val CON_LECTURA = setOf(EntidadSincronizada.REGISTRO, EntidadSincronizada.PESAJE_PENDIENTE)
    }
}
