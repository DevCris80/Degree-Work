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
            if (siblings.isEmpty()) return@map null
            val tabla = tablaDe(entidad)
            val vigente = tabla.buscar(idFila) ?: return@map null
            Conflicto(
                entidad = entidad,
                idFila = idFila,
                vigente = tabla.enConflicto(vigente),
                siblings = siblings.map { sibling ->
                    SiblingEnConflicto(sibling.toDomain(), tabla.enConflicto(tabla.desdeElSibling(sibling.contenido)))
                },
            )
        }

    override suspend fun resolver(
        entidad: EntidadConRevisiones,
        idFila: String,
        idVersionElegida: String?,
        idsResueltos: List<String>,
    ) {
        val tabla = tablaDe(entidad)
        database.withTransaction {
            val local = checkNotNull(tabla.buscar(idFila)) { "La fila $idFila ya no existe" }
            val elegida = if (idVersionElegida == null) {
                local
            } else {
                val sibling = siblingDao.findByFila(entidad.textoGuardado(), idFila).firstOrNull { it.idVersion == idVersionElegida }
                tabla.desdeElSibling(checkNotNull(sibling) { "El Sibling $idVersionElegida ya no está sin resolver" }.contenido)
            }
            tabla.guardar(tabla.resolviendo(local, elegida, nuevoIdVersion(), clock()))
            database.resolucionDao().insert(idsResueltos.map { ResolucionEntity(it, entidad.textoGuardado(), idFila) })
            siblingDao.deleteAll(idsResueltos)
        }
    }

    // Cada tabla con revisiones es de su propia fila; aquí solo se usan sus operaciones, que no mezclan tipos.
    @Suppress("UNCHECKED_CAST")
    private fun tablaDe(entidad: EntidadConRevisiones) =
        tablas.first { it.entidad == entidad.textoGuardado() } as TablaConRevisiones<Any>

    // El contenido del Sibling es la fila que subió su autor: no trae `rev`, que `desdeElServidor` exige y
    // `resolviendo` reemplaza por el de la fila local.
    private fun TablaConRevisiones<Any>.desdeElSibling(contenido: String): Any {
        val fila: JsonObject = Json.parseToJsonElement(contenido).jsonObject
        return desdeElServidor(JsonObject(fila + ("rev" to JsonPrimitive(0))))
    }
}
