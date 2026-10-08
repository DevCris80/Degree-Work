package com.devcris80.prototipo.data.repository

import androidx.room.withTransaction
import com.devcris80.prototipo.data.local.AppDatabase
import com.devcris80.prototipo.domain.repository.Transaccion

class RoomTransaccion(private val database: AppDatabase) : Transaccion {
    override suspend fun <T> ejecutar(bloque: suspend () -> T): T = database.withTransaction { bloque() }
}
