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

// Las entidades de Siblings y Rechazos se guardan con el nombre de su tabla en el servidor.
private val TEXTO_ENTIDAD_SINCRONIZADA = mapOf(
    EntidadSincronizada.PERFIL_FINCA to "perfil_finca",
    EntidadSincronizada.ANIMAL to "animal",
    EntidadSincronizada.EVENTO to "evento",
    EntidadSincronizada.REGISTRO to "registro",
    EntidadSincronizada.CHAPETA to "chapeta",
    EntidadSincronizada.PESAJE_PENDIENTE to "pesaje_pendiente",
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

fun Usuario.toEntity(fechaModificacion: Long) = UsuarioEntity(
    idUsuario = idUsuario,
    idPerfilFinca = idPerfilFinca,
    nombre = nombre,
    rol = TEXTO_ROL.aTexto(rol),
    fechaModificacion = fechaModificacion,
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

fun RechazoEntity.toDomain() = Rechazo(
    idRechazo = idRechazo,
    entidad = TEXTO_ENTIDAD_SINCRONIZADA.desdeTexto(entidad),
    idFila = idFila,
    contenido = contenido,
    motivo = motivo,
    fechaRechazo = fechaRechazo,
)
