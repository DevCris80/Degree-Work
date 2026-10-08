package com.devcris80.prototipo.domain.model

/** Etapa de vida de un Animal. Cada etapa pertenece a un solo sexo. */
enum class Etapa(val sexo: Sexo) {
    TORO(Sexo.MACHO),
    NOVILLO(Sexo.MACHO),
    VACA(Sexo.HEMBRA),
    TERNERA(Sexo.HEMBRA),
    ;

    companion object {
        fun de(sexo: Sexo): List<Etapa> = entries.filter { it.sexo == sexo }
    }
}
