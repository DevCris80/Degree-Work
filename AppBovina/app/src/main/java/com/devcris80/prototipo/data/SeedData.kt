package com.devcris80.prototipo.data

import java.util.UUID

const val SEED_CHIP_ID = "TEST001"

/**
 * Precarga un Animal y una Chapeta de prueba (id_chip = "TEST001") para poder probar el flujo
 * de pesaje end-to-end sin depender de hardware NFC físico (ver sección 6 de la spec).
 */
suspend fun AppDatabase.seedTestDataIfEmpty() {
    if (animalDao().getAllOnce().isNotEmpty()) return

    val idAnimal = UUID.randomUUID().toString()
    animalDao().insert(
        Animal(
            idAnimal = idAnimal,
            nombre = "Animal de prueba",
            raza = "Holstein",
            sexo = "hembra",
            edad = 2,
            proposito = "leche",
        ),
    )
    chapetaDao().insert(
        Chapeta(
            idChip = SEED_CHIP_ID,
            idAnimal = idAnimal,
            fechaAsociacion = System.currentTimeMillis(),
        ),
    )
}
