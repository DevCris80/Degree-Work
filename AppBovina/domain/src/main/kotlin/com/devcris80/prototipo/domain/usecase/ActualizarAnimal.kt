package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Sexo
import com.devcris80.prototipo.domain.repository.AnimalRepository
import com.devcris80.prototipo.domain.repository.Transaccion

/**
 * Guarda los cambios de un Animal activo. La finca y la baja no se editan: se conservan las del Animal
 * guardado. Si lo recibido es igual a lo guardado no escribe nada, para no dejar una escritura pendiente
 * de subir sin motivo. La etapa debe corresponder al sexo; eso lo exige el propio [Animal].
 *
 * Si trae padre o madre, deben existir y ser macho y hembra, igual que al registrarlo. Un Animal que ya es
 * padre o madre de otros no cambia de sexo: limpiar esas referencias borraría datos de otros Animales.
 *
 * Si la persona no cambió la foto ([fotoCambiada] falso), queda la guardada: la sincronización pudo bajar una más
 * nueva mientras el formulario estaba abierto, y guardar no la vuelve a la de antes.
 */
class ActualizarAnimal(
    private val animalRepository: AnimalRepository,
    private val transaccion: Transaccion,
) {
    sealed interface Resultado {
        data object Actualizado : Resultado
        data object SinCambios : Resultado

        /** El Animal no existe o está dado de baja. */
        data object NoEditable : Resultado

        /** Cambiar el sexo dejaría a sus crías con un padre hembra o una madre macho. */
        data object SexoConCrias : Resultado
    }

    suspend operator fun invoke(animal: Animal, fotoCambiada: Boolean = true): Resultado = transaccion.ejecutar {
        val guardado = animalRepository.findById(animal.idAnimal)
        if (guardado == null || !guardado.activo) return@ejecutar Resultado.NoEditable
        val nuevo = animal.copy(
            idPerfilFinca = guardado.idPerfilFinca,
            fechaBaja = null,
            fotoUri = if (fotoCambiada) animal.fotoUri else guardado.fotoUri,
        )
        if (nuevo == guardado) return@ejecutar Resultado.SinCambios
        if (nuevo.sexo != guardado.sexo && animalRepository.tieneCrias(guardado.idAnimal)) {
            return@ejecutar Resultado.SexoConCrias
        }
        animalRepository.verificarProgenitor(nuevo.idPadre, Sexo.MACHO)
        animalRepository.verificarProgenitor(nuevo.idMadre, Sexo.HEMBRA)
        animalRepository.update(nuevo)
        Resultado.Actualizado
    }
}
