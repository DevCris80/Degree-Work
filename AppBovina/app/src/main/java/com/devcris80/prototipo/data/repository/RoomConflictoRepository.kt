package com.devcris80.prototipo.data.repository

import androidx.room.withTransaction
import com.devcris80.prototipo.data.local.AppDatabase
import com.devcris80.prototipo.data.local.entity.ResolucionEntity
import com.devcris80.prototipo.domain.model.Conflicto
import com.devcris80.prototipo.domain.model.EntidadConRevisiones
import com.devcris80.prototipo.domain.model.Sibling
import com.devcris80.prototipo.domain.model.SiblingEnConflicto
import com.devcris80.prototipo.domain.repository.ConflictoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import java.util.UUID

class RoomConflictoRepository(
    private val database: AppDatabase,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val nuevoIdVersion: () -> String = { UUID.randomUUID().toString() },
) : ConflictoRepository {
    private val siblingDao = database.siblingDao()
    private val tablas = tablasSincronizadas(database).filterIsInstance<TablaConRevisiones<*>>()

    override fun observeEnConflicto(entidad: EntidadConRevisiones, idFila: String): Flow<Boolean> =
        siblingDao.observeEnConflicto(entidad.textoGuardado(), idFila)

    override fun observeFilasEnConflicto(entidad: EntidadConRevisiones): Flow<Set<String>> =
        siblingDao.observeFilasEnConflicto(entidad.textoGuardado()).map { it.toSet() }

    override fun observeSiblings(entidad: EntidadConRevisiones, idFila: String): Flow<List<Sibling>> =
        siblingDao.observeByFila(entidad.textoGuardado(), idFila).map { siblings -> siblings.map { it.toDomain() } }

    override fun observeConflicto(entidad: EntidadConRevisiones, idFila: String): Flow<Conflicto?> =
        siblingDao.observeByFila(entidad.textoGuardado(), idFila).map { siblings ->
            val tabla = tablaDe(entidad)
            val vigente = tabla.buscar(idFila) ?: return@map null
            // Un Sibling que no se puede leer no se muestra: la pantalla no puede compararlo ni elegirlo.
            val legibles = siblings.mapNotNull { sibling ->
                tabla.desdeElSibling(sibling.contenido)?.let { fila ->
                    SiblingEnConflicto(sibling.toDomain(), tabla.enConflicto(fila))
                }
            }
            if (legibles.isEmpty()) {
                null
            } else {
                Conflicto(entidad, idFila, vigente = tabla.enConflicto(vigente), siblings = legibles)
            }
        }

    override suspend fun resolver(
        entidad: EntidadConRevisiones,
        idFila: String,
        idVersionElegida: String?,
        idsResueltos: List<String>,
    ): Boolean {
        val tabla = tablaDe(entidad)
        return database.withTransaction {
            val local = tabla.buscar(idFila) ?: return@withTransaction false
            val elegida = if (idVersionElegida == null) {
                local
            } else {
                siblingDao.findByFila(entidad.textoGuardado(), idFila)
                    .firstOrNull { it.idVersion == idVersionElegida }
                    ?.let { tabla.desdeElSibling(it.contenido) }
                    ?: return@withTransaction false
            }
            tabla.guardar(tabla.resolviendo(local, elegida, nuevoIdVersion(), clock()))
            // Los Siblings quedan en su tabla, ocultos por `resolucion`, hasta que el servidor acepte la edición.
            database.resolucionDao().insert(idsResueltos.map { ResolucionEntity(it, entidad.textoGuardado(), idFila) })
            true
        }
    }

    // Cada tabla con revisiones es de su propia fila; aquí solo se usan sus operaciones, que no mezclan tipos.
    @Suppress("UNCHECKED_CAST")
    private fun tablaDe(entidad: EntidadConRevisiones) =
        tablas.first { it.entidad == entidad.textoGuardado() } as TablaConRevisiones<Any>

    // El contenido del Sibling es la fila que subió su autor: no trae `rev`, que `desdeElServidor` exige y
    // `resolviendo` reemplaza por el de la fila local. Nulo si le falta una columna o está mal escrito.
    private fun TablaConRevisiones<Any>.desdeElSibling(contenido: String): Any? = try {
        val fila: JsonObject = Json.parseToJsonElement(contenido).jsonObject
        desdeElServidor(JsonObject(fila + ("rev" to JsonPrimitive(0))))
    } catch (e: IllegalArgumentException) {
        // También cubre SerializationException, una fecha o un número mal escritos y un valor de otro tipo.
        null
    } catch (e: NoSuchElementException) {
        null
    }
}
