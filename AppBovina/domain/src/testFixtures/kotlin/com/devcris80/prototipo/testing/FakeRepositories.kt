package com.devcris80.prototipo.testing

import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Chapeta
import com.devcris80.prototipo.domain.model.Conciliacion
import com.devcris80.prototipo.domain.model.Conflicto
import com.devcris80.prototipo.domain.model.EntidadConRevisiones
import com.devcris80.prototipo.domain.model.Evento
import com.devcris80.prototipo.domain.model.PerfilFinca
import com.devcris80.prototipo.domain.model.PesajePendiente
import com.devcris80.prototipo.domain.model.Rechazo
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
import com.devcris80.prototipo.domain.repository.RechazoRepository
import com.devcris80.prototipo.domain.repository.RegistroRepository
import com.devcris80.prototipo.domain.repository.Invitacion
import com.devcris80.prototipo.domain.repository.InvitacionRepository
import com.devcris80.prototipo.domain.repository.OperarioRepository
import com.devcris80.prototipo.domain.repository.ResultadoListaOperarios
import com.devcris80.prototipo.domain.repository.ResultadoQuitarOperario
import com.devcris80.prototipo.domain.repository.ResultadoCreacionCuenta
import com.devcris80.prototipo.domain.repository.ResultadoInicioSesion
import com.devcris80.prototipo.domain.repository.ResultadoInvitacion
import com.devcris80.prototipo.domain.repository.ResultadoRecuperacion
import com.devcris80.prototipo.domain.repository.ResultadoSincronizacion
import com.devcris80.prototipo.domain.repository.SesionRepository
import com.devcris80.prototipo.domain.repository.SincronizacionRepository
import com.devcris80.prototipo.domain.repository.Transaccion
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Repositorios falsos en memoria, compartidos entre los tests de casos de uso de `:domain` y los de ViewModel de `:app`. */
class FakeAnimalRepository : AnimalRepository {
    val activos = MutableStateFlow<List<Animal>>(emptyList())
    private val porId = mutableMapOf<String, MutableStateFlow<Animal?>>()
    val bajas = mutableListOf<Pair<String, Long>>()
    val insertados = mutableListOf<Animal>()
    val actualizados = mutableListOf<Animal>()

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
    override suspend fun tieneCrias(idAnimal: String): Boolean =
        (activos.value + porId.values.mapNotNull { it.value })
            .any { it.idPadre == idAnimal || it.idMadre == idAnimal }
    override suspend fun update(animal: Animal) {
        actualizados.add(animal)
        set(animal)
    }
    override suspend fun darDeBaja(idAnimal: String, fecha: Long) {
        bajas.add(idAnimal to fecha)
        porId[idAnimal]?.let { it.value = it.value?.copy(fechaBaja = fecha) }
    }
}

class FakeChapetaRepository : ChapetaRepository {
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
    override suspend fun findActivaByAnimal(idAnimal: String): Chapeta? =
        todas.value.firstOrNull { it.idAnimal == idAnimal && it.fechaDesasociacion == null }
    override suspend fun liberar(idChapeta: String, fecha: Long) {
        liberadas.add(idChapeta)
        todas.value = todas.value.map { if (it.idChapeta == idChapeta) it.copy(fechaDesasociacion = fecha) else it }
        porAnimal.values.forEach { flow ->
            if (flow.value?.idChapeta == idChapeta) flow.value = flow.value?.copy(fechaDesasociacion = fecha)
        }
    }
}

class FakeCuentaRepository : CuentaRepository {
    val perfil = MutableStateFlow<PerfilFinca?>(null)
    val usuario = MutableStateFlow<Usuario?>(null)
    val guardadas = mutableListOf<PerfilFinca>()

    override fun observePerfilFinca(): Flow<PerfilFinca?> = perfil
    override suspend fun getPerfilFinca(): PerfilFinca? = perfil.value
    override suspend fun guardarPerfilFinca(perfil: PerfilFinca) {
        guardadas.add(perfil)
        this.perfil.value = perfil
    }
    override fun observeUsuario(): Flow<Usuario?> = usuario
    override suspend fun getUsuario(): Usuario? = usuario.value
}

class FakeRegistroRepository : RegistroRepository {
    private val porAnimal = mutableMapOf<String, MutableStateFlow<List<Registro>>>()

    fun set(idAnimal: String, registros: List<Registro>) {
        porAnimal.getOrPut(idAnimal) { MutableStateFlow(emptyList()) }.value = registros
    }

    fun setUltimo(idAnimal: String, registro: Registro?) = set(idAnimal, listOfNotNull(registro))

    override suspend fun insert(registro: Registro) {
        val actuales = porAnimal.getOrPut(registro.idAnimal) { MutableStateFlow(emptyList()) }
        actuales.value = listOf(registro) + actuales.value
    }
    override suspend fun findByIdLectura(idLectura: String): Registro? =
        porAnimal.values.flatMap { it.value }.firstOrNull { it.idLectura == idLectura }
    // Como el repositorio real: los dados de baja no salen, pero siguen guardados.
    override fun observeByAnimal(idAnimal: String): Flow<List<Registro>> =
        porAnimal.getOrPut(idAnimal) { MutableStateFlow(emptyList()) }
            .map { registros -> registros.filter { it.fechaBaja == null } }
    override fun observeUltimoByAnimal(idAnimal: String): Flow<Registro?> =
        observeByAnimal(idAnimal).map { it.firstOrNull() }
    override suspend fun findById(idRegistro: String): Registro? =
        porAnimal.values.flatMap { it.value }.firstOrNull { it.idRegistro == idRegistro }
    override suspend fun update(registro: Registro) = reemplazar(registro.idRegistro) {
        it.copy(peso = registro.peso, timestamp = registro.timestamp)
    }
    override suspend fun darDeBaja(idRegistro: String, fecha: Long) = reemplazar(idRegistro) { it.copy(fechaBaja = fecha) }

    private fun reemplazar(idRegistro: String, cambio: (Registro) -> Registro) {
        porAnimal.values.forEach { flow ->
            flow.value = flow.value.map { if (it.idRegistro == idRegistro) cambio(it) else it }
        }
    }
}

class FakeEventoRepository : EventoRepository {
    private val todos = MutableStateFlow<List<Evento>>(emptyList())

    fun set(idAnimal: String, eventos: List<Evento>) {
        todos.value = todos.value.filterNot { it.idAnimal == idAnimal } + eventos
    }

    override suspend fun insert(evento: Evento) {
        todos.value = listOf(evento) + todos.value
    }
    // Como el repositorio real: los dados de baja no salen, pero siguen guardados.
    override fun observeByAnimal(idAnimal: String): Flow<List<Evento>> =
        todos.map { lista -> lista.filter { it.idAnimal == idAnimal && it.fechaBaja == null } }
    override fun observeActivos(): Flow<List<Evento>> =
        todos.map { lista -> lista.filter { it.fechaBaja == null }.sortedByDescending { it.fecha } }
    override suspend fun findById(idEvento: String): Evento? = todos.value.firstOrNull { it.idEvento == idEvento }
    override suspend fun update(evento: Evento) = reemplazar(evento.idEvento) {
        it.copy(
            detalle = evento.detalle,
            fecha = evento.fecha,
            biologico = evento.biologico,
            dosis = evento.dosis,
            via = evento.via,
        )
    }
    override suspend fun darDeBaja(idEvento: String, fecha: Long) = reemplazar(idEvento) { it.copy(fechaBaja = fecha) }

    private fun reemplazar(idEvento: String, cambio: (Evento) -> Evento) {
        todos.value = todos.value.map { if (it.idEvento == idEvento) cambio(it) else it }
    }
}

class FakePesajePendienteRepository : PesajePendienteRepository {
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

/**
 * Ejecuta el bloque directo, sin revertir nada si falla: los tests de "todo o nada" solo comprueban que el caso de uso
 * valide antes de escribir. La reversión real de Room es de `RoomTransaccion`.
 */
class FakeTransaccion : Transaccion {
    override suspend fun <T> ejecutar(bloque: suspend () -> T): T = bloque()
}

class FakeSesionRepository : SesionRepository {
    val guardada = MutableStateFlow(false)
    var resultado: ResultadoInicioSesion = ResultadoInicioSesion.Exito
    val intentos = mutableListOf<Pair<String, String>>()
    var resultadoRecuperacion: ResultadoRecuperacion = ResultadoRecuperacion.Enviado
    val recuperaciones = mutableListOf<String>()

    /** Mientras no se complete, la recuperación queda en curso. */
    var pausaRecuperacion: CompletableDeferred<Unit>? = null

    override fun observeSesionGuardada(): Flow<Boolean> = guardada
    override suspend fun iniciarSesion(correo: String, contrasena: String): ResultadoInicioSesion {
        intentos.add(correo to contrasena)
        if (resultado == ResultadoInicioSesion.Exito) guardada.value = true
        return resultado
    }
    data class CreacionDeCuenta(
        val nombre: String,
        val nombreFinca: String,
        val correo: String,
        val contrasena: String,
        val versionAutorizacion: String,
    )

    var resultadoCreacion: ResultadoCreacionCuenta = ResultadoCreacionCuenta.Exito
    val creaciones = mutableListOf<CreacionDeCuenta>()

    override suspend fun crearCuenta(
        nombre: String,
        nombreFinca: String,
        correo: String,
        contrasena: String,
        versionAutorizacion: String,
    ): ResultadoCreacionCuenta {
        creaciones.add(CreacionDeCuenta(nombre, nombreFinca, correo, contrasena, versionAutorizacion))
        if (resultadoCreacion == ResultadoCreacionCuenta.Exito) guardada.value = true
        return resultadoCreacion
    }

    data class UnionAFinca(
        val nombre: String,
        val codigoInvitacion: String,
        val correo: String,
        val contrasena: String,
        val versionAutorizacion: String,
    )

    val uniones = mutableListOf<UnionAFinca>()

    override suspend fun unirseAFinca(
        nombre: String,
        codigoInvitacion: String,
        correo: String,
        contrasena: String,
        versionAutorizacion: String,
    ): ResultadoCreacionCuenta {
        uniones.add(UnionAFinca(nombre, codigoInvitacion, correo, contrasena, versionAutorizacion))
        if (resultadoCreacion == ResultadoCreacionCuenta.Exito) guardada.value = true
        return resultadoCreacion
    }
    override suspend fun recuperarContrasena(correo: String): ResultadoRecuperacion {
        recuperaciones.add(correo)
        pausaRecuperacion?.await()
        return resultadoRecuperacion
    }
    override suspend fun cerrarSesion() {
        guardada.value = false
    }
}

class FakeSincronizacionRepository : SincronizacionRepository {
    var resultado: ResultadoSincronizacion = ResultadoSincronizacion.Completa(0, 0, 0)
    var sincronizaciones = 0
    var sincronizacionesDeFotos = 0
    var programaciones = 0
    val estado = MutableStateFlow(EstadoSincronizacion(cambiosSinSubir = 0, ultimaSincronizacion = null))

    /** Si no es nulo, la subida espera a que se complete. */
    var espera: CompletableDeferred<Unit>? = null

    override suspend fun sincronizar(): ResultadoSincronizacion {
        sincronizaciones++
        espera?.await()
        return resultado
    }

    override suspend fun sincronizarFotos() {
        sincronizacionesDeFotos++
    }

    override fun programar() {
        programaciones++
    }

    override fun observeEstado(): Flow<EstadoSincronizacion> = estado
}

/** Conflictos en memoria. `resoluciones` guarda lo que pidió resolver, y la fila sale del conflicto como en Room. */
class FakeConflictoRepository : ConflictoRepository {
    data class Resolucion(
        val entidad: EntidadConRevisiones,
        val idFila: String,
        val idVersionElegida: String?,
        val idsResueltos: List<String>,
    )

    val conflictos = MutableStateFlow<List<Conflicto>>(emptyList())
    val resoluciones = mutableListOf<Resolucion>()

    /** Si es falso, `resolver` no escribe nada y avisa que la fila ya no existe. */
    var puedeResolver = true

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
    ): Boolean {
        if (!puedeResolver) return false
        resoluciones.add(Resolucion(entidad, idFila, idVersionElegida, idsResueltos))
        conflictos.value = conflictos.value.mapNotNull { conflicto ->
            if (!conflicto.es(entidad, idFila)) return@mapNotNull conflicto
            val quedan = conflicto.siblings.filterNot { it.sibling.idVersion in idsResueltos }
            conflicto.takeIf { quedan.isNotEmpty() }?.copy(siblings = quedan)
        }
        return true
    }
}

/** Rechazos en memoria. `cerrados` guarda los que se cerraron, que salen de los abiertos como en Room. */
class FakeRechazoRepository : RechazoRepository {
    private val abiertos = MutableStateFlow<List<Rechazo>>(emptyList())
    val cerrados = mutableListOf<String>()

    fun set(rechazo: Rechazo) {
        abiertos.value = abiertos.value.filterNot { it.idRechazo == rechazo.idRechazo } + rechazo
    }

    override fun observeAbiertos(): Flow<List<Rechazo>> = abiertos

    override suspend fun cerrar(idRechazo: String) {
        cerrados += idRechazo
        abiertos.value = abiertos.value.filterNot { it.idRechazo == idRechazo }
    }
}

class FakeInvitacionRepository : InvitacionRepository {
    var resultado: ResultadoInvitacion =
        ResultadoInvitacion.Generada(
            Invitacion("ABCD2345", 1_800_000_000_000),
        )
    var generaciones = 0

    /** Si no es nulo, generar espera a que se complete. */
    var espera: CompletableDeferred<Unit>? = null

    override suspend fun generar(): ResultadoInvitacion {
        generaciones++
        espera?.await()
        return resultado
    }
}

class FakeOperarioRepository : OperarioRepository {
    var lista: ResultadoListaOperarios = ResultadoListaOperarios.Lista(emptyList())
    var resultadoQuitar: ResultadoQuitarOperario = ResultadoQuitarOperario.Quitado
    val quitados = mutableListOf<String>()

    /** Si no es nulo, listar espera a que se complete. */
    var espera: CompletableDeferred<Unit>? = null

    override suspend fun listar(): ResultadoListaOperarios {
        espera?.await()
        return lista
    }

    override suspend fun quitar(idUsuario: String): ResultadoQuitarOperario {
        quitados += idUsuario
        return resultadoQuitar
    }
}
