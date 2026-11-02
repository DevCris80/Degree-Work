package com.devcris80.prototipo

import android.content.Context
import com.devcris80.prototipo.data.archivos.FotosEnArchivos
import com.devcris80.prototipo.data.archivos.FotosLocales
import com.devcris80.prototipo.data.local.CuentasEnElCelular
import com.devcris80.prototipo.data.remote.SesionCifrada
import com.devcris80.prototipo.data.remote.crearClienteSupabase
import com.devcris80.prototipo.data.repository.RoomCuentasGuardadasRepository
import com.devcris80.prototipo.data.repository.SupabaseSesionRepository
import com.devcris80.prototipo.domain.repository.CuentasGuardadasRepository
import com.devcris80.prototipo.domain.repository.SesionRepository
import io.github.jan.supabase.SupabaseClient

/**
 * Lo que la app necesita sea cual sea la cuenta con la sesión abierta, y aun sin ninguna: la sesión, el cliente de
 * Supabase y las cuentas guardadas (ADR-0006). Lo que sí depende de la cuenta lo arma [AppContainer].
 */
class NucleoApp(context: Context) {
    private val sesionCifrada = SesionCifrada(context)
    val supabase: SupabaseClient = crearClienteSupabase(sesionCifrada)
    val fotos: FotosLocales = FotosEnArchivos(context)
    val cuentas: CuentasEnElCelular = CuentasEnElCelular(context, fotos, sesionCifrada.guardada)
    val sesionRepository: SesionRepository = SupabaseSesionRepository(supabase, sesionCifrada, cuentas)
    val cuentasGuardadasRepository: CuentasGuardadasRepository = RoomCuentasGuardadasRepository(cuentas)
}
