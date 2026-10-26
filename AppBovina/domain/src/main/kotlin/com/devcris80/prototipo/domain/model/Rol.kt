package com.devcris80.prototipo.domain.model

enum class Rol {
    GANADERO,
    OPERARIO,
    ;

    /**
     * Dar de baja, liberar una Chapeta y descartar un Pesaje pendiente son lo que la matriz de permisos
     * (SRS 2.1) niega al Operario. Lo demás lo pueden hacer ambos roles.
     */
    val puedeAdministrar: Boolean get() = this == GANADERO
}
