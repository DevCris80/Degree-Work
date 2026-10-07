package com.devcris80.prototipo.data

import java.util.Calendar
import java.util.UUID

const val SEED_CHIP_ID = "TEST001"

/**
 * Precarga un Animal y una Chapeta de prueba (id_chip = "TEST001") para poder probar el flujo
 * de pesaje end-to-end sin depender de hardware NFC físico (ver sección 6 de la spec).
 * Solo corre cuando ya existe una finca, para no saltarse el onboarding.
 */
suspend fun AppDatabase.seedTestDataIfEmpty() {
    if (animalDao().getAllOnce().isNotEmpty()) return
    if (perfilFincaDao().getOnce() == null) return

    val idAnimal = UUID.randomUUID().toString()
    val fechaNacimiento = Calendar.getInstance().apply { add(Calendar.YEAR, -2) }.timeInMillis
    animalDao().insert(
        Animal(
            idAnimal = idAnimal,
            idPerfilFinca = PERFIL_FINCA_ID,
            nombre = "Animal de prueba",
            raza = "Holstein",
            sexo = "Hembra",
            etapa = "Vaca",
            fechaNacimiento = fechaNacimiento,
            fechaNacimientoEsEstimada = true,
            proposito = "Leche",
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
