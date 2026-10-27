package com.devcris80.prototipo.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.UnknownRestException
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.time.ExperimentalTime

/** Un archivo del bucket de fotos: [nombre] es `<idAnimal>.webp`, y [fechaModificacion], su `updated_at`. */
data class FotoGuardada(val nombre: String, val fechaModificacion: String, val tamano: Long?)

/** Storage no acepta la foto y no la aceptará si se reenvía. [codigo] es el de supabase/README.md. */
class FotoRechazada(val codigo: String, val motivo: String) : Exception("$codigo: $motivo")

/** El bucket `fotos` de Storage (supabase/README.md): una carpeta por finca y un archivo por Animal. */
interface AlmacenFotos {
    /** Sube [webp] a [ruta], o reemplaza la foto que ya estaba. Lanza [FotoRechazada] si no la aceptará nunca. */
    suspend fun subir(ruta: String, webp: ByteArray)

    /** Todas las fotos de [carpeta]. */
    suspend fun listar(carpeta: String): List<FotoGuardada>

    suspend fun descargar(ruta: String): ByteArray
}

/** Llama la API de Storage con el JWT de la sesión guardada. */
class SupabaseAlmacenFotos(supabase: SupabaseClient) : AlmacenFotos {
    private val bucket = supabase.storage.from("fotos")

    override suspend fun subir(ruta: String, webp: ByteArray) {
        try {
            bucket.upload(ruta, webp) {
                upsert = true
                contentType = ContentType("image", "webp")
            }
        } catch (e: UnknownRestException) {
            val codigo = RECHAZOS.entries.firstOrNull { it.key in e.error }?.value ?: throw e
            throw FotoRechazada(codigo, e.error)
        }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun listar(carpeta: String): List<FotoGuardada> {
        val fotos = mutableListOf<FotoGuardada>()
        var desde = 0
        do {
            val pagina = bucket.list(carpeta) {
                limit = TAMANO_PAGINA
                offset = desde
            }
            desde += pagina.size
            // Una subcarpeta llega sin fecha. El bucket no deja crearlas, pero no es una foto.
            pagina.mapNotNullTo(fotos) { archivo ->
                archivo.updatedAt?.let {
                    FotoGuardada(archivo.name, it.toString(), archivo.metadata?.get("size")?.jsonPrimitive?.longOrNull)
                }
            }
        } while (pagina.size == TAMANO_PAGINA)
        return fotos
    }

    override suspend fun descargar(ruta: String): ByteArray = bucket.downloadAuthenticated(ruta)

    private companion object {
        const val TAMANO_PAGINA = 500

        /**
         * Los rechazos que no cambian al reenviar la misma foto, por un trozo del `message` con que responde Storage.
         * supabase-kt solo tipa 400, 401 y 404 (una sesión vencida, un bucket que todavía no existe), que sí se
         * reintentan. Los demás llegan como [UnknownRestException] con el `message` en `error` y sin el código.
         */
        val RECHAZOS = mapOf(
            "row-level security" to "403",
            "exceeded the maximum allowed size" to "413",
            "is not supported" to "415",
        )
    }
}
