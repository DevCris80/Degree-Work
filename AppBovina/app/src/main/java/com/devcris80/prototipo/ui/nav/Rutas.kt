package com.devcris80.prototipo.ui.nav

import android.net.Uri
import com.devcris80.prototipo.domain.model.EntidadConRevisiones

sealed class Rutas(val ruta: String) {
    data object CrearCuenta : Rutas("crear-cuenta")
    data object Login : Rutas("login")
    data object ListaAnimales : Rutas("animales")
    data object NuevoAnimal : Rutas("animales/nuevo?codigo={codigo}&aviso={aviso}&idMadre={idMadre}") {
        fun crear(codigo: String? = null, aviso: String? = null, idMadre: String? = null): String {
            val params = listOfNotNull(
                codigo?.let { "codigo=${Uri.encode(it)}" },
                aviso?.let { "aviso=${Uri.encode(it)}" },
                idMadre?.let { "idMadre=${Uri.encode(it)}" },
            )
            return if (params.isEmpty()) "animales/nuevo" else "animales/nuevo?${params.joinToString("&")}"
        }
    }
    data object DetalleAnimal : Rutas("animales/{idAnimal}") {
        fun crear(idAnimal: String) = "animales/$idAnimal"
    }
    data object EditarAnimal : Rutas("animales/{idAnimal}/editar") {
        fun crear(idAnimal: String) = "animales/$idAnimal/editar"
    }
    data object NuevoEvento : Rutas("eventos/nuevo?idAnimal={idAnimal}") {
        fun crear(idAnimal: String? = null) =
            if (idAnimal == null) "eventos/nuevo" else "eventos/nuevo?idAnimal=${Uri.encode(idAnimal)}"
    }
    data object NuevoPesaje : Rutas("animales/{idAnimal}/pesaje") {
        fun crear(idAnimal: String) = "animales/$idAnimal/pesaje"
    }
    data object ResolverConflicto : Rutas("conflicto/{entidad}/{idFila}") {
        fun crear(entidad: EntidadConRevisiones, idFila: String) = "conflicto/${entidad.name}/$idFila"
    }
    data object Indicadores : Rutas("indicadores")
    data object Rechazos : Rutas("rechazos")
    data object Escanear : Rutas("escanear")
    data object Eventos : Rutas("eventos")
    data object EstadoServicio : Rutas("servicio")
    data object UbicacionFinca : Rutas("ubicacion-finca")
    data object InvitarOperario : Rutas("invitar-operario")
}
