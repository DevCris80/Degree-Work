package com.devcris80.prototipo.data.repository

import com.devcris80.prototipo.data.local.AppDatabase
import com.devcris80.prototipo.data.local.CuentasEnElCelular
import com.devcris80.prototipo.domain.model.CuentaGuardada
import com.devcris80.prototipo.domain.repository.CuentasGuardadasRepository
import com.devcris80.prototipo.domain.repository.ResultadoQuitarCuenta
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Lee de la base de cada cuenta guardada lo que muestra la lista, sin abrir su sesión (ADR-0006). */
class RoomCuentasGuardadasRepository(private val cuentas: CuentasEnElCelular) : CuentasGuardadasRepository {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observe(): Flow<List<CuentaGuardada>> =
        combine(cuentas.entradas, cuentas.activa) { entradas, activa -> entradas.filter { it.archivo != activa } }
            .distinctUntilChanged()
            .flatMapLatest { entradas ->
                // La base adoptada no dice de quién es hasta que se abre.
                cuentas.resolverIdentidades()
                if (entradas.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(entradas.map { observarCuenta(it) }) { lista -> lista.filterNotNull().sortedBy { it.nombreFinca } }
                }
            }

    override suspend fun quitar(idUsuario: String): ResultadoQuitarCuenta {
        val entrada = cuentas.entradaDe(idUsuario) ?: return ResultadoQuitarCuenta.Quitada
        val cuenta = observarCuenta(entrada).first()
        if (cuenta != null && cuenta.tienePendientes) return ResultadoQuitarCuenta.TienePendientes
        cuentas.eliminar(entrada.archivo)
        return ResultadoQuitarCuenta.Quitada
    }

    private fun observarCuenta(entrada: CuentasEnElCelular.Entrada): Flow<CuentaGuardada?> {
        val base: AppDatabase = cuentas.base(entrada.archivo)
        return combine(
            base.usuarioDao().observe(),
            base.perfilFincaDao().observe(),
            base.estadoSyncDao().observeCambiosSinSubir(),
            base.estadoSyncDao().observeFotosSinSubir(),
            base.rechazoDao().observeAbiertos().map { it.size },
        ) { usuario, finca, cambios, fotos, rechazos ->
            if (usuario == null || finca == null) {
                null
            } else {
                CuentaGuardada(
                    idUsuario = usuario.idUsuario,
                    correo = entrada.correo,
                    nombreUsuario = usuario.nombre,
                    rol = usuario.toDomain().rol,
                    nombreFinca = finca.nombreFinca,
                    cambiosSinSubir = cambios,
                    fotosSinSubir = fotos,
                    rechazosAbiertos = rechazos,
                )
            }
        }
    }
}
