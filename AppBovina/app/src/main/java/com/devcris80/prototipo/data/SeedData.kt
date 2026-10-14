package com.devcris80.prototipo.data

import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.model.Sexo
import com.devcris80.prototipo.domain.repository.AnimalRepository
import com.devcris80.prototipo.domain.repository.ChapetaRepository
import com.devcris80.prototipo.domain.repository.CuentaRepository
import com.devcris80.prototipo.domain.usecase.RegistrarAnimal
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.UUID

const val SEED_CHIP_ID = "TEST001"

/**
 * Precarga un Animal y una Chapeta de prueba (codigo = "TEST001") para poder probar el flujo
 * de pesaje end-to-end sin depender de hardware NFC físico (ver sección 6 de la spec).
 * Solo corre cuando ya existe una finca, para no saltarse el onboarding, y nunca vuelve a sembrar
 * si esa Chapeta ya existió.
 */
class SeedData(
    private val cuentaRepository: CuentaRepository,
    private val animalRepository: AnimalRepository,
    private val chapetaRepository: ChapetaRepository,
    private val registrarAnimal: RegistrarAnimal,
) {
    suspend fun seedTestDataIfEmpty() {
        val perfilFinca = cuentaRepository.getPerfilFinca() ?: return
        if (animalRepository.observeActivos().first().isNotEmpty()) return
        if (chapetaRepository.findUltimaByCodigo(SEED_CHIP_ID) != null) return

        val fechaNacimiento = Calendar.getInstance().apply { add(Calendar.YEAR, -2) }.timeInMillis
        registrarAnimal(
            Animal(
                idAnimal = UUID.randomUUID().toString(),
                idPerfilFinca = perfilFinca.idPerfilFinca,
                nombre = "Animal de prueba",
                raza = "Holstein",
                sexo = Sexo.HEMBRA,
                etapa = Etapa.VACA,
                fechaNacimiento = fechaNacimiento,
                fechaNacimientoEsEstimada = true,
                proposito = Proposito.LECHE,
            ),
            codigoIngresado = SEED_CHIP_ID,
        )
    }
}
