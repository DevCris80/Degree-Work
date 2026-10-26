package com.devcris80.prototipo.testing

import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Conflicto
import com.devcris80.prototipo.domain.model.EntidadConRevisiones
import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.Evento
import com.devcris80.prototipo.domain.model.FilaEnConflicto
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.model.Sexo
import com.devcris80.prototipo.domain.model.Sibling
import com.devcris80.prototipo.domain.model.SiblingEnConflicto

fun animalEnConflicto(idAnimal: String, raza: String = "Holstein") = Animal(
    idAnimal = idAnimal,
    idPerfilFinca = "perfil_local",
    nombre = "Manchas",
    raza = raza,
    sexo = Sexo.HEMBRA,
    etapa = Etapa.VACA,
    fechaNacimiento = 1L,
    proposito = Proposito.LECHE,
)

fun siblingDeAnimal(idVersion: String, idAnimal: String, raza: String, fechaRecepcion: Long = 10L) =
    SiblingEnConflicto(
        sibling = Sibling(
            idVersion = idVersion,
            entidad = EntidadConRevisiones.ANIMAL,
            idFila = idAnimal,
            revBase = 1,
            contenido = "{}",
            idUsuario = "usuario-2",
            fechaRecepcion = fechaRecepcion,
        ),
        fila = FilaEnConflicto.DeAnimal(animalEnConflicto(idAnimal, raza)),
    )

/** El Animal [idAnimal] con raza vigente "Holstein" y un Sibling por cada [siblings] (idVersion a raza). */
fun conflictoDeAnimal(idAnimal: String, vararg siblings: Pair<String, String>) = Conflicto(
    entidad = EntidadConRevisiones.ANIMAL,
    idFila = idAnimal,
    vigente = FilaEnConflicto.DeAnimal(animalEnConflicto(idAnimal)),
    siblings = siblings.mapIndexed { i, (idVersion, raza) -> siblingDeAnimal(idVersion, idAnimal, raza, 10L + i) },
)

fun conflictoDeEvento(idEvento: String, idVersion: String) = Conflicto(
    entidad = EntidadConRevisiones.EVENTO,
    idFila = idEvento,
    vigente = FilaEnConflicto.DeEvento(Evento(idEvento, "animal", "usuario", "vacuna", 1L, "aftosa")),
    siblings = listOf(
        SiblingEnConflicto(
            Sibling(idVersion, EntidadConRevisiones.EVENTO, idEvento, 1, "{}", "usuario-2", 10L),
            FilaEnConflicto.DeEvento(Evento(idEvento, "animal", "usuario", "vacuna", 1L, "triple")),
        ),
    ),
)
