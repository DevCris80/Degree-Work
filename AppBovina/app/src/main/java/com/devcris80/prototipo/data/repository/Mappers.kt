package com.devcris80.prototipo.data.repository

import com.devcris80.prototipo.data.local.entity.AnimalEntity
import com.devcris80.prototipo.data.local.entity.ChapetaEntity
import com.devcris80.prototipo.data.local.entity.EventoEntity
import com.devcris80.prototipo.data.local.entity.PerfilFincaEntity
import com.devcris80.prototipo.data.local.entity.RegistroEntity
import com.devcris80.prototipo.data.local.entity.UsuarioEntity
import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Chapeta
import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.Evento
import com.devcris80.prototipo.domain.model.PerfilFinca
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.model.Registro
import com.devcris80.prototipo.domain.model.Rol
import com.devcris80.prototipo.domain.model.Sexo
import com.devcris80.prototipo.domain.model.Usuario

// Los enum del dominio se guardan en Room con el mismo texto que ya escribían las pantallas.

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
)

fun Animal.toEntity() = AnimalEntity(
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
)

fun ChapetaEntity.toDomain() = Chapeta(
    idChapeta = idChapeta,
    codigo = codigo,
    idAnimal = idAnimal,
    fechaAsociacion = fechaAsociacion,
    fechaDesasociacion = fechaDesasociacion,
)

fun Chapeta.toEntity() = ChapetaEntity(
    idChapeta = idChapeta,
    codigo = codigo,
    idAnimal = idAnimal,
    fechaAsociacion = fechaAsociacion,
    fechaDesasociacion = fechaDesasociacion,
)

fun EventoEntity.toDomain() = Evento(
    idEvento = idEvento,
    idAnimal = idAnimal,
    tipoEvento = tipoEvento,
    fecha = fecha,
    detalle = detalle,
    fechaBaja = fechaBaja,
)

fun Evento.toEntity() = EventoEntity(
    idEvento = idEvento,
    idAnimal = idAnimal,
    tipoEvento = tipoEvento,
    fecha = fecha,
    detalle = detalle,
    fechaBaja = fechaBaja,
)

fun RegistroEntity.toDomain() = Registro(
    idRegistro = idRegistro,
    idAnimal = idAnimal,
    peso = peso,
    timestamp = timestamp,
    fechaBaja = fechaBaja,
)

fun Registro.toEntity() = RegistroEntity(
    idRegistro = idRegistro,
    idAnimal = idAnimal,
    peso = peso,
    timestamp = timestamp,
    fechaBaja = fechaBaja,
)

fun PerfilFincaEntity.toDomain() = PerfilFinca(
    idPerfilFinca = id,
    nombreFinca = nombreFinca,
)

fun UsuarioEntity.toDomain() = Usuario(
    idUsuario = idUsuario,
    idPerfilFinca = idPerfilFinca,
    nombre = nombre,
    rol = TEXTO_ROL.desdeTexto(rol),
)

fun Usuario.toEntity() = UsuarioEntity(
    idUsuario = idUsuario,
    idPerfilFinca = idPerfilFinca,
    nombre = nombre,
    rol = TEXTO_ROL.aTexto(rol),
)
