package com.devcris80.prototipo.data.repository

import com.devcris80.prototipo.data.local.dao.PerfilFincaDao
import com.devcris80.prototipo.data.local.dao.UsuarioDao
import com.devcris80.prototipo.data.local.entity.PerfilFincaEntity
import com.devcris80.prototipo.domain.model.PerfilFinca
import com.devcris80.prototipo.domain.model.Usuario
import com.devcris80.prototipo.domain.repository.CuentaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomCuentaRepository(
    private val perfilFincaDao: PerfilFincaDao,
    private val usuarioDao: UsuarioDao,
) : CuentaRepository {
    override fun observePerfilFinca(): Flow<PerfilFinca?> = perfilFincaDao.observe().map { it?.toDomain() }

    override suspend fun getPerfilFinca(): PerfilFinca? = perfilFincaDao.getOnce()?.toDomain()

    // El id sigue siendo el fijo de PerfilFincaEntity hasta que #41 lo pase a UUID.
    override suspend fun crearPerfilFinca(nombreFinca: String): PerfilFinca {
        val perfil = PerfilFincaEntity(nombreFinca = nombreFinca)
        perfilFincaDao.guardar(perfil)
        return perfil.toDomain()
    }

    override fun observeUsuario(): Flow<Usuario?> = usuarioDao.observe().map { it?.toDomain() }

    override suspend fun insertUsuario(usuario: Usuario) = usuarioDao.insert(usuario.toEntity())
}
