package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.repository.AnimalRepository
import com.devcris80.prototipo.domain.repository.ChapetaRepository

sealed interface DestinoEscaneo {
    data class AbrirAnimal(val idAnimal: String) : DestinoEscaneo
    data class AnimalDadoDeBaja(val idChapeta: String, val nombreAnimal: String, val codigo: String) : DestinoEscaneo
    data class RegistrarConAviso(val codigo: String, val nombreAnimalAnterior: String) : DestinoEscaneo
    data class RegistrarNuevo(val codigo: String) : DestinoEscaneo
}

/**
 * Decide qué pantalla corresponde a un codigo leído en Escanear (ver decisiones de #31):
 * Chapeta activa de animal activo → detalle; activa de animal dado de baja → liberar primero;
 * sin activa pero con historial → registrar avisando del animal anterior; nunca visto → registrar.
 */
class ResolverDestinoEscaneo(
    private val chapetaRepository: ChapetaRepository,
    private val animalRepository: AnimalRepository,
) {
    suspend operator fun invoke(codigo: String): DestinoEscaneo {
        val activa = chapetaRepository.findActivaByCodigo(codigo)
        if (activa != null) {
            val animal = animalRepository.findById(activa.idAnimal)
            return when {
                animal == null -> DestinoEscaneo.RegistrarNuevo(codigo)
                animal.fechaBaja == null -> DestinoEscaneo.AbrirAnimal(animal.idAnimal)
                else -> DestinoEscaneo.AnimalDadoDeBaja(activa.idChapeta, animal.nombre, codigo)
            }
        }
        val ultima = chapetaRepository.findUltimaByCodigo(codigo) ?: return DestinoEscaneo.RegistrarNuevo(codigo)
        val anterior = animalRepository.findById(ultima.idAnimal) ?: return DestinoEscaneo.RegistrarNuevo(codigo)
        return DestinoEscaneo.RegistrarConAviso(codigo, anterior.nombre)
    }
}
