package com.devcris80.prototipo.domain

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.devcris80.prototipo.data.Animal
import com.devcris80.prototipo.data.AppDatabase
import com.devcris80.prototipo.data.Chapeta
import com.devcris80.prototipo.data.PERFIL_FINCA_ID
import com.devcris80.prototipo.data.PerfilFinca
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class RegistroPesoResolverTest {
    private lateinit var db: AppDatabase
    private lateinit var resolver: RegistroPesoResolver

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        resolver = RegistroPesoResolver(
            chapetaDao = db.chapetaDao(),
            registroDao = db.registroDao(),
            clock = { FIXED_TIMESTAMP },
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun conIdChipConocido_creaRegistroAsociadoAlAnimalCorrecto() = runTest {
        val idAnimal = UUID.randomUUID().toString()
        db.perfilFincaDao().guardar(PerfilFinca(nombreFinca = "Finca test"))
        db.animalDao().insert(
            Animal(
                idAnimal = idAnimal,
                idPerfilFinca = PERFIL_FINCA_ID,
                nombre = "Manchas",
                raza = "Holstein",
                sexo = "Hembra",
                etapa = "Vaca",
                fechaNacimiento = 1_700_000_000_000L,
                proposito = "Leche",
            ),
        )
        db.chapetaDao().insert(
            Chapeta(
                idChip = "TEST001",
                idAnimal = idAnimal,
                fechaAsociacion = 1L,
            ),
        )

        val resultado = resolver.registrarPeso(idChip = "TEST001", peso = 123.45f)

        assertTrue(resultado is RegistroPesoResultado.Exito)
        val exito = resultado as RegistroPesoResultado.Exito
        val registros = db.registroDao().observeByAnimal(idAnimal).first()
        assertEquals(1, registros.size)
        assertEquals(exito.idRegistro, registros[0].idRegistro)
        assertEquals(idAnimal, registros[0].idAnimal)
        assertEquals(123.45f, registros[0].peso)
        assertEquals(FIXED_TIMESTAMP, registros[0].timestamp)
    }

    @Test
    fun conIdChipDesconocido_noCreaRegistroYRetornaError() = runTest {
        val resultado = resolver.registrarPeso(idChip = "NO-EXISTE", peso = 50f)

        assertTrue(resultado is RegistroPesoResultado.Error)
        val registros = db.registroDao().observeByAnimal("cualquier-id").first()
        assertEquals(0, registros.size)
    }

    private companion object {
        const val FIXED_TIMESTAMP = 1_700_000_000_000L
    }
}
