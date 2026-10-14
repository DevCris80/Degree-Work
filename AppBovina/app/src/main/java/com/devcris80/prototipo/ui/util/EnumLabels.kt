package com.devcris80.prototipo.ui.util

import com.devcris80.prototipo.domain.model.Etapa
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
