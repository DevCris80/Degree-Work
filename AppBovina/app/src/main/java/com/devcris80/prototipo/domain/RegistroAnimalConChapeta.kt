package com.devcris80.prototipo.domain

import androidx.room.withTransaction
import com.devcris80.prototipo.data.Animal
import com.devcris80.prototipo.data.AppDatabase
import com.devcris80.prototipo.data.Chapeta
import java.util.UUID

/**
 * Inserta un Animal y, si hay codigo, su Chapeta activa en una sola transacción: o se guardan
 * ambos o ninguno. Verifica dentro de la transacción que el codigo no tenga otra Chapeta activa.
 */
class RegistroAnimalConChapeta(
    private val database: AppDatabase,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    sealed interface Resultado {
        data class Exito(val idAnimal: String) : Resultado
        data class CodigoYaActivo(val codigo: String) : Resultado
    }

    suspend fun registrar(animal: Animal, codigo: String?): Resultado = database.withTransaction {
        if (codigo != null && database.chapetaDao().findActivaByCodigo(codigo) != null) {
            return@withTransaction Resultado.CodigoYaActivo(codigo)
        }
        database.animalDao().insert(animal)
        if (codigo != null) {
            database.chapetaDao().insert(
                Chapeta(
                    idChapeta = UUID.randomUUID().toString(),
                    codigo = codigo,
                    idAnimal = animal.idAnimal,
                    fechaAsociacion = clock(),
                ),
            )
        }
        Resultado.Exito(animal.idAnimal)
    }
}
