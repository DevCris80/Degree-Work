package com.devcris80.prototipo.ui.nav

import android.net.Uri

sealed class Rutas(val ruta: String) {
    data object Registro : Rutas("registro")
    data object Login : Rutas("login")
    data object ListaAnimales : Rutas("animales")
    data object NuevoAnimal : Rutas("animales/nuevo?codigo={codigo}&aviso={aviso}") {
        fun crear(codigo: String? = null, aviso: String? = null): String {
            val params = listOfNotNull(
                codigo?.let { "codigo=${Uri.encode(it)}" },
                aviso?.let { "aviso=${Uri.encode(it)}" },
            )
            return if (params.isEmpty()) "animales/nuevo" else "animales/nuevo?${params.joinToString("&")}"
        }
    }
    data object DetalleAnimal : Rutas("animales/{idAnimal}") {
        fun crear(idAnimal: String) = "animales/$idAnimal"
    }
    data object NuevoEvento : Rutas("animales/{idAnimal}/evento") {
        fun crear(idAnimal: String) = "animales/$idAnimal/evento"
    }
    data object Escanear : Rutas("escanear")
    data object EstadoServicio : Rutas("servicio")
}
