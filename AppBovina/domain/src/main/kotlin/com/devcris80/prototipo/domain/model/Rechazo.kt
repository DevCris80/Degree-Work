package com.devcris80.prototipo.domain.model

/** Las entidades que se suben al servidor. */
enum class EntidadSincronizada { PERFIL_FINCA, USUARIO, ANIMAL, EVENTO, REGISTRO, CHAPETA, PESAJE_PENDIENTE }

/** Un cambio local que el servidor no aceptó y espera que una persona lo corrija. */
data class Rechazo(
    val idRechazo: String,
    val entidad: EntidadSincronizada,
    val idFila: String,
    /** La fila completa que se intentó subir, en JSON. */
    val contenido: String,
    val motivo: String,
    val fechaRechazo: Long,
)
