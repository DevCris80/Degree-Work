package com.devcris80.prototipo.ui.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Abrir la cámara del sistema para la foto de un Animal, como lo hace `TakePicture` en el formulario. Si el manifiesto
 * declara `android.permission.CAMERA` sin que la app lo pida, Android lanza `SecurityException` y la app se cierra
 * (#146): la foto la toma la cámara del sistema y la app no necesita ese permiso.
 */
@RunWith(AndroidJUnit4::class)
class AbrirCamaraTest {
    @Test
    fun abrirLaCamaraDelSistema_noLanzaSecurityException() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val uri = crearArchivoFotoUri(context)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            .putExtra(MediaStore.EXTRA_OUTPUT, uri)
            .apply { clipData = ClipData.newRawUri("", uri) }
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

        context.startActivity(intent)
    }
}
