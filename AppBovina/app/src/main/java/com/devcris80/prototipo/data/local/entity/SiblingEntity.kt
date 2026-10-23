package com.devcris80.prototipo.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Un Sibling sin resolver de un Animal, Evento o Registro (ADR-0004). Polimórfica: `entidad` e `idFila`
 * dicen de qué fila es, así que no lleva FK. Al resolverse sale de esta tabla; el servidor lo conserva.
 */
@Entity(tableName = "sibling", indices = [Index("entidad", "idFila")])
data class SiblingEntity(
    @PrimaryKey val idVersion: String,
    val entidad: String,
    val idFila: String,
    val revBase: Int,
    /** La fila completa que subió su autor, en JSON. */
    val contenido: String,
    val idUsuario: String,
    val fechaRecepcion: Long,
)
