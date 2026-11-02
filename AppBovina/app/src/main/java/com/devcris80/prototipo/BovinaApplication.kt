package com.devcris80.prototipo

import android.app.Application
import com.devcris80.prototipo.service.programarSincronizacionTrasEscrituras
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BovinaApplication : Application() {
    val nucleo: NucleoApp by lazy { NucleoApp(this) }

    private val contenedores = mutableMapOf<String?, AppContainer>()

    /**
     * El contenedor de la cuenta con la sesión abierta, o uno sin datos si no hay ninguna (ADR-0006). Cambia al
     * iniciar o cerrar sesión: quien lo guarde mucho tiempo se queda con el de una cuenta que ya no es la abierta.
     */
    val container: AppContainer get() = contenedorDe(nucleo.cuentas.activa.value)

    @Synchronized
    private fun contenedorDe(archivo: String?): AppContainer {
        // Una cuenta quitada no deja su contenedor, que apunta a una base que ya se borró.
        val guardadas = nucleo.cuentas.entradas.value.map { it.archivo }.toSet()
        contenedores.keys.retainAll { it == null || it in guardadas }
        return contenedores.getOrPut(archivo) { AppContainer(this, nucleo, archivo) }
    }

    override fun onCreate() {
        super.onCreate()
        MainScope().launch {
            // La sincronización tras las escrituras es de la cuenta abierta: al cambiar de cuenta se cambia de motor.
            nucleo.cuentas.activa.collectLatest { archivo ->
                coroutineScope { programarSincronizacionTrasEscrituras(contenedorDe(archivo).sincronizacionRepository) }
            }
        }
    }
}
