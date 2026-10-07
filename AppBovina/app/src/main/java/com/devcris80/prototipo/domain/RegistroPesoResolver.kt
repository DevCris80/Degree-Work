package com.devcris80.prototipo.domain

import com.devcris80.prototipo.data.ChapetaDao
import com.devcris80.prototipo.data.Registro
import com.devcris80.prototipo.data.RegistroDao
import java.util.UUID

sealed interface RegistroPesoResultado {
    data class Exito(val idRegistro: String) : RegistroPesoResultado
    data class Error(val mensaje: String) : RegistroPesoResultado
}

/**
 * Resuelve un pesaje recibido (id_chip + peso) contra la Chapeta asociada y crea el Registro
 * correspondiente. No depende de NanoHTTPD ni del Foreground Service: solo de los DAOs de Room,
 * para poder testearse de forma aislada del transporte HTTP.
 */
class RegistroPesoResolver(
    private val chapetaDao: ChapetaDao,
    private val registroDao: RegistroDao,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    suspend fun registrarPeso(codigo: String, peso: Float): RegistroPesoResultado {
        val chapeta = chapetaDao.findActivaByCodigo(codigo)
            ?: return RegistroPesoResultado.Error("chip no asociado")

        val registro = Registro(
            idRegistro = UUID.randomUUID().toString(),
            idAnimal = chapeta.idAnimal,
            peso = peso,
            timestamp = clock(),
        )
        registroDao.insert(registro)
        return RegistroPesoResultado.Exito(registro.idRegistro)
    }
}
