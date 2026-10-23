package com.devcris80.prototipo.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigracionesTest {
    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun de11a12ConservaLosDatosYDaUnIdVersionDistintoACadaFila() {
        helper.createDatabase(BASE, 11).use { db ->
            db.execSQL("INSERT INTO perfil_finca VALUES ('finca-1', 'Finca', 0, 0)")
            db.execSQL("INSERT INTO usuario VALUES ('usuario-1', 'finca-1', 'Ganadero', 'Ganadero', 0, 0)")
            db.execSQL(
                "INSERT INTO animal (idAnimal, idPerfilFinca, nombre, raza, sexo, etapa, fechaNacimiento, " +
                    "fechaNacimientoEsEstimada, proposito, sincronizado, fechaModificacion) VALUES " +
                    "('animal-1', 'finca-1', 'Lucero', 'Brahman', 'Hembra', 'Vaca', 0, 0, 'Leche', 0, 0), " +
                    "('animal-2', 'finca-1', 'Mariposa', 'Gyr', 'Hembra', 'Ternera', 0, 0, 'Leche', 0, 0)",
            )
            db.execSQL("INSERT INTO evento VALUES ('evento-1', 'animal-1', 'usuario-1', 'Vacunación', 0, 'Aftosa', NULL, 0, 0)")
            db.execSQL(
                "INSERT INTO registro VALUES ('registro-1', 'animal-1', 'usuario-1', 'lectura-1', 410.0, 0, NULL, 0, 0)",
            )
        }

        helper.runMigrationsAndValidate(BASE, 12, true, MIGRACION_11_12).use { db ->
            val versiones = mutableListOf<String>()
            for (tabla in listOf("animal", "evento", "registro")) {
                db.query("SELECT rev, revBase, idVersion FROM $tabla").use { fila ->
                    while (fila.moveToNext()) {
                        assertTrue(fila.isNull(0))
                        assertTrue(fila.isNull(1))
                        versiones += fila.getString(2)
                    }
                }
            }
            assertEquals(4, versiones.size)
            assertEquals("cada fila tiene su propio idVersion", 4, versiones.toSet().size)
            versiones.forEach { assertTrue("$it no es un UUID", UUID.matches(it)) }

            db.query("SELECT nombre FROM animal ORDER BY nombre").use { fila ->
                fila.moveToNext()
                assertEquals("Lucero", fila.getString(0))
            }
            for (tabla in listOf("sibling", "rechazo", "estado_sync")) {
                db.query("SELECT COUNT(*) FROM $tabla").use { fila ->
                    fila.moveToNext()
                    assertEquals("$tabla nace vacía", 0, fila.getInt(0))
                }
            }
        }
    }

    private companion object {
        const val BASE = "migracion-test"
        val UUID = Regex("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")
    }
}
