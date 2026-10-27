package com.devcris80.prototipo.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

/** Las fotos de los Animales como archivos del celular, para subirlas y para guardar las que bajan (#86). */
interface FotosLocales {
    /** La foto en [fotoUri] lista para subir (RNF-10), o nula si ya no se puede leer. */
    suspend fun comoWebp(fotoUri: String): ByteArray?

    /** Guarda la foto [webp] que bajó del servidor para el Animal [idAnimal] y devuelve su URI. */
    suspend fun guardar(idAnimal: String, webp: ByteArray): String
}

/**
 * Las fotos en `files/fotos`, donde también escribe la cámara (`crearArchivoFotoUri`). Una foto bajada toma un
 * archivo nuevo en vez de reemplazar el anterior: con la misma URI, la pantalla no sabría que cambió.
 */
class FotosEnArchivos(private val context: Context) : FotosLocales {
    override suspend fun comoWebp(fotoUri: String): ByteArray? = withContext(Dispatchers.IO) {
        val uri = Uri.parse(fotoUri)
        try {
            aWebp { context.contentResolver.openInputStream(uri) ?: throw FileNotFoundException(fotoUri) }
        } catch (e: IOException) {
            null
        } catch (e: SecurityException) {
            // Una foto elegida de la galería deja de poder leerse cuando el permiso de la URI se vence.
            null
        }
    }

    override suspend fun guardar(idAnimal: String, webp: ByteArray): String = withContext(Dispatchers.IO) {
        val carpeta = File(context.filesDir, "fotos").apply { mkdirs() }
        val archivo = File(carpeta, "${idAnimal}_${System.currentTimeMillis()}.webp")
        val temporal = File(carpeta, "${archivo.name}.tmp")
        temporal.writeBytes(webp)
        check(temporal.renameTo(archivo)) { "No se pudo guardar la foto de $idAnimal" }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo).toString()
    }
}

/**
 * La foto como WebP con calidad [CALIDAD] y su lado mayor de [LADO_MAYOR] px como máximo, ya girada según su EXIF,
 * que el WebP no conserva. Nula si no es una imagen. [abrir] se llama una vez por cada lectura.
 */
private fun aWebp(abrir: () -> InputStream): ByteArray? {
    val giro = abrir().use { ExifInterface(it).rotationDegrees }
    val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    abrir().use { BitmapFactory.decodeStream(it, null, limites) }
    if (limites.outWidth <= 0 || limites.outHeight <= 0) return null
    // Decodifica de una vez a una fracción cercana al tamaño final: una foto de 50 MP no cabe en memoria.
    val opciones = BitmapFactory.Options().apply {
        inSampleSize = submuestreo(maxOf(limites.outWidth, limites.outHeight))
    }
    val leida = abrir().use { BitmapFactory.decodeStream(it, null, opciones) } ?: return null
    val escala = minOf(1f, LADO_MAYOR.toFloat() / maxOf(leida.width, leida.height))
    val ajuste = Matrix().apply {
        postScale(escala, escala)
        postRotate(giro.toFloat())
    }
    val lista = Bitmap.createBitmap(leida, 0, 0, leida.width, leida.height, ajuste, true)
    return ByteArrayOutputStream().use { salida ->
        lista.compress(FORMATO_WEBP, CALIDAD, salida)
        salida.toByteArray()
    }
}

/** La mayor potencia de 2 que deja el lado mayor en [LADO_MAYOR] px o más. */
private fun submuestreo(ladoMayor: Int): Int {
    var muestreo = 1
    while (ladoMayor / (muestreo * 2) >= LADO_MAYOR) muestreo *= 2
    return muestreo
}

private const val LADO_MAYOR = 1280
private const val CALIDAD = 75

private val FORMATO_WEBP =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSY else @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP
