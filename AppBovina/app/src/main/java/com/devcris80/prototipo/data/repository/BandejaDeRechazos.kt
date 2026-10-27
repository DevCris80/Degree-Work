package com.devcris80.prototipo.data.repository

import com.devcris80.prototipo.data.local.dao.RechazoDao
import com.devcris80.prototipo.data.local.entity.RechazoEntity
import kotlinx.serialization.json.JsonObject

/**
 * Donde la sincronización deja lo que el servidor no aceptó (RN-24): una fila o una foto. Cada Rechazo queda abierto
 * hasta que una persona lo corrige. [entidad] es el nombre que tiene en el servidor, como en `TEXTO_ENTIDAD_SINCRONIZADA`.
 */
internal class BandejaDeRechazos(
    private val rechazos: RechazoDao,
    private val clock: () -> Long,
    private val nuevoIdRechazo: () -> String,
) {
    /** [contenido] es lo que se intentó subir, y [motivo] empieza con el código de error del servidor. */
    suspend fun guardar(entidad: String, idFila: String, contenido: JsonObject, motivo: String) = rechazos.insert(
        RechazoEntity(
            idRechazo = nuevoIdRechazo(),
            entidad = entidad,
            idFila = idFila,
            contenido = contenido.toString(),
            motivo = motivo,
            fechaRechazo = clock(),
        ),
    )
}
