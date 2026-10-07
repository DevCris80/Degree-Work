package com.devcris80.prototipo.domain

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.devcris80.prototipo.data.Animal
import com.devcris80.prototipo.data.AppDatabase
import com.devcris80.prototipo.data.Chapeta
import com.devcris80.prototipo.data.PERFIL_FINCA_ID
import com.devcris80.prototipo.data.PerfilFinca
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DestinoEscaneoResolverTest {
    private lateinit var db: AppDatabase
    private lateinit var resolver: DestinoEscaneoResolver

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).addCallback(AppDatabase.CALLBACK_INDICES_CHAPETA).allowMainThreadQueries().build()
        resolver = DestinoEscaneoResolver(db.chapetaDao(), db.animalDao())
        db.perfilFincaDao().guardar(PerfilFinca(nombreFinca = "Finca test"))
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun crearAnimal(nombre: String, fechaBaja: Long? = null): Animal {
        val animal = Animal(
            idAnimal = UUID.randomUUID().toString(),
            idPerfilFinca = PERFIL_FINCA_ID,
            nombre = nombre,
            raza = "Holstein",
            sexo = "Hembra",
            etapa = "Vaca",
            fechaNacimiento = 1_700_000_000_000L,
            proposito = "Leche",
            fechaBaja = fechaBaja,
        )
        db.animalDao().insert(animal)
        return animal
    }

    private suspend fun asociar(animal: Animal, codigo: String): Chapeta {
        val chapeta = Chapeta(
            idChapeta = UUID.randomUUID().toString(),
            codigo = codigo,
            idAnimal = animal.idAnimal,
            fechaAsociacion = 1L,
        )
        db.chapetaDao().insert(chapeta)
        return chapeta
    }

    @Test
    fun chapetaActivaDeAnimalActivo_abreElAnimal() = runTest {
        val animal = crearAnimal("Manchas")
        asociar(animal, "04A1B2C3")

        assertEquals(
            DestinoEscaneo.AbrirAnimal(animal.idAnimal),
            resolver.resolver("04A1B2C3"),
        )
    }

    @Test
    fun chapetaActivaDeAnimalDadoDeBaja_pideLiberarPrimero() = runTest {
        val animal = crearAnimal("Manchas", fechaBaja = 2L)
        val chapeta = asociar(animal, "04A1B2C3")

        assertEquals(
            DestinoEscaneo.AnimalDadoDeBaja(chapeta.idChapeta, "Manchas", "04A1B2C3"),
            resolver.resolver("04A1B2C3"),
        )
    }

    @Test
    fun codigoConHistorialSinActiva_registraAvisandoDelAnimalAnterior() = runTest {
        val anterior = crearAnimal("Canela")
        val chapeta = asociar(anterior, "04A1B2C3")
        db.chapetaDao().desasociar(chapeta.idChapeta, 2L)

        assertEquals(
            DestinoEscaneo.RegistrarConAviso("04A1B2C3", "Canela"),
            resolver.resolver("04A1B2C3"),
        )
    }

    @Test
    fun codigoNuncaVisto_registraNuevo() = runTest {
        assertEquals(
            DestinoEscaneo.RegistrarNuevo("FFEEDD00"),
            resolver.resolver("FFEEDD00"),
        )
    }
}
