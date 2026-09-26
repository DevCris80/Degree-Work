package com.devcris80.prototipo.ui.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/** Crea un archivo nuevo en almacenamiento interno para que la cámara escriba la foto tomada. */
fun crearArchivoFotoUri(context: Context): Uri {
    val carpeta = File(context.filesDir, "fotos").apply { mkdirs() }
    val archivo = File.createTempFile("foto_", ".jpg", carpeta)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
}
