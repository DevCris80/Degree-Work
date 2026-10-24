package com.devcris80.prototipo.data.local.dao

/**
 * Lo que escribe toda modificación de un Animal, un Evento o un Registro, además de sus campos
 * (ADR-0004, regla 2): la fila queda pendiente de subir, con un `idVersion` nuevo y su fecha de
 * modificación. `revBase` toma la revisión vigente solo en la primera escritura después de
 * sincronizar, y las siguientes la conservan. Funciona porque SQLite evalúa todo el SET con los
 * valores anteriores de la fila, así que `sincronizado` todavía dice si estaba al día.
 *
 * La consulta que lo usa recibe los parámetros `:idVersion` y `:fechaModificacion`.
 *
 * Una excepción: resolver un Conflicto (`TablaConRevisiones.resolviendo`) fija `revBase` en la revisión vigente
 * aunque la fila ya estuviera pendiente, porque la resolución es una edición nueva sobre la vigente (ADR-0004,
 * regla 5), no una continuación de la edición anterior.
 */
const val EDICION_LOCAL =
    "sincronizado = 0, revBase = CASE WHEN sincronizado = 1 THEN rev ELSE revBase END, " +
        "idVersion = :idVersion, fechaModificacion = :fechaModificacion"
