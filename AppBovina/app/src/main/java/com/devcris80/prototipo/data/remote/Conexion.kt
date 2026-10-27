package com.devcris80.prototipo.data.remote

import io.github.jan.supabase.exceptions.HttpRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.CancellationException
import java.io.IOException

/** Si la llamada al servidor falló porque no hay señal, y no porque el servidor respondió con un error. */
internal fun Exception.esFaltaDeConexion(): Boolean =
    this is HttpRequestException || this is HttpRequestTimeoutException || this is IOException

/** Lo que devuelve [bloque], o lo que devuelve [alFallar] con su excepción. Una cancelación sigue su curso. */
internal inline fun <T> salvoCancelacion(bloque: () -> T, alFallar: (Exception) -> T): T = try {
    bloque()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    alFallar(e)
}
