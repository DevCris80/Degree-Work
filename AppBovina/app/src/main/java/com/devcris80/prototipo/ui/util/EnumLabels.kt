package com.devcris80.prototipo.ui.util

import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.MotivoPesajePendiente
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.model.Sexo

/** Traduce los enum del dominio a las etiquetas en español que ya usaba la UI. */
fun Sexo.aEtiqueta(): String = when (this) {
    Sexo.MACHO -> "Macho"
    Sexo.HEMBRA -> "Hembra"
}

fun Etapa.aEtiqueta(): String = when (this) {
    Etapa.TORO -> "Toro"
    Etapa.NOVILLO -> "Novillo"
    Etapa.VACA -> "Vaca"
    Etapa.TERNERA -> "Ternera"
}

fun Proposito.aEtiqueta(): String = when (this) {
    Proposito.LECHE -> "Leche"
    Proposito.CARNE -> "Carne"
    Proposito.DOBLE_PROPOSITO -> "Doble Propósito"
}

fun MotivoPesajePendiente.aEtiqueta(): String = when (this) {
    MotivoPesajePendiente.CODIGO_SIN_CHAPETA_ACTIVA -> "Código sin chapeta activa"
    MotivoPesajePendiente.ANIMAL_DADO_DE_BAJA -> "Animal dado de baja"
}

/** El nombre de un Animal, marcado si está dado de baja (por ejemplo, un padre que ya salió de la finca). */
fun Animal.nombreConBaja(): String = if (fechaBaja == null) nombre else "$nombre (dado de baja)"
