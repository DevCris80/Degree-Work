package com.devcris80.prototipo.domain.repository

import com.devcris80.prototipo.domain.model.PerfilFinca
import com.devcris80.prototipo.domain.model.Usuario
import kotlinx.coroutines.flow.Flow

/**
 * El PerfilFinca de este dispositivo y su Usuario. Los escribe [SesionRepository] al crear la cuenta o al iniciar
 * sesión, tal como quedaron en el servidor.
 */
interface CuentaRepository {
    fun observePerfilFinca(): Flow<PerfilFinca?>

    suspend fun getPerfilFinca(): PerfilFinca?

    fun observeUsuario(): Flow<Usuario?>

    suspend fun getUsuario(): Usuario?
}
