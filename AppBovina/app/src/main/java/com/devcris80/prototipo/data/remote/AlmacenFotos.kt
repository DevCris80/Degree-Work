package com.devcris80.prototipo.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlin.time.ExperimentalTime

/** Un archivo del bucket de fotos: [nombre] es `<idAnimal>.webp`, y [version], su `updated_at`. */
data class FotoGuardada(val nombre: String, val version: String, val tamano: Long?)

/** El bucket `fotos` de Storage (supabase/README.md): una carpeta por finca y un archivo por Animal. */
interface AlmacenFotos {
    /** Sube [webp] a [ruta], o reemplaza la foto que ya estaba. */
    suspend fun subir(ruta: String, webp: ByteArray)

    /** Todas las fotos de [carpeta]. */
    suspend fun listar(carpeta: String): List<FotoGuardada>

    suspend fun descargar(ruta: String): ByteArray
}

/** Llama la API de Storage con el JWT de la sesión guardada. */
class SupabaseAlmacenFotos(supabase: SupabaseClient) : AlmacenFotos {
    private val bucket = supabase.storage.from("fotos")

    override suspend fun subir(ruta: String, webp: ByteArray) {
        bucket.upload(ruta, webp) {
            upsert = true
            contentType = ContentType("image", "webp")
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
    }
}
