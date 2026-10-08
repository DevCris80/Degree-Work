package com.devcris80.prototipo.domain.repository

import com.devcris80.prototipo.domain.model.PerfilFinca
import com.devcris80.prototipo.domain.model.Usuario
import kotlinx.coroutines.flow.Flow

/** El PerfilFinca de este dispositivo y su Usuario. */
interface CuentaRepository {
    fun observePerfilFinca(): Flow<PerfilFinca?>

    suspend fun getPerfilFinca(): PerfilFinca?

    /** Guarda la finca de este dispositivo; el id lo asigna la capa de datos. */
    suspend fun crearPerfilFinca(nombreFinca: String): PerfilFinca

    fun observeUsuario(): Flow<Usuario?>

    suspend fun insertUsuario(usuario: Usuario)
}
