package com.devcris80.prototipo.domain

import com.devcris80.prototipo.data.Chapeta
import com.devcris80.prototipo.data.ChapetaDao

/**
 * Asocia una Chapeta a un Animal sin importar si el id_chip llegó escrito a mano o por
 * lectura NFC (ver sección 4 de la spec): ambas vías terminan aquí. Si el animal ya tenía
 * una Chapeta activa, se cierra con fecha_desasociacion antes de crear la nueva.
 */
class AsociacionChapetaResolver(
    private val chapetaDao: ChapetaDao,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    suspend fun asociar(idAnimal: String, idChip: String): Chapeta {
        val activa = chapetaDao.findActivaByAnimal(idAnimal)
        if (activa != null) {
            if (activa.idChip == idChip) return activa
            chapetaDao.desasociar(activa.idChip, clock())
        }
        val nueva = Chapeta(
            idChip = idChip,
            idAnimal = idAnimal,
            fechaAsociacion = clock(),
        )
        chapetaDao.insert(nueva)
        return nueva
    }
}
