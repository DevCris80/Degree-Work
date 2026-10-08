package com.devcris80.prototipo

import com.devcris80.prototipo.data.local.AppDatabase
import com.devcris80.prototipo.data.repository.RoomAnimalRepository
import com.devcris80.prototipo.data.repository.RoomChapetaRepository
import com.devcris80.prototipo.data.repository.RoomCuentaRepository
import com.devcris80.prototipo.data.repository.RoomEventoRepository
import com.devcris80.prototipo.data.repository.RoomPesajePendienteRepository
import com.devcris80.prototipo.data.repository.RoomRegistroRepository
import com.devcris80.prototipo.data.repository.RoomTransaccion
import com.devcris80.prototipo.domain.repository.AnimalRepository
import com.devcris80.prototipo.domain.repository.ChapetaRepository
import com.devcris80.prototipo.domain.repository.CuentaRepository
import com.devcris80.prototipo.domain.repository.EventoRepository
import com.devcris80.prototipo.domain.repository.PesajePendienteRepository
import com.devcris80.prototipo.domain.repository.RegistroRepository
import com.devcris80.prototipo.domain.repository.Transaccion
import com.devcris80.prototipo.domain.usecase.AsignarPesajesPendientes
import com.devcris80.prototipo.domain.usecase.CrearCuenta
import com.devcris80.prototipo.domain.usecase.DarDeBajaAnimal
import com.devcris80.prototipo.domain.usecase.DescartarPesajePendiente
import com.devcris80.prototipo.domain.usecase.LiberarChapeta
import com.devcris80.prototipo.domain.usecase.ObservarPesajesPendientes
import com.devcris80.prototipo.domain.usecase.RegistrarAnimal
import com.devcris80.prototipo.domain.usecase.RegistrarPesaje
import com.devcris80.prototipo.domain.usecase.ResolverDestinoEscaneo

/**
 * Arma a mano los repositorios y casos de uso de la app (ver docs/adr/0001). Es el único lugar,
 * fuera de `data`, que conoce las implementaciones de Room.
 */
class AppContainer(database: AppDatabase) {
    private val transaccion: Transaccion = RoomTransaccion(database)

    val animalRepository: AnimalRepository = RoomAnimalRepository(database.animalDao())
    val chapetaRepository: ChapetaRepository = RoomChapetaRepository(database.chapetaDao())
    val eventoRepository: EventoRepository = RoomEventoRepository(database.eventoDao())
    val registroRepository: RegistroRepository = RoomRegistroRepository(database.registroDao())
    val pesajePendienteRepository: PesajePendienteRepository =
        RoomPesajePendienteRepository(database.pesajePendienteDao())
    val cuentaRepository: CuentaRepository =
        RoomCuentaRepository(database.perfilFincaDao(), database.usuarioDao())

    val registrarPesaje = RegistrarPesaje(
        chapetaRepository,
        animalRepository,
        registroRepository,
        pesajePendienteRepository,
        cuentaRepository,
        transaccion,
    )
    val resolverDestinoEscaneo = ResolverDestinoEscaneo(chapetaRepository, animalRepository)
    val registrarAnimal =
        RegistrarAnimal(animalRepository, chapetaRepository, pesajePendienteRepository, transaccion)
    val darDeBajaAnimal = DarDeBajaAnimal(animalRepository)
    val liberarChapeta = LiberarChapeta(chapetaRepository)
    val crearCuenta = CrearCuenta(cuentaRepository, transaccion)
    val observarPesajesPendientes =
        ObservarPesajesPendientes(pesajePendienteRepository, chapetaRepository, animalRepository)
    val asignarPesajesPendientes = AsignarPesajesPendientes(
        pesajePendienteRepository,
        animalRepository,
        registroRepository,
        cuentaRepository,
        transaccion,
    )
    val descartarPesajePendiente =
        DescartarPesajePendiente(pesajePendienteRepository, cuentaRepository, transaccion)
}
