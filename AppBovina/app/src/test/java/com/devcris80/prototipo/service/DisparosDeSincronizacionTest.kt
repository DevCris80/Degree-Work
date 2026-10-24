package com.devcris80.prototipo.service

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** Cómo las escrituras locales se vuelven sincronizaciones (CU-20): cada valor es cuántos cambios faltan por subir. */
class DisparosDeSincronizacionTest {
    private val espera = 2.minutes

    @Test
    fun treintaPesajesSeguidosUnoPorMinutoDanUnSoloDisparo() = runTest {
        val pendientes = flow {
            repeat(30) {
                emit(it + 1)
                delay(1.minutes)
            }
        }

        assertEquals(1, pendientes.disparosDeSincronizacion(espera).count())
    }

    @Test
    fun unaEscrituraDespuesDeLaEsperaDaOtroDisparo() = runTest {
        val pendientes = flow {
            emit(1)
            delay(espera + 1.seconds)
            emit(2)
        }

        assertEquals(2, pendientes.disparosDeSincronizacion(espera).count())
    }

    @Test
    fun siYaSeSubioTodoAntesDeLaEsperaNoHayDisparo() = runTest {
        val pendientes = flow {
            emit(1)
            delay(30.seconds)
            emit(0)
        }

        assertEquals(0, pendientes.disparosDeSincronizacion(espera).count())
    }
}
