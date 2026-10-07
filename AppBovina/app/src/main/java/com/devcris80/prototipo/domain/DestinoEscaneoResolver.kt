package com.devcris80.prototipo.domain

import com.devcris80.prototipo.data.AnimalDao
import com.devcris80.prototipo.data.ChapetaDao

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
class DestinoEscaneoResolver(
    private val chapetaDao: ChapetaDao,
    private val animalDao: AnimalDao,
) {
    suspend fun resolver(codigo: String): DestinoEscaneo {
        val activa = chapetaDao.findActivaByCodigo(codigo)
        if (activa != null) {
            val animal = animalDao.findById(activa.idAnimal)
            return when {
                animal == null -> DestinoEscaneo.RegistrarNuevo(codigo)
                animal.fechaBaja == null -> DestinoEscaneo.AbrirAnimal(animal.idAnimal)
                else -> DestinoEscaneo.AnimalDadoDeBaja(activa.idChapeta, animal.nombre, codigo)
            }
        }
        val ultima = chapetaDao.findUltimaByCodigo(codigo) ?: return DestinoEscaneo.RegistrarNuevo(codigo)
        val anterior = animalDao.findById(ultima.idAnimal) ?: return DestinoEscaneo.RegistrarNuevo(codigo)
        return DestinoEscaneo.RegistrarConAviso(codigo, anterior.nombre)
    }
}
