package com.devcris80.prototipo.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Un Sibling que una persona resolvió en este celular y que la próxima subida todavía no ha nombrado
 * (ADR-0004, regla 5). Polimórfica, como `sibling`: `entidad` e `idFila` dicen de qué fila es. Sale de esta
 * tabla cuando la subida de la edición que lo resuelve recibe respuesta del servidor.
 */
@Entity(tableName = "resolucion", indices = [Index("entidad", "idFila")])
data class ResolucionEntity(
    @PrimaryKey val idVersionSibling: String,
    val entidad: String,
    val idFila: String,
)
