package com.devcris80.prototipo

import android.content.Context
import com.devcris80.prototipo.data.local.AppDatabase
import com.devcris80.prototipo.data.local.FotosEnArchivos
import com.devcris80.prototipo.data.remote.SesionCifrada
import com.devcris80.prototipo.data.remote.SupabaseAlmacenFotos
import com.devcris80.prototipo.data.remote.SupabaseServidorSincronizacion
import com.devcris80.prototipo.data.remote.crearClienteSupabase
import com.devcris80.prototipo.data.repository.MotorSincronizacion
import com.devcris80.prototipo.data.repository.RoomAnimalRepository
import com.devcris80.prototipo.data.repository.RoomChapetaRepository
import com.devcris80.prototipo.data.repository.RoomConflictoRepository
import com.devcris80.prototipo.data.repository.RoomCuentaRepository
import com.devcris80.prototipo.data.repository.RoomEventoRepository
import com.devcris80.prototipo.data.repository.RoomPesajePendienteRepository
import com.devcris80.prototipo.data.repository.RoomRechazoRepository
import com.devcris80.prototipo.data.repository.RoomRegistroRepository
import com.devcris80.prototipo.data.repository.RoomTransaccion
import com.devcris80.prototipo.data.repository.SupabaseSesionRepository
import com.devcris80.prototipo.domain.repository.AnimalRepository
import com.devcris80.prototipo.domain.repository.ChapetaRepository
import com.devcris80.prototipo.domain.repository.ConflictoRepository
import com.devcris80.prototipo.domain.repository.CuentaRepository
import com.devcris80.prototipo.domain.repository.EventoRepository
import com.devcris80.prototipo.domain.repository.PesajePendienteRepository
import com.devcris80.prototipo.domain.repository.RechazoRepository
import com.devcris80.prototipo.domain.repository.RegistroRepository
import com.devcris80.prototipo.domain.repository.SesionRepository
import com.devcris80.prototipo.domain.repository.SincronizacionRepository
import com.devcris80.prototipo.domain.repository.Transaccion
import com.devcris80.prototipo.domain.usecase.AsignarPesajesPendientes
import com.devcris80.prototipo.domain.usecase.CrearCuenta
import com.devcris80.prototipo.domain.usecase.ActualizarAnimal
import com.devcris80.prototipo.domain.usecase.DarDeBajaAnimal
import com.devcris80.prototipo.domain.usecase.DescartarPesajePendiente
import com.devcris80.prototipo.domain.usecase.LiberarChapeta
import com.devcris80.prototipo.domain.usecase.ObservarEvolucionPeso
import com.devcris80.prototipo.domain.usecase.ObservarIndicadoresHato
import com.devcris80.prototipo.domain.usecase.ObservarPesajesPendientes
import com.devcris80.prototipo.domain.usecase.RegistrarAnimal
import com.devcris80.prototipo.domain.usecase.RegistrarPesaje
import com.devcris80.prototipo.domain.usecase.RegistrarPesajeManual
import com.devcris80.prototipo.domain.usecase.ResolverConflicto
import com.devcris80.prototipo.domain.usecase.ResolverDestinoEscaneo
import com.devcris80.prototipo.service.ProgramadorSincronizacion

/**
 * Arma a mano los repositorios y casos de uso de la app (ver docs/adr/0001). Es el único lugar,
 * fuera de `data`, que conoce las implementaciones de Room.
 */
class AppContainer(context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val transaccion: Transaccion = RoomTransaccion(database)

    val animalRepository: AnimalRepository = RoomAnimalRepository(database.animalDao())
    val chapetaRepository: ChapetaRepository = RoomChapetaRepository(database.chapetaDao())
    val eventoRepository: EventoRepository = RoomEventoRepository(database.eventoDao())
    val registroRepository: RegistroRepository = RoomRegistroRepository(database.registroDao())
    val pesajePendienteRepository: PesajePendienteRepository =
        RoomPesajePendienteRepository(database.pesajePendienteDao())
    val cuentaRepository: CuentaRepository =
        RoomCuentaRepository(database.perfilFincaDao(), database.usuarioDao())
    val conflictoRepository: ConflictoRepository = RoomConflictoRepository(database)
    val rechazoRepository: RechazoRepository = RoomRechazoRepository(database.rechazoDao())

    private val sesionCifrada = SesionCifrada(context)
    private val supabase = crearClienteSupabase(sesionCifrada)
    val sesionRepository: SesionRepository = SupabaseSesionRepository(
        supabase,
        sesionCifrada,
        database.perfilFincaDao(),
        database.usuarioDao(),
        transaccion,
    )
    val sincronizacionRepository: SincronizacionRepository = MotorSincronizacion(
        SupabaseServidorSincronizacion(supabase),
        database,
        SupabaseAlmacenFotos(supabase),
        FotosEnArchivos(context),
        programarTrabajo = ProgramadorSincronizacion(context)::programar,
    )

    val registrarPesaje = RegistrarPesaje(
        chapetaRepository,
        animalRepository,
        registroRepository,
        pesajePendienteRepository,
        cuentaRepository,
        transaccion,
    )
    val registrarPesajeManual =
        RegistrarPesajeManual(animalRepository, registroRepository, cuentaRepository, transaccion)
    val resolverDestinoEscaneo = ResolverDestinoEscaneo(chapetaRepository, animalRepository)
    val registrarAnimal =
        RegistrarAnimal(animalRepository, chapetaRepository, pesajePendienteRepository, transaccion)
    val actualizarAnimal = ActualizarAnimal(animalRepository, transaccion)
    val resolverConflicto = ResolverConflicto(conflictoRepository)
    val darDeBajaAnimal = DarDeBajaAnimal(animalRepository, cuentaRepository)
    val liberarChapeta = LiberarChapeta(chapetaRepository, cuentaRepository)
    val crearCuenta = CrearCuenta(sesionRepository)
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
    val observarIndicadoresHato = ObservarIndicadoresHato(
        animalRepository,
        chapetaRepository,
        registroRepository,
        pesajePendienteRepository,
        eventoRepository,
    )
    val observarEvolucionPeso = ObservarEvolucionPeso(registroRepository)
}
