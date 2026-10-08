package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.Rol
import com.devcris80.prototipo.domain.model.Usuario
import com.devcris80.prototipo.domain.repository.CuentaRepository
import com.devcris80.prototipo.domain.repository.Transaccion
import java.util.UUID

/**
 * Crea el PerfilFinca de este dispositivo y su primer Usuario, con rol Ganadero, en una sola
 * transacción: o se guardan ambos o ninguno.
 */
class CrearCuenta(
    private val cuentaRepository: CuentaRepository,
    private val transaccion: Transaccion,
) {
    suspend operator fun invoke(nombreUsuario: String, nombreFinca: String): Usuario = transaccion.ejecutar {
        val perfilFinca = cuentaRepository.crearPerfilFinca(nombreFinca)
        val usuario = Usuario(
            idUsuario = UUID.randomUUID().toString(),
            idPerfilFinca = perfilFinca.idPerfilFinca,
            nombre = nombreUsuario,
            rol = Rol.GANADERO,
        )
        cuentaRepository.insertUsuario(usuario)
        usuario
    }
}
