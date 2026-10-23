package com.devcris80.prototipo.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Una fila que el servidor no aceptó, sacada de su tabla y guardada completa (ADR-0004, regla 7).
 * Polimórfica como SiblingEntity. Está abierta mientras `fechaCorreccion` es nula, y después se conserva.
 */
@Entity(tableName = "rechazo")
data class RechazoEntity(
    @PrimaryKey val idRechazo: String,
    val entidad: String,
    val idFila: String,
    /** La fila completa que se intentó subir, en JSON. */
    val contenido: String,
    val motivo: String,
    val fechaRechazo: Long,
    val fechaCorreccion: Long? = null,
)
