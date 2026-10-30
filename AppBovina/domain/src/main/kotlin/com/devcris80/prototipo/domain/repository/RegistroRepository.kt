package com.devcris80.prototipo.domain.repository

import com.devcris80.prototipo.domain.model.Registro
import kotlinx.coroutines.flow.Flow

interface RegistroRepository {
    suspend fun insert(registro: Registro)

    suspend fun findByIdLectura(idLectura: String): Registro?

    /** Registros del Animal que no están dados de baja, del más reciente al más antiguo. */
    fun observeByAnimal(idAnimal: String): Flow<List<Registro>>

    fun observeUltimoByAnimal(idAnimal: String): Flow<Registro?>

    suspend fun findById(idRegistro: String): Registro?

    /** Guarda el peso y la hora de medición del Registro. Nada más cambia: ni el Animal, ni el autor, ni el idLectura. */
    suspend fun update(registro: Registro)

    suspend fun darDeBaja(idRegistro: String, fecha: Long)
}
