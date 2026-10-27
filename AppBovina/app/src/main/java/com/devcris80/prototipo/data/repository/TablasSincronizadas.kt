package com.devcris80.prototipo.data.repository

import com.devcris80.prototipo.data.local.AppDatabase
import com.devcris80.prototipo.data.local.dao.ResolucionDao
import com.devcris80.prototipo.data.local.entity.AnimalEntity
import com.devcris80.prototipo.data.local.entity.ChapetaEntity
import com.devcris80.prototipo.data.local.entity.EventoEntity
import com.devcris80.prototipo.data.local.entity.PerfilFincaEntity
import com.devcris80.prototipo.data.local.entity.PesajePendienteEntity
import com.devcris80.prototipo.data.local.entity.RegistroEntity
import com.devcris80.prototipo.domain.model.FilaEnConflicto
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** Una fila que sale de su tabla junto con la que el servidor rechazó, porque depende de ella. */
internal data class Dependiente(val entidad: String, val idFila: String, val fila: JsonObject)

/** Lo que el motor necesita de cada tabla que sube y baja, con el nombre que tiene en el servidor. */
internal abstract class TablaSincronizada<E : Any>(val entidad: String) {
    /** Las filas con cambios por subir, en el orden en que suben. */
    abstract suspend fun pendientes(): List<E>
    abstract suspend fun buscar(id: String): E?
    abstract fun id(fila: E): String
    abstract fun sincronizada(fila: E): Boolean

    /** Distingue la escritura local que se envió de una posterior hecha mientras el lote viajaba. */
    abstract fun version(fila: E): Any
    abstract fun aFila(fila: E): JsonObject
    abstract fun conLaDelServidor(local: E, delServidor: JsonObject): E
    abstract suspend fun guardar(fila: E)

    /** Una fila que llega en la descarga y el celular no tiene. */
    abstract fun desdeElServidor(fila: JsonObject): E
    abstract suspend fun insertar(fila: E)

    /** Si la fila del servidor es al menos tan nueva como la local, ya sincronizada. */
    open fun alDia(delServidor: JsonObject, local: E): Boolean = true

    /**
     * Las otras filas locales que no dejan guardar [fila], la versión del servidor, por un índice único (el código
     * de una Chapeta activa, un `idLectura`). Una pendiente es una fila nueva que el servidor rechazaría al subirla,
     * por [motivoDelChoque]; una sincronizada está vieja, y la descarga trae su versión.
     */
    open suspend fun choques(fila: E): List<E> = emptyList()
    open val motivoDelChoque: String = ""

    /** Deja que [fila] tome el lugar de [choque], ya sincronizada, mientras llega la versión del servidor. */
    open suspend fun ceder(choque: E, fila: E): Unit =
        error("En $entidad, una fila sincronizada no choca con la del servidor: el servidor no tiene dos así")

    /** Saca de su tabla una fila que el servidor rechazó y no tiene, con sus dependientes (ADR-0004, regla 7). */
    abstract suspend fun sacar(fila: E): List<Dependiente>

    open suspend fun aCambio(fila: E): JsonObject = buildJsonObject {
        put("entidad", entidad)
        put("fila", aFila(fila))
    }
}

/** Animal, Evento y Registro: cada cambio viaja con su `idVersion` y la revisión de la que partió. */
internal abstract class TablaConRevisiones<E : Any>(
    entidad: String,
    private val resoluciones: ResolucionDao,
) : TablaSincronizada<E>(entidad) {
    abstract fun idVersion(fila: E): String
    abstract fun rev(fila: E): Int?
    abstract fun revBase(fila: E): Int?

    /** El servidor aceptó como [rev] una edición que se volvió a editar: la edición nueva parte de [rev]. */
    abstract suspend fun rebasar(id: String, rev: Int)

    override fun version(fila: E): Any = idVersion(fila)

    // Por el margen de solapamiento, una revisión puede volver a llegar; nunca una más vieja que la local.
    override fun alDia(delServidor: JsonObject, local: E): Boolean =
        delServidor.getValue("rev").jsonPrimitive.int >= (rev(local) ?: 0)

    /**
     * [local] con el contenido de [elegida] como una edición nueva sobre la revisión vigente (`revBase` es `rev`),
     * pendiente de subir (ADR-0004, regla 5). Una baja ya hecha se conserva: es definitiva.
     */
    abstract fun resolviendo(local: E, elegida: E, idVersion: String, fechaModificacion: Long): E

    /** La fila como la muestra un Conflicto. */
    abstract fun enConflicto(fila: E): FilaEnConflicto

    // Si la edición resuelve Siblings, los nombra para que el servidor los cierre.
    override suspend fun aCambio(fila: E): JsonObject {
        val resueltos = resoluciones.idsDeLaFila(entidad, id(fila))
        return buildJsonObject {
            put("entidad", entidad)
            put("id_version", idVersion(fila))
            put("rev_base", revBase(fila))
            put("fila", aFila(fila))
            if (resueltos.isNotEmpty()) put("resuelve", JsonArray(resueltos.map(::JsonPrimitive)))
        }
    }
}

/**
 * De padres a hijos: cada tabla sube y baja después de las tablas a las que apuntan sus filas. Una tabla nueva
 * también entra en el conteo de `EstadoSyncDao.observeCambiosSinSubir`.
 */
internal fun tablasSincronizadas(db: AppDatabase): List<TablaSincronizada<*>> {
    val eventos = TablaEventos(db)
    val registros = TablaRegistros(db)
    val chapetas = TablaChapetas(db)
    return listOf(
        TablaPerfilFinca(db),
        TablaAnimales(db, chapetas, eventos, registros),
        chapetas,
        eventos,
        registros,
        // Después de los Registros: uno asignado se rechaza si el servidor todavía no tiene su Registro.
        TablaPesajesPendientes(db),
    )
}

// Un cambio sin revisiones se distingue de uno posterior por su fecha de modificación.

private class TablaPerfilFinca(db: AppDatabase) : TablaSincronizada<PerfilFincaEntity>("perfil_finca") {
    private val dao = db.perfilFincaDao()

    override suspend fun pendientes() = dao.findPendientes()
    override suspend fun buscar(id: String) = dao.getOnce()?.takeIf { it.idPerfilFinca == id }
    override fun id(fila: PerfilFincaEntity) = fila.idPerfilFinca
    override fun sincronizada(fila: PerfilFincaEntity) = fila.sincronizado
    override fun version(fila: PerfilFincaEntity): Any = fila.fechaModificacion
    override fun aFila(fila: PerfilFincaEntity) = fila.aFilaDelServidor()
    override fun conLaDelServidor(local: PerfilFincaEntity, delServidor: JsonObject) = local.conLaDelServidor(delServidor)
    override suspend fun guardar(fila: PerfilFincaEntity) = dao.guardar(fila)
    override fun desdeElServidor(fila: JsonObject) = fila.aPerfilFincaEntity()
    override suspend fun insertar(fila: PerfilFincaEntity) = dao.guardar(fila)

    // La finca nunca sale de su tabla: el CASCADE se llevaría todo. El cambio queda en la bandeja y no se reenvía.
    override suspend fun sacar(fila: PerfilFincaEntity): List<Dependiente> {
        dao.guardar(fila.copy(sincronizado = true))
        return emptyList()
    }
}

private class TablaAnimales(
    db: AppDatabase,
    private val chapetas: TablaChapetas,
    private val eventos: TablaEventos,
    private val registros: TablaRegistros,
) : TablaConRevisiones<AnimalEntity>("animal", db.resolucionDao()) {
    private val dao = db.animalDao()

    // El padre y la madre suben antes que la cría, aunque su cambio sea más reciente.
    override suspend fun pendientes(): List<AnimalEntity> {
        val pendientes = dao.findPendientes()
        val porId = pendientes.associateBy { it.idAnimal }
        val orden = LinkedHashSet<AnimalEntity>()
        fun agregar(animal: AnimalEntity) {
            if (animal in orden) return
            listOfNotNull(animal.idPadre, animal.idMadre).mapNotNull(porId::get).forEach(::agregar)
            orden += animal
        }
        pendientes.forEach(::agregar)
        return orden.toList()
    }

    override suspend fun buscar(id: String) = dao.findById(id)
    override fun id(fila: AnimalEntity) = fila.idAnimal
    override fun sincronizada(fila: AnimalEntity) = fila.sincronizado
    override fun idVersion(fila: AnimalEntity) = fila.idVersion
    override fun rev(fila: AnimalEntity) = fila.rev
    override fun revBase(fila: AnimalEntity) = fila.revBase
    override fun aFila(fila: AnimalEntity) = fila.aFilaDelServidor()
    override fun conLaDelServidor(local: AnimalEntity, delServidor: JsonObject) = local.conLaDelServidor(delServidor)
    override suspend fun guardar(fila: AnimalEntity) = dao.update(fila)
    override fun desdeElServidor(fila: JsonObject) = fila.aAnimalEntity()
    override suspend fun insertar(fila: AnimalEntity) = dao.insert(fila)
    override suspend fun rebasar(id: String, rev: Int) = dao.rebasar(id, rev)
    override fun enConflicto(fila: AnimalEntity) = FilaEnConflicto.DeAnimal(fila.toDomain())

    // La foto no viaja en la fila: la del Animal local se queda.
    override fun resolviendo(local: AnimalEntity, elegida: AnimalEntity, idVersion: String, fechaModificacion: Long) =
        elegida.copy(
            fotoUri = local.fotoUri,
            fotoPorSubir = local.fotoPorSubir,
            fechaFotoServidor = local.fechaFotoServidor,
            fechaBaja = local.fechaBaja ?: elegida.fechaBaja,
            sincronizado = false,
            rev = local.rev,
            revBase = local.rev,
            idVersion = idVersion,
            fechaModificacion = fechaModificacion,
        )

    // Sin el Animal en el servidor, sus Chapetas, Eventos, Registros y crías tampoco pueden subir. Salen con él
    // en vez de que el CASCADE las borre, o que la cría pierda en silencio a su padre o su madre.
    override suspend fun sacar(fila: AnimalEntity): List<Dependiente> {
        val dependientes = mutableListOf<Dependiente>()
        for (cria in dao.findCrias(fila.idAnimal)) {
            dependientes += Dependiente(entidad, cria.idAnimal, cria.aFilaDelServidor())
            if (cria.rev == null) {
                dependientes += sacar(cria)
            } else {
                // Una cría que el servidor ya tiene se queda. Su cambio, que apunta a este Animal, va a la bandeja
                // y no se reenvía; al borrar este Animal, la fila local pierde la referencia (SET NULL).
                dao.update(cria.copy(sincronizado = true))
            }
        }
        dependientes += chapetas.sacarDelAnimal(fila.idAnimal)
        dependientes += eventos.sacarDelAnimal(fila.idAnimal)
        dependientes += registros.sacarDelAnimal(fila.idAnimal)
        dao.delete(fila.idAnimal)
        return dependientes
    }
}

private class TablaEventos(db: AppDatabase) : TablaConRevisiones<EventoEntity>("evento", db.resolucionDao()) {
    private val dao = db.eventoDao()

    override suspend fun pendientes() = dao.findPendientes()
    override suspend fun buscar(id: String) = dao.findById(id)
    override fun id(fila: EventoEntity) = fila.idEvento
    override fun sincronizada(fila: EventoEntity) = fila.sincronizado
    override fun idVersion(fila: EventoEntity) = fila.idVersion
    override fun rev(fila: EventoEntity) = fila.rev
    override fun revBase(fila: EventoEntity) = fila.revBase
    override fun aFila(fila: EventoEntity) = fila.aFilaDelServidor()
    override fun conLaDelServidor(local: EventoEntity, delServidor: JsonObject) = local.conLaDelServidor(delServidor)
    override suspend fun guardar(fila: EventoEntity) = dao.update(fila)
    override fun desdeElServidor(fila: JsonObject) = fila.aEventoEntity()
    override suspend fun insertar(fila: EventoEntity) = dao.insert(fila)
    override suspend fun rebasar(id: String, rev: Int) = dao.rebasar(id, rev)
    override fun enConflicto(fila: EventoEntity) = FilaEnConflicto.DeEvento(fila.toDomain())

    override fun resolviendo(local: EventoEntity, elegida: EventoEntity, idVersion: String, fechaModificacion: Long) =
        elegida.copy(
            fechaBaja = local.fechaBaja ?: elegida.fechaBaja,
            sincronizado = false,
            rev = local.rev,
            revBase = local.rev,
            idVersion = idVersion,
            fechaModificacion = fechaModificacion,
        )

    override suspend fun sacar(fila: EventoEntity): List<Dependiente> {
        dao.delete(fila.idEvento)
        return emptyList()
    }

    suspend fun sacarDelAnimal(idAnimal: String) = dao.findAllByAnimal(idAnimal).map {
        dao.delete(it.idEvento)
        Dependiente(entidad, it.idEvento, it.aFilaDelServidor())
    }
}

private class TablaRegistros(db: AppDatabase) : TablaConRevisiones<RegistroEntity>("registro", db.resolucionDao()) {
    private val dao = db.registroDao()

    override suspend fun pendientes() = dao.findPendientes()
    override suspend fun buscar(id: String) = dao.findById(id)
    override fun id(fila: RegistroEntity) = fila.idRegistro
    override fun sincronizada(fila: RegistroEntity) = fila.sincronizado
    override fun idVersion(fila: RegistroEntity) = fila.idVersion
    override fun rev(fila: RegistroEntity) = fila.rev
    override fun revBase(fila: RegistroEntity) = fila.revBase
    override fun aFila(fila: RegistroEntity) = fila.aFilaDelServidor()
    override fun conLaDelServidor(local: RegistroEntity, delServidor: JsonObject) = local.conLaDelServidor(delServidor)
    override suspend fun guardar(fila: RegistroEntity) = dao.update(fila)
    override fun desdeElServidor(fila: JsonObject) = fila.aRegistroEntity()
    override suspend fun insertar(fila: RegistroEntity) = dao.insert(fila)
    override suspend fun choques(fila: RegistroEntity) =
        listOfNotNull(fila.idLectura?.let { dao.findByIdLectura(it) }).filter { it.idRegistro != fila.idRegistro }
    override val motivoDelChoque = "Ya hay un pesaje con esta lectura en la finca"
    override suspend fun rebasar(id: String, rev: Int) = dao.rebasar(id, rev)
    override fun enConflicto(fila: RegistroEntity) = FilaEnConflicto.DeRegistro(fila.toDomain())

    override fun resolviendo(local: RegistroEntity, elegida: RegistroEntity, idVersion: String, fechaModificacion: Long) =
        elegida.copy(
            fechaBaja = local.fechaBaja ?: elegida.fechaBaja,
            sincronizado = false,
            rev = local.rev,
            revBase = local.rev,
            idVersion = idVersion,
            fechaModificacion = fechaModificacion,
        )

    override suspend fun sacar(fila: RegistroEntity): List<Dependiente> {
        dao.delete(fila.idRegistro)
        return emptyList()
    }

    suspend fun sacarDelAnimal(idAnimal: String) = dao.findAllByAnimal(idAnimal).map {
        dao.delete(it.idRegistro)
        Dependiente(entidad, it.idRegistro, it.aFilaDelServidor())
    }
}

private class TablaChapetas(db: AppDatabase) : TablaSincronizada<ChapetaEntity>("chapeta") {
    private val dao = db.chapetaDao()

    override suspend fun pendientes() = dao.findPendientes()
    override suspend fun buscar(id: String) = dao.findById(id)
    override fun id(fila: ChapetaEntity) = fila.idChapeta
    override fun sincronizada(fila: ChapetaEntity) = fila.sincronizado
    override fun version(fila: ChapetaEntity): Any = fila.fechaModificacion
    override fun aFila(fila: ChapetaEntity) = fila.aFilaDelServidor()
    override fun conLaDelServidor(local: ChapetaEntity, delServidor: JsonObject) = local.conLaDelServidor(delServidor)
    override suspend fun guardar(fila: ChapetaEntity) = dao.update(fila)
    override fun desdeElServidor(fila: JsonObject) = fila.aChapetaEntity()
    override suspend fun insertar(fila: ChapetaEntity) = dao.insert(fila)
    override suspend fun choques(fila: ChapetaEntity) =
        if (fila.fechaDesasociacion != null) {
            emptyList()
        } else {
            listOfNotNull(dao.findActivaByCodigo(fila.codigo)).filter { it.idChapeta != fila.idChapeta }
        }
    override val motivoDelChoque = "Ya hay una Chapeta activa con este código en la finca"

    // El servidor no deja dos Chapetas activas con el mismo código: la local ya se liberó allá antes de la otra.
    override suspend fun ceder(choque: ChapetaEntity, fila: ChapetaEntity) =
        dao.update(choque.copy(fechaDesasociacion = fila.fechaAsociacion))

    override suspend fun sacar(fila: ChapetaEntity): List<Dependiente> {
        dao.delete(fila.idChapeta)
        return emptyList()
    }

    suspend fun sacarDelAnimal(idAnimal: String) = dao.findAllByAnimal(idAnimal).map {
        dao.delete(it.idChapeta)
        Dependiente(entidad, it.idChapeta, it.aFilaDelServidor())
    }
}

private class TablaPesajesPendientes(db: AppDatabase) : TablaSincronizada<PesajePendienteEntity>("pesaje_pendiente") {
    private val dao = db.pesajePendienteDao()

    override suspend fun pendientes() = dao.findPendientes()
    override suspend fun buscar(id: String) = dao.findById(id)
    override fun id(fila: PesajePendienteEntity) = fila.idPesajePendiente
    override fun sincronizada(fila: PesajePendienteEntity) = fila.sincronizado
    override fun version(fila: PesajePendienteEntity): Any = fila.fechaModificacion
    override fun aFila(fila: PesajePendienteEntity) = fila.aFilaDelServidor()
    override fun conLaDelServidor(local: PesajePendienteEntity, delServidor: JsonObject) =
        local.conLaDelServidor(delServidor)
    override suspend fun guardar(fila: PesajePendienteEntity) = dao.update(fila)
    override fun desdeElServidor(fila: JsonObject) = fila.aPesajePendienteEntity()
    override suspend fun insertar(fila: PesajePendienteEntity) = dao.insert(fila)
    override suspend fun choques(fila: PesajePendienteEntity) =
        listOfNotNull(dao.findByIdLectura(fila.idLectura)).filter { it.idPesajePendiente != fila.idPesajePendiente }
    override val motivoDelChoque = "Ya hay un pesaje pendiente con esta lectura en la finca"

    override suspend fun sacar(fila: PesajePendienteEntity): List<Dependiente> {
        dao.delete(fila.idPesajePendiente)
        return emptyList()
    }
}
