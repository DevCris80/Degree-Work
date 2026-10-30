package com.devcris80.prototipo.domain.repository

/** Un Operario que sigue en la finca, como lo ve el Ganadero (CU-05). */
data class OperarioDeLaFinca(val idUsuario: String, val nombre: String)

/**
 * Los Operarios de la finca viven en el servidor: listarlos y quitarlos necesita señal y, para quitar, el rol
 * Ganadero (RF-37). Quitar a uno revoca su acceso a la finca; sus registros anteriores se conservan a su nombre.
 */
interface OperarioRepository {
    /** Los Operarios que siguen en la finca, por nombre. */
    suspend fun listar(): ResultadoListaOperarios

    suspend fun quitar(idUsuario: String): ResultadoQuitarOperario
}

sealed interface ResultadoListaOperarios {
    data class Lista(val operarios: List<OperarioDeLaFinca>) : ResultadoListaOperarios
    data object SinConexion : ResultadoListaOperarios

    /** El servidor respondió con un error inesperado. */
    data object NoSePudo : ResultadoListaOperarios
}

sealed interface ResultadoQuitarOperario {
    data object Quitado : ResultadoQuitarOperario
    data object SinConexion : ResultadoQuitarOperario

    /** El servidor no deja quitar a este Usuario: no es Ganadero o la sesión ya no vale. */
    data object NoPermitido : ResultadoQuitarOperario

    /** Ese Operario ya no está en la finca: alguien lo quitó antes, o no es Operario de esta finca. */
    data object NoDisponible : ResultadoQuitarOperario

    /** El servidor respondió con un error inesperado. */
    data object NoSePudo : ResultadoQuitarOperario
}
