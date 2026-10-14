package com.devcris80.prototipo.data.repository

import com.devcris80.prototipo.data.local.dao.PerfilFincaDao
import com.devcris80.prototipo.data.local.dao.UsuarioDao
import com.devcris80.prototipo.data.local.entity.PerfilFincaEntity
import com.devcris80.prototipo.domain.model.PerfilFinca
import com.devcris80.prototipo.domain.model.Usuario
import com.devcris80.prototipo.domain.repository.CuentaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class RoomCuentaRepository(
    private val perfilFincaDao: PerfilFincaDao,
    private val usuarioDao: UsuarioDao,
    private val clock: () -> Long = { System.currentTimeMillis() },
) : CuentaRepository {
    override fun observePerfilFinca(): Flow<PerfilFinca?> = perfilFincaDao.observe().map { it?.toDomain() }

    override suspend fun getPerfilFinca(): PerfilFinca? = perfilFincaDao.getOnce()?.toDomain()

    // Hay un solo PerfilFinca por dispositivo: si ya existe, se actualiza conservando su id.
    override suspend fun crearPerfilFinca(nombreFinca: String): PerfilFinca {
        val idPerfilFinca = perfilFincaDao.getOnce()?.idPerfilFinca ?: UUID.randomUUID().toString()
        val perfil = PerfilFincaEntity(
            idPerfilFinca = idPerfilFinca,
            nombreFinca = nombreFinca,
            fechaModificacion = clock(),
        )
        perfilFincaDao.guardar(perfil)
        return perfil.toDomain()
    }

    override fun observeUsuario(): Flow<Usuario?> = usuarioDao.observe().map { it?.toDomain() }

    override suspend fun getUsuario(): Usuario? = usuarioDao.getOnce()?.toDomain()

    override suspend fun insertUsuario(usuario: Usuario) =
        usuarioDao.insert(usuario.toEntity(fechaModificacion = clock()))
}
