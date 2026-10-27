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
    /**
     * [contenido] es lo que se intentó subir, y [motivo] empieza con el código de error del servidor, como
     * `"23505: <mensaje>"`: la bandeja elige con él la acción sugerida.
     */
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

/** El motivo de una fila que sale del celular porque el servidor rechazó la [entidad] [id] de la que dependía. */
internal fun motivoDeUnDependiente(codigo: String, entidad: String, id: String, motivo: String) =
    "$codigo: $SE_RECHAZO $entidad $id. $motivo"

/** Si [motivo] es el de una fila que salió con la fila de la que dependía ([motivoDeUnDependiente]). */
internal fun esDeUnDependiente(motivo: String) = motivo.substringAfter(": ").startsWith(SE_RECHAZO)

private const val SE_RECHAZO = "Se rechazó el"
