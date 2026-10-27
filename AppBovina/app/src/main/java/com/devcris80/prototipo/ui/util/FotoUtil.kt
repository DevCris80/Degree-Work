package com.devcris80.prototipo.ui.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/** Crea un archivo nuevo en almacenamiento interno para que la cámara escriba la foto tomada. */
fun crearArchivoFotoUri(context: Context): Uri = uriDe(context, crearArchivoFoto(context))

/**
 * Copia a almacenamiento interno la foto elegida de la galería y devuelve su URI, o nula si no se pudo leer. El
 * permiso para leer la URI de la galería se vence cuando se cierra la app, y la foto todavía tiene que subir (#86).
 */
suspend fun copiarFotoElegida(context: Context, elegida: Uri): Uri? = withContext(Dispatchers.IO) {
    val archivo = crearArchivoFoto(context)
    val copiada = try {
        context.contentResolver.openInputStream(elegida)?.use { entrada ->
            archivo.outputStream().use { entrada.copyTo(it) }
        } != null
    } catch (e: IOException) {
        false
    } catch (e: SecurityException) {
        false
    }
    if (copiada) uriDe(context, archivo) else null.also { archivo.delete() }
}

private fun crearArchivoFoto(context: Context): File {
    val carpeta = File(context.filesDir, "fotos").apply { mkdirs() }
    return File.createTempFile("foto_", ".jpg", carpeta)
}

private fun uriDe(context: Context, archivo: File): Uri =
    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
