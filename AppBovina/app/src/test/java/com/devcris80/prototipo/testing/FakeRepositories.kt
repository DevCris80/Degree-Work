package com.devcris80.prototipo.testing

import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Chapeta
import com.devcris80.prototipo.domain.model.Conciliacion
import com.devcris80.prototipo.domain.model.Conflicto
import com.devcris80.prototipo.domain.model.EntidadConRevisiones
import com.devcris80.prototipo.domain.model.Evento
import com.devcris80.prototipo.domain.model.PerfilFinca
import com.devcris80.prototipo.domain.model.PesajePendiente
import com.devcris80.prototipo.domain.model.Registro
import com.devcris80.prototipo.domain.model.Sexo
import com.devcris80.prototipo.domain.model.Sibling
import com.devcris80.prototipo.domain.model.Usuario
import com.devcris80.prototipo.domain.repository.AnimalRepository
import com.devcris80.prototipo.domain.repository.ChapetaRepository
import com.devcris80.prototipo.domain.repository.ConflictoRepository
import com.devcris80.prototipo.domain.repository.CuentaRepository
import com.devcris80.prototipo.domain.repository.EstadoSincronizacion
import com.devcris80.prototipo.domain.repository.EventoRepository
import com.devcris80.prototipo.domain.repository.PesajePendienteRepository
import com.devcris80.prototipo.domain.repository.RegistroRepository
import com.devcris80.prototipo.domain.repository.ResultadoInicioSesion
import com.devcris80.prototipo.domain.repository.ResultadoSincronizacion
import com.devcris80.prototipo.domain.repository.SesionRepository
import com.devcris80.prototipo.domain.repository.SincronizacionRepository
import com.devcris80.prototipo.domain.repository.Transaccion
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.UUID

/** Repositorios falsos en memoria, compartidos entre los tests de ViewModel de `ui.screens`. */
internal class FakeAnimalRepository : AnimalRepository {
    val activos = MutableStateFlow<List<Animal>>(emptyList())
    private val porId = mutableMapOf<String, MutableStateFlow<Animal?>>()
    val bajas = mutableListOf<Pair<String, Long>>()
    val insertados = mutableListOf<Animal>()

    fun set(animal: Animal) {
        porId.getOrPut(animal.idAnimal) { MutableStateFlow(null) }.value = animal
    }

    override suspend fun insert(animal: Animal) {
        insertados.add(animal)
        set(animal)
    }
    override fun observeActivos(): Flow<List<Animal>> = activos
    // Como el repositorio real: también los dados de baja, primero los activos y cada grupo por nombre.
    override fun observePorSexo(sexo: Sexo): Flow<List<Animal>> =
        activos.map { animales ->
            (animales + porId.values.mapNotNull { it.value })
                .distinctBy { it.idAnimal }
                .filter { it.sexo == sexo }
                .sortedWith(compareBy({ it.fechaBaja != null }, { it.nombre }))
        }
    override fun observeById(idAnimal: String): Flow<Animal?> =
        porId.getOrPut(idAnimal) { MutableStateFlow(null) }
    override suspend fun findById(idAnimal: String): Animal? =
        porId[idAnimal]?.value ?: activos.value.firstOrNull { it.idAnimal == idAnimal }
    override suspend fun darDeBaja(idAnimal: String, fecha: Long) {
        bajas.add(idAnimal to fecha)
        porId[idAnimal]?.let { it.value = it.value?.copy(fechaBaja = fecha) }
    }
}

internal class FakeChapetaRepository : ChapetaRepository {
    private val todas = MutableStateFlow<List<Chapeta>>(emptyList())
    private val porAnimal = mutableMapOf<String, MutableStateFlow<Chapeta?>>()
    val liberadas = mutableListOf<String>()

    fun set(chapeta: Chapeta) {
        todas.value = todas.value.filterNot { it.idChapeta == chapeta.idChapeta } + chapeta
        if (chapeta.fechaDesasociacion == null) {
            porAnimal.getOrPut(chapeta.idAnimal) { MutableStateFlow(null) }.value = chapeta
        }
    }

    fun chapeta(idChapeta: String): Chapeta? = todas.value.firstOrNull { it.idChapeta == idChapeta }

    override suspend fun insert(chapeta: Chapeta) = set(chapeta)
    override suspend fun findActivaByCodigo(codigo: String): Chapeta? =
        todas.value.firstOrNull { it.codigo == codigo && it.fechaDesasociacion == null }
    override suspend fun findUltimaByCodigo(codigo: String): Chapeta? =
        todas.value.filter { it.codigo == codigo }.maxByOrNull { it.fechaAsociacion }
    override fun observeActivas(): Flow<List<Chapeta>> =
        todas.map { lista -> lista.filter { it.fechaDesasociacion == null } }
    override fun observeActivaByAnimal(idAnimal: String): Flow<Chapeta?> =
        porAnimal.getOrPut(idAnimal) {
            MutableStateFlow(todas.value.firstOrNull { it.idAnimal == idAnimal && it.fechaDesasociacion == null })
        }
    override suspend fun liberar(idChapeta: String, fecha: Long) {
        liberadas.add(idChapeta)
        todas.value = todas.value.map { if (it.idChapeta == idChapeta) it.copy(fechaDesasociacion = fecha) else it }
        porAnimal.values.forEach { flow ->
            if (flow.value?.idChapeta == idChapeta) flow.value = flow.value?.copy(fechaDesasociacion = fecha)
        }
    }
}

internal class FakeCuentaRepository : CuentaRepository {
    val perfil = MutableStateFlow<PerfilFinca?>(null)
    val usuario = MutableStateFlow<Usuario?>(null)

    override fun observePerfilFinca(): Flow<PerfilFinca?> = perfil
    override suspend fun getPerfilFinca(): PerfilFinca? = perfil.value
    override suspend fun crearPerfilFinca(nombreFinca: String): PerfilFinca {
        val nuevo = PerfilFinca(idPerfilFinca = UUID.randomUUID().toString(), nombreFinca = nombreFinca)
        perfil.value = nuevo
        return nuevo
    }
    override fun observeUsuario(): Flow<Usuario?> = usuario
    override suspend fun getUsuario(): Usuario? = usuario.value
    override suspend fun insertUsuario(usuario: Usuario) {
        this.usuario.value = usuario
    }
}

internal class FakeRegistroRepository : RegistroRepository {
    private val porAnimal = mutableMapOf<String, MutableStateFlow<List<Registro>>>()

    fun set(idAnimal: String, registros: List<Registro>) {
        porAnimal.getOrPut(idAnimal) { MutableStateFlow(emptyList()) }.value = registros
    }

    fun setUltimo(idAnimal: String, registro: Registro?) = set(idAnimal, listOfNotNull(registro))

    override suspend fun insert(registro: Registro) {
        val actuales = porAnimal.getOrPut(registro.idAnimal) { MutableStateFlow(emptyList()) }
        actuales.value = listOf(registro) + actuales.value
    }
    override suspend fun findByIdLectura(idLectura: String): Registro? = null
    override fun observeByAnimal(idAnimal: String): Flow<List<Registro>> =
        porAnimal.getOrPut(idAnimal) { MutableStateFlow(emptyList()) }
    override fun observeUltimoByAnimal(idAnimal: String): Flow<Registro?> =
        observeByAnimal(idAnimal).map { it.firstOrNull() }
}

internal class FakeEventoRepository : EventoRepository {
    private val todos = MutableStateFlow<List<Evento>>(emptyList())

    fun set(idAnimal: String, eventos: List<Evento>) {
        todos.value = todos.value.filterNot { it.idAnimal == idAnimal } + eventos
    }

    override suspend fun insert(evento: Evento) {
        todos.value = listOf(evento) + todos.value
    }
    override fun observeByAnimal(idAnimal: String): Flow<List<Evento>> =
        todos.map { lista -> lista.filter { it.idAnimal == idAnimal } }
    override fun observeActivos(): Flow<List<Evento>> =
        todos.map { lista -> lista.filter { it.fechaBaja == null }.sortedByDescending { it.fecha } }
}

internal class FakePesajePendienteRepository : PesajePendienteRepository {
    val todos = MutableStateFlow<List<PesajePendiente>>(emptyList())

    fun set(pesajePendiente: PesajePendiente) {
        todos.value = todos.value.filterNot { it.idPesajePendiente == pesajePendiente.idPesajePendiente } + pesajePendiente
    }

    override suspend fun insert(pesajePendiente: PesajePendiente) = set(pesajePendiente)
    override suspend fun findById(idPesajePendiente: String): PesajePendiente? =
        todos.value.firstOrNull { it.idPesajePendiente == idPesajePendiente }
    override suspend fun findByIdLectura(idLectura: String): PesajePendiente? =
        todos.value.firstOrNull { it.idLectura == idLectura }
    override fun observeSinResolver(): Flow<List<PesajePendiente>> =
        todos.map { lista -> lista.filter { it.conciliacion == null }.sortedByDescending { it.timestamp } }
    override suspend fun findSinResolverByCodigo(codigo: String): List<PesajePendiente> =
        todos.value.filter { it.codigo == codigo && it.conciliacion == null }.sortedByDescending { it.timestamp }
    override suspend fun conciliar(idPesajePendiente: String, conciliacion: Conciliacion) {
        todos.value.firstOrNull { it.idPesajePendiente == idPesajePendiente }?.let { set(it.copy(conciliacion = conciliacion)) }
    }
}

/** Ejecuta el bloque directo, sin control de atomicidad real: alcanza para probar el resultado. */
internal class FakeTransaccion : Transaccion {
    override suspend fun <T> ejecutar(bloque: suspend () -> T): T = bloque()
}

internal class FakeSesionRepository : SesionRepository {
    val guardada = MutableStateFlow(false)
    var resultado: ResultadoInicioSesion = ResultadoInicioSesion.Exito
    val intentos = mutableListOf<Pair<String, String>>()

    override fun observeSesionGuardada(): Flow<Boolean> = guardada
    override suspend fun iniciarSesion(correo: String, contrasena: String): ResultadoInicioSesion {
        intentos.add(correo to contrasena)
        if (resultado == ResultadoInicioSesion.Exito) guardada.value = true
        return resultado
    }
    override suspend fun cerrarSesion() {
        guardada.value = false
    }
}

internal class FakeSincronizacionRepository : SincronizacionRepository {
    var resultado: ResultadoSincronizacion = ResultadoSincronizacion.Completa(0, 0, 0)
    var sincronizaciones = 0
    var programaciones = 0
    val estado = MutableStateFlow(EstadoSincronizacion(cambiosSinSubir = 0, ultimaSincronizacion = null))

    /** Si no es nulo, la subida espera a que se complete. */
    var espera: CompletableDeferred<Unit>? = null

    override suspend fun sincronizar(): ResultadoSincronizacion {
        sincronizaciones++
        espera?.await()
        return resultado
    }

    override fun programar() {
        programaciones++
    }

    override fun observeEstado(): Flow<EstadoSincronizacion> = estado
}

/** Conflictos en memoria. `resoluciones` guarda lo que pidió resolver, y la fila sale del conflicto como en Room. */
internal class FakeConflictoRepository : ConflictoRepository {
    data class Resolucion(
        val entidad: EntidadConRevisiones,
        val idFila: String,
        val idVersionElegida: String?,
        val idsResueltos: List<String>,
    )

    val conflictos = MutableStateFlow<List<Conflicto>>(emptyList())
    val resoluciones = mutableListOf<Resolucion>()

    private fun Conflicto.es(entidad: EntidadConRevisiones, idFila: String) =
        this.entidad == entidad && this.idFila == idFila

    override fun observeEnConflicto(entidad: EntidadConRevisiones, idFila: String): Flow<Boolean> =
        conflictos.map { lista -> lista.any { it.es(entidad, idFila) } }

    override fun observeFilasEnConflicto(entidad: EntidadConRevisiones): Flow<Set<String>> =
        conflictos.map { lista -> lista.filter { it.entidad == entidad }.map { it.idFila }.toSet() }

    override fun observeSiblings(entidad: EntidadConRevisiones, idFila: String): Flow<List<Sibling>> =
        conflictos.map { lista -> lista.firstOrNull { it.es(entidad, idFila) }?.siblings?.map { it.sibling }.orEmpty() }

    override fun observeConflicto(entidad: EntidadConRevisiones, idFila: String): Flow<Conflicto?> =
        conflictos.map { lista -> lista.firstOrNull { it.es(entidad, idFila) } }

    override suspend fun resolver(
        entidad: EntidadConRevisiones,
        idFila: String,
        idVersionElegida: String?,
        idsResueltos: List<String>,
    ) {
        resoluciones.add(Resolucion(entidad, idFila, idVersionElegida, idsResueltos))
        conflictos.value = conflictos.value.mapNotNull { conflicto ->
            if (!conflicto.es(entidad, idFila)) return@mapNotNull conflicto
            val quedan = conflicto.siblings.filterNot { it.sibling.idVersion in idsResueltos }
            conflicto.takeIf { quedan.isNotEmpty() }?.copy(siblings = quedan)
        }
    }
}
