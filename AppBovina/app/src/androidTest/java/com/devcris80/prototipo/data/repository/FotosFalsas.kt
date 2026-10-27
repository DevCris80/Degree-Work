package com.devcris80.prototipo.data.repository

import com.devcris80.prototipo.data.archivos.FotosLocales
import com.devcris80.prototipo.data.remote.AlmacenFotos
import com.devcris80.prototipo.data.remote.FotoGuardada
import kotlinx.coroutines.CompletableDeferred

/**
 * El bucket de fotos, compartido por los celulares de una prueba. Cada subida le da a su archivo una versión nueva,
 * como el `updated_at` de Storage. [falla], si no es nulo, se lanza en cada llamada; [fallaAlSubir], solo al subir.
 */
internal class AlmacenFotosFalso : AlmacenFotos {
    val archivos = mutableMapOf<String, Pair<ByteArray, String>>()
    val subidas = mutableListOf<String>()
    val descargas = mutableListOf<String>()
    var falla: Exception? = null
    var fallaAlSubir: Exception? = null

    /** Si no es nula, cada subida avisa en [subiendo] y espera a que se complete. */
    var pausaAlSubir: CompletableDeferred<Unit>? = null
    val subiendo = CompletableDeferred<Unit>()
    private var versiones = 0

    override suspend fun subir(ruta: String, webp: ByteArray) {
        (falla ?: fallaAlSubir)?.let { throw it }
        pausaAlSubir?.let {
            subiendo.complete(Unit)
            it.await()
        }
        subidas += ruta
        archivos[ruta] = webp to "version-${++versiones}"
    }

    override suspend fun listar(carpeta: String): List<FotoGuardada> {
        falla?.let { throw it }
        return archivos.filterKeys { it.startsWith("$carpeta/") }.map { (ruta, archivo) ->
            FotoGuardada(ruta.substringAfterLast('/'), archivo.second, archivo.first.size.toLong())
        }
    }

    override suspend fun descargar(ruta: String): ByteArray {
        falla?.let { throw it }
        descargas += ruta
        return checkNotNull(archivos[ruta]) { "No hay foto en $ruta" }.first
    }
}

/**
 * Los archivos de un celular: el "WebP" de una foto es el texto de su URI, y una foto bajada queda en
 * `bajada://<idAnimal>/<n>`. Las URIs de [ilegibles] ya no se pueden leer.
 */
internal class FotosLocalesFalsas : FotosLocales {
    val guardadas = mutableMapOf<String, ByteArray>()
    val ilegibles = mutableSetOf<String>()
    val borradas = mutableListOf<String>()
    var enUsoAlLimpiar: Set<String>? = null

    /** Si no es nulo, se llama después de guardar una foto bajada, antes de que el motor la anote en el Animal. */
    var alGuardar: (suspend () -> Unit)? = null

    override suspend fun comoWebp(fotoUri: String): ByteArray? =
        if (fotoUri in ilegibles) null else guardadas[fotoUri] ?: "webp de $fotoUri".toByteArray()

    override suspend fun guardar(idAnimal: String, webp: ByteArray): String {
        val fotoUri = "bajada://$idAnimal/${guardadas.size + 1}"
        guardadas[fotoUri] = webp
        alGuardar?.invoke()
        return fotoUri
    }

    override suspend fun borrar(fotoUri: String) {
        borradas += fotoUri
    }

    override suspend fun limpiar(enUso: Set<String>) {
        enUsoAlLimpiar = enUso
    }
}
