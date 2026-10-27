package com.devcris80.prototipo.data.archivos

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Las fotos que suben son WebP de 1280 px como máximo en su lado mayor (RNF-10). */
@RunWith(AndroidJUnit4::class)
class FotosEnArchivosTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val fotos = FotosEnArchivos(context)

    @Test
    fun unaFotoGrandeDeLaCamaraSaleEnWebpDe1280PxYDerecha() = runTest {
        // La cámara guarda la foto acostada y anota en el EXIF que hay que girarla: el WebP no lleva EXIF.
        val tomada = jpeg(ancho = 4000, alto = 2000, orientacion = ExifInterface.ORIENTATION_ROTATE_90)

        val webp = checkNotNull(fotos.comoWebp(tomada))

        assertEquals("RIFF", String(webp, 0, 4))
        assertEquals("WEBP", String(webp, 8, 4))
        val tamano = tamano(webp)
        assertEquals("ya girada, queda de pie", 640 to 1280, tamano)
    }

    @Test
    fun unaFotoPequenaNoSeAgranda() = runTest {
        val elegida = jpeg(ancho = 800, alto = 600)

        val webp = checkNotNull(fotos.comoWebp(elegida))

        assertEquals("WEBP", String(webp, 8, 4))
        assertEquals(800 to 600, tamano(webp))
    }

    @Test
    fun unaFotoQueYaNoSePuedeLeerNoDaNada() = runTest {
        assertNull(fotos.comoWebp(Uri.fromFile(File(context.cacheDir, "no-existe.jpg")).toString()))
    }

    @Test
    fun unaFotoQueBajoDelServidorQuedaEnElCelular() = runTest {
        val bajada = checkNotNull(fotos.comoWebp(jpeg(ancho = 100, alto = 50)))

        val fotoUri = fotos.guardar("animal-1", bajada)

        val guardada = context.contentResolver.openInputStream(Uri.parse(fotoUri))?.use { it.readBytes() }
        assertNotNull(guardada)
        assertArrayEquals(bajada, guardada)
    }

    private fun jpeg(ancho: Int, alto: Int, orientacion: Int = ExifInterface.ORIENTATION_NORMAL): String {
        val archivo = File.createTempFile("foto_", ".jpg", context.cacheDir)
        val bitmap = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
        archivo.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        ExifInterface(archivo).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, orientacion.toString())
            saveAttributes()
        }
        return Uri.fromFile(archivo).toString()
    }

    private fun tamano(imagen: ByteArray): Pair<Int, Int> {
        val opciones = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(imagen, 0, imagen.size, opciones)
        return opciones.outWidth to opciones.outHeight
    }
}
