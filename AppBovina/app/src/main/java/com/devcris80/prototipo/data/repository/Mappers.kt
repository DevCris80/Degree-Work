package com.devcris80.prototipo.data.repository

import com.devcris80.prototipo.data.local.entity.AnimalEntity
import com.devcris80.prototipo.data.local.entity.ChapetaEntity
import com.devcris80.prototipo.data.local.entity.EventoEntity
import com.devcris80.prototipo.data.local.entity.PerfilFincaEntity
import com.devcris80.prototipo.data.local.entity.PesajePendienteEntity
import com.devcris80.prototipo.data.local.entity.RechazoEntity
import com.devcris80.prototipo.data.local.entity.RegistroEntity
import com.devcris80.prototipo.data.local.entity.SiblingEntity
import com.devcris80.prototipo.data.local.entity.UsuarioEntity
import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Chapeta
import com.devcris80.prototipo.domain.model.Conciliacion
import com.devcris80.prototipo.domain.model.EntidadConRevisiones
import com.devcris80.prototipo.domain.model.EntidadSincronizada
import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.Evento
import com.devcris80.prototipo.domain.model.MotivoPesajePendiente
import com.devcris80.prototipo.domain.model.PerfilFinca
import com.devcris80.prototipo.domain.model.PesajePendiente
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.model.Rechazo
import com.devcris80.prototipo.domain.model.Registro
import com.devcris80.prototipo.domain.model.ResolucionPesajePendiente
import com.devcris80.prototipo.domain.model.Rol
import com.devcris80.prototipo.domain.model.Sexo
import com.devcris80.prototipo.domain.model.Sibling
import com.devcris80.prototipo.domain.model.Usuario
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.float
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

// Los enum del dominio se guardan en Room como texto. Sexo, Etapa, Proposito y Rol usan el mismo
// que ya escribían las pantallas.

private val TEXTO_SEXO = mapOf(Sexo.MACHO to "Macho", Sexo.HEMBRA to "Hembra")
private val TEXTO_ETAPA = mapOf(
    Etapa.TORO to "Toro",
    Etapa.NOVILLO to "Novillo",
    Etapa.VACA to "Vaca",
    Etapa.TERNERA to "Ternera",
)
private val TEXTO_PROPOSITO = mapOf(
    Proposito.LECHE to "Leche",
    Proposito.CARNE to "Carne",
    Proposito.DOBLE_PROPOSITO to "Doble Propósito",
)
private val TEXTO_ROL = mapOf(Rol.GANADERO to "Ganadero", Rol.OPERARIO to "Operario")
private val TEXTO_MOTIVO_PESAJE_PENDIENTE = mapOf(
    MotivoPesajePendiente.CODIGO_SIN_CHAPETA_ACTIVA to "CODIGO_SIN_CHAPETA_ACTIVA",
    MotivoPesajePendiente.ANIMAL_DADO_DE_BAJA to "ANIMAL_DADO_DE_BAJA",
)
private val TEXTO_RESOLUCION_PESAJE_PENDIENTE = mapOf(
    ResolucionPesajePendiente.ASIGNADO to "ASIGNADO",
    ResolucionPesajePendiente.DESCARTADO to "DESCARTADO",
)

// Las entidades de Siblings y Rechazos se guardan con el nombre de su tabla en el servidor. La foto no tiene tabla:
// es el archivo de un Animal en Storage.
internal const val ENTIDAD_FOTO = "foto"

private val TEXTO_ENTIDAD_SINCRONIZADA = mapOf(
    EntidadSincronizada.PERFIL_FINCA to "perfil_finca",
    EntidadSincronizada.ANIMAL to "animal",
    EntidadSincronizada.EVENTO to "evento",
    EntidadSincronizada.REGISTRO to "registro",
    EntidadSincronizada.CHAPETA to "chapeta",
    EntidadSincronizada.PESAJE_PENDIENTE to "pesaje_pendiente",
    EntidadSincronizada.FOTO to ENTIDAD_FOTO,
)
private val TEXTO_ENTIDAD_CON_REVISIONES = mapOf(
    EntidadConRevisiones.ANIMAL to "animal",
    EntidadConRevisiones.EVENTO to "evento",
    EntidadConRevisiones.REGISTRO to "registro",
)

// Conciliar escribe la resolución sola, sin pasar por toEntity.
internal fun ResolucionPesajePendiente.textoGuardado(): String = TEXTO_RESOLUCION_PESAJE_PENDIENTE.aTexto(this)

internal fun EntidadConRevisiones.textoGuardado(): String = TEXTO_ENTIDAD_CON_REVISIONES.aTexto(this)

// Filtrar animales por sexo compara contra el texto guardado.
internal fun Sexo.textoGuardado(): String = TEXTO_SEXO.aTexto(this)

private fun <E : Enum<E>> Map<E, String>.aTexto(valor: E): String = getValue(valor)

private fun <E : Enum<E>> Map<E, String>.desdeTexto(texto: String): E =
    entries.firstOrNull { it.value == texto }?.key
        ?: error("Valor guardado desconocido: \"$texto\" (esperado uno de $values)")

fun AnimalEntity.toDomain() = Animal(
    idAnimal = idAnimal,
    idPerfilFinca = idPerfilFinca,
    nombre = nombre,
    raza = raza,
    sexo = TEXTO_SEXO.desdeTexto(sexo),
    etapa = TEXTO_ETAPA.desdeTexto(etapa),
    fechaNacimiento = fechaNacimiento,
    fechaNacimientoEsEstimada = fechaNacimientoEsEstimada,
    proposito = TEXTO_PROPOSITO.desdeTexto(proposito),
    fotoUri = fotoUri,
    fechaBaja = fechaBaja,
    idPadre = idPadre,
    idMadre = idMadre,
)

fun Animal.toEntity(fechaModificacion: Long, idVersion: String) = AnimalEntity(
    idAnimal = idAnimal,
    idPerfilFinca = idPerfilFinca,
    nombre = nombre,
    raza = raza,
    sexo = TEXTO_SEXO.aTexto(sexo),
    etapa = TEXTO_ETAPA.aTexto(etapa),
    fechaNacimiento = fechaNacimiento,
    fechaNacimientoEsEstimada = fechaNacimientoEsEstimada,
    proposito = TEXTO_PROPOSITO.aTexto(proposito),
    fotoUri = fotoUri,
    fechaBaja = fechaBaja,
    idPadre = idPadre,
    idMadre = idMadre,
    fechaModificacion = fechaModificacion,
    idVersion = idVersion,
    // Un Animal nuevo con foto la deja por subir. Al editar, lo decide `AnimalDao.editar`.
    fotoPorSubir = fotoUri != null,
)

fun ChapetaEntity.toDomain() = Chapeta(
    idChapeta = idChapeta,
    codigo = codigo,
    idAnimal = idAnimal,
    fechaAsociacion = fechaAsociacion,
    fechaDesasociacion = fechaDesasociacion,
)

fun Chapeta.toEntity(fechaModificacion: Long) = ChapetaEntity(
    idChapeta = idChapeta,
    codigo = codigo,
    idAnimal = idAnimal,
    fechaAsociacion = fechaAsociacion,
    fechaDesasociacion = fechaDesasociacion,
    fechaModificacion = fechaModificacion,
)

fun EventoEntity.toDomain() = Evento(
    idEvento = idEvento,
    idAnimal = idAnimal,
    idUsuario = idUsuario,
    tipoEvento = tipoEvento,
    fecha = fecha,
    detalle = detalle,
    fechaBaja = fechaBaja,
)

fun Evento.toEntity(fechaModificacion: Long, idVersion: String) = EventoEntity(
    idEvento = idEvento,
    idAnimal = idAnimal,
    idUsuario = idUsuario,
    tipoEvento = tipoEvento,
    fecha = fecha,
    detalle = detalle,
    fechaBaja = fechaBaja,
    fechaModificacion = fechaModificacion,
    idVersion = idVersion,
)

fun RegistroEntity.toDomain() = Registro(
    idRegistro = idRegistro,
    idAnimal = idAnimal,
    idUsuario = idUsuario,
    idLectura = idLectura,
    peso = peso,
    timestamp = timestamp,
    fechaBaja = fechaBaja,
)

fun Registro.toEntity(fechaModificacion: Long, idVersion: String) = RegistroEntity(
    idRegistro = idRegistro,
    idAnimal = idAnimal,
    idUsuario = idUsuario,
    idLectura = idLectura,
    peso = peso,
    timestamp = timestamp,
    fechaBaja = fechaBaja,
    fechaModificacion = fechaModificacion,
    idVersion = idVersion,
)

fun PesajePendienteEntity.toDomain() = PesajePendiente(
    idPesajePendiente = idPesajePendiente,
    idLectura = idLectura,
    codigo = codigo,
    peso = peso,
    timestamp = timestamp,
    idUsuario = idUsuario,
    motivo = TEXTO_MOTIVO_PESAJE_PENDIENTE.desdeTexto(motivo),
    conciliacion = resolucion?.let {
        Conciliacion(
            resolucion = TEXTO_RESOLUCION_PESAJE_PENDIENTE.desdeTexto(it),
            idUsuario = checkNotNull(idUsuarioResolucion) { "Pesaje pendiente resuelto sin Usuario" },
            fecha = checkNotNull(fechaResolucion) { "Pesaje pendiente resuelto sin fecha" },
        )
    },
)

fun PesajePendiente.toEntity(fechaModificacion: Long) = PesajePendienteEntity(
    idPesajePendiente = idPesajePendiente,
    idLectura = idLectura,
    codigo = codigo,
    peso = peso,
    timestamp = timestamp,
    idUsuario = idUsuario,
    motivo = TEXTO_MOTIVO_PESAJE_PENDIENTE.aTexto(motivo),
    resolucion = conciliacion?.resolucion?.textoGuardado(),
    idUsuarioResolucion = conciliacion?.idUsuario,
    fechaResolucion = conciliacion?.fecha,
    fechaModificacion = fechaModificacion,
)

fun PerfilFincaEntity.toDomain() = PerfilFinca(
    idPerfilFinca = idPerfilFinca,
    nombreFinca = nombreFinca,
)

fun UsuarioEntity.toDomain() = Usuario(
    idUsuario = idUsuario,
    idPerfilFinca = idPerfilFinca,
    nombre = nombre,
    rol = TEXTO_ROL.desdeTexto(rol),
)

fun SiblingEntity.toDomain() = Sibling(
    idVersion = idVersion,
    entidad = TEXTO_ENTIDAD_CON_REVISIONES.desdeTexto(entidad),
    idFila = idFila,
    revBase = revBase,
    contenido = contenido,
    idUsuario = idUsuario,
    fechaRecepcion = fechaRecepcion,
)

private val CON_CODIGO = setOf(EntidadSincronizada.CHAPETA, EntidadSincronizada.PESAJE_PENDIENTE)

// El motivo empieza con el código de error (BandejaDeRechazos), y el contenido es lo que se intentó subir, con las
// columnas del servidor: todas las filas de un Animal, y su foto, llevan `id_animal`. Un contenido que no se puede
// leer no deja al Rechazo fuera de la bandeja: sale sin esos datos.
fun RechazoEntity.toDomain(): Rechazo {
    val entidadSincronizada = TEXTO_ENTIDAD_SINCRONIZADA.desdeTexto(entidad)
    val fila = runCatching { Json.parseToJsonElement(contenido).jsonObject }.getOrNull()
    return Rechazo(
        idRechazo = idRechazo,
        entidad = entidadSincronizada,
        idFila = idFila,
        contenido = contenido,
        motivo = motivo,
        fechaRechazo = fechaRechazo,
        codigoError = motivo.substringBefore(":", missingDelimiterValue = ""),
        mensaje = motivo.substringAfter(": "),
        idAnimal = fila?.textoONulo("id_animal"),
        nombreAnimal = fila?.textoONulo("nombre").takeIf { entidadSincronizada == EntidadSincronizada.ANIMAL },
        codigo = fila?.textoONulo("codigo").takeIf { entidadSincronizada in CON_CODIGO },
        peso = fila?.get("peso")?.jsonPrimitive?.doubleOrNull,
        salioConSuAnimal = esDeUnDependiente(motivo),
    )
}

// Filas que llegan del servidor por PostgREST, con sus columnas en snake_case. Ya están sincronizadas.

fun JsonObject.aPerfilFincaEntity() = PerfilFincaEntity(
    idPerfilFinca = texto("id_perfil_finca"),
    nombreFinca = texto("nombre_finca"),
    sincronizado = true,
    fechaModificacion = fecha("fecha_modificacion"),
)

// El rol pasa por TEXTO_ROL para rechazar un valor que la app no conoce.
fun JsonObject.aUsuarioEntity(delDispositivo: Boolean) = UsuarioEntity(
    idUsuario = texto("id_usuario"),
    idPerfilFinca = texto("id_perfil_finca"),
    nombre = texto("nombre"),
    rol = TEXTO_ROL.aTexto(TEXTO_ROL.desdeTexto(texto("rol"))),
    sincronizado = true,
    fechaModificacion = fecha("fecha_modificacion"),
    delDispositivo = delDispositivo,
)

// Filas que sube el celular, con las columnas de su tabla en el servidor (supabase/README.md). Las tablas
// hijas no envían `id_perfil_finca`, que llena el servidor, y la foto no viaja con el Animal.

fun PerfilFincaEntity.aFilaDelServidor() = buildJsonObject {
    put("id_perfil_finca", idPerfilFinca)
    put("nombre_finca", nombreFinca)
    putFecha("fecha_modificacion", fechaModificacion)
}

fun AnimalEntity.aFilaDelServidor() = buildJsonObject {
    put("id_animal", idAnimal)
    put("id_perfil_finca", idPerfilFinca)
    put("id_padre", idPadre)
    put("id_madre", idMadre)
    put("nombre", nombre)
    put("raza", raza)
    put("sexo", sexo)
    put("etapa", etapa)
    putFecha("fecha_nacimiento", fechaNacimiento)
    put("fecha_nacimiento_es_estimada", fechaNacimientoEsEstimada)
    put("proposito", proposito)
    putFecha("fecha_baja", fechaBaja)
    putFecha("fecha_modificacion", fechaModificacion)
}

fun EventoEntity.aFilaDelServidor() = buildJsonObject {
    put("id_evento", idEvento)
    put("id_animal", idAnimal)
    put("id_usuario", idUsuario)
    put("tipo_evento", tipoEvento)
    putFecha("fecha", fecha)
    put("detalle", detalle)
    putFecha("fecha_baja", fechaBaja)
    putFecha("fecha_modificacion", fechaModificacion)
}

fun RegistroEntity.aFilaDelServidor() = buildJsonObject {
    put("id_registro", idRegistro)
    put("id_animal", idAnimal)
    put("id_usuario", idUsuario)
    put("id_lectura", idLectura)
    put("peso", peso)
    putFecha("fecha_medicion", timestamp)
    putFecha("fecha_baja", fechaBaja)
    putFecha("fecha_modificacion", fechaModificacion)
}

fun ChapetaEntity.aFilaDelServidor() = buildJsonObject {
    put("id_chapeta", idChapeta)
    put("codigo", codigo)
    put("id_animal", idAnimal)
    putFecha("fecha_asociacion", fechaAsociacion)
    putFecha("fecha_desasociacion", fechaDesasociacion)
    putFecha("fecha_modificacion", fechaModificacion)
}

fun PesajePendienteEntity.aFilaDelServidor() = buildJsonObject {
    put("id_pesaje_pendiente", idPesajePendiente)
    put("id_lectura", idLectura)
    put("codigo", codigo)
    put("peso", peso)
    putFecha("fecha_medicion", timestamp)
    put("id_usuario", idUsuario)
    put("motivo", motivo)
    put("resolucion", resolucion)
    put("id_usuario_resolucion", idUsuarioResolucion)
    putFecha("fecha_resolucion", fechaResolucion)
    putFecha("fecha_modificacion", fechaModificacion)
}

// La fila local con la versión que tiene el servidor, ya sincronizada. Conserva lo que el servidor no guarda
// (la foto) y el idVersion de la última escritura local, que solo cuenta mientras haya cambios por subir.

fun PerfilFincaEntity.conLaDelServidor(fila: JsonObject) = copy(
    nombreFinca = fila.texto("nombre_finca"),
    fechaModificacion = fila.fecha("fecha_modificacion"),
    sincronizado = true,
)

fun AnimalEntity.conLaDelServidor(fila: JsonObject) = copy(
    idPadre = fila.textoONulo("id_padre"),
    idMadre = fila.textoONulo("id_madre"),
    nombre = fila.texto("nombre"),
    raza = fila.texto("raza"),
    sexo = fila.texto("sexo"),
    etapa = fila.texto("etapa"),
    fechaNacimiento = fila.fecha("fecha_nacimiento"),
    fechaNacimientoEsEstimada = fila.getValue("fecha_nacimiento_es_estimada").jsonPrimitive.boolean,
    proposito = fila.texto("proposito"),
    fechaBaja = fila.fechaONula("fecha_baja"),
    fechaModificacion = fila.fecha("fecha_modificacion"),
    sincronizado = true,
    rev = fila.getValue("rev").jsonPrimitive.int,
    revBase = null,
)

fun EventoEntity.conLaDelServidor(fila: JsonObject) = copy(
    tipoEvento = fila.texto("tipo_evento"),
    fecha = fila.fecha("fecha"),
    detalle = fila.texto("detalle"),
    fechaBaja = fila.fechaONula("fecha_baja"),
    fechaModificacion = fila.fecha("fecha_modificacion"),
    sincronizado = true,
    rev = fila.getValue("rev").jsonPrimitive.int,
    revBase = null,
)

fun RegistroEntity.conLaDelServidor(fila: JsonObject) = copy(
    peso = fila.getValue("peso").jsonPrimitive.float,
    timestamp = fila.fecha("fecha_medicion"),
    fechaBaja = fila.fechaONula("fecha_baja"),
    fechaModificacion = fila.fecha("fecha_modificacion"),
    sincronizado = true,
    rev = fila.getValue("rev").jsonPrimitive.int,
    revBase = null,
)

fun ChapetaEntity.conLaDelServidor(fila: JsonObject) = copy(
    fechaDesasociacion = fila.fechaONula("fecha_desasociacion"),
    fechaModificacion = fila.fecha("fecha_modificacion"),
    sincronizado = true,
)

fun PesajePendienteEntity.conLaDelServidor(fila: JsonObject) = copy(
    resolucion = fila.textoONulo("resolucion"),
    idUsuarioResolucion = fila.textoONulo("id_usuario_resolucion"),
    fechaResolucion = fila.fechaONula("fecha_resolucion"),
    fechaModificacion = fila.fecha("fecha_modificacion"),
    sincronizado = true,
)

// Filas que llegan en la descarga y el celular no tiene. Nunca se editaron en este celular, así que su idVersion
// queda vacío: solo sube una fila con sincronizado = false, y toda edición local le da uno (EDICION_LOCAL).

fun JsonObject.aAnimalEntity() = AnimalEntity(
    idAnimal = texto("id_animal"),
    idPerfilFinca = texto("id_perfil_finca"),
    nombre = texto("nombre"),
    raza = texto("raza"),
    sexo = texto("sexo"),
    etapa = texto("etapa"),
    fechaNacimiento = fecha("fecha_nacimiento"),
    proposito = texto("proposito"),
    fechaModificacion = fecha("fecha_modificacion"),
    idVersion = "",
).conLaDelServidor(this)

fun JsonObject.aEventoEntity() = EventoEntity(
    idEvento = texto("id_evento"),
    idAnimal = texto("id_animal"),
    idUsuario = texto("id_usuario"),
    tipoEvento = texto("tipo_evento"),
    fecha = fecha("fecha"),
    detalle = texto("detalle"),
    fechaModificacion = fecha("fecha_modificacion"),
    idVersion = "",
).conLaDelServidor(this)

fun JsonObject.aRegistroEntity() = RegistroEntity(
    idRegistro = texto("id_registro"),
    idAnimal = texto("id_animal"),
    idUsuario = texto("id_usuario"),
    idLectura = textoONulo("id_lectura"),
    peso = getValue("peso").jsonPrimitive.float,
    timestamp = fecha("fecha_medicion"),
    fechaModificacion = fecha("fecha_modificacion"),
    idVersion = "",
).conLaDelServidor(this)

fun JsonObject.aChapetaEntity() = ChapetaEntity(
    idChapeta = texto("id_chapeta"),
    codigo = texto("codigo"),
    idAnimal = texto("id_animal"),
    fechaAsociacion = fecha("fecha_asociacion"),
    fechaModificacion = fecha("fecha_modificacion"),
).conLaDelServidor(this)

fun JsonObject.aPesajePendienteEntity() = PesajePendienteEntity(
    idPesajePendiente = texto("id_pesaje_pendiente"),
    idLectura = texto("id_lectura"),
    codigo = texto("codigo"),
    peso = getValue("peso").jsonPrimitive.float,
    timestamp = fecha("fecha_medicion"),
    idUsuario = texto("id_usuario"),
    motivo = texto("motivo"),
    fechaModificacion = fecha("fecha_modificacion"),
).conLaDelServidor(this)

fun JsonObject.aSiblingEntity() = SiblingEntity(
    idVersion = texto("id_version"),
    entidad = texto("entidad"),
    idFila = texto("id_fila"),
    revBase = getValue("rev_base").jsonPrimitive.int,
    contenido = getValue("contenido").toString(),
    idUsuario = texto("id_usuario"),
    fechaRecepcion = fecha("fecha_recepcion"),
)

/** Si un sibling que llega en la descarga ya se resolvió, en este celular o en otro. */
fun JsonObject.siblingResuelto(): Boolean = textoONulo("resuelto_por") != null

internal fun JsonObject.texto(campo: String): String = getValue(campo).jsonPrimitive.content

private fun JsonObject.textoONulo(campo: String): String? = get(campo)?.takeUnless { it is JsonNull }?.jsonPrimitive?.content

@OptIn(ExperimentalTime::class)
private fun JsonObject.fecha(campo: String): Long = Instant.parse(texto(campo)).toEpochMilliseconds()

@OptIn(ExperimentalTime::class)
private fun JsonObject.fechaONula(campo: String): Long? =
    textoONulo(campo)?.let { Instant.parse(it).toEpochMilliseconds() }

@OptIn(ExperimentalTime::class)
private fun JsonObjectBuilder.putFecha(campo: String, milisegundos: Long?) {
    put(campo, milisegundos?.let { Instant.fromEpochMilliseconds(it).toString() })
}
