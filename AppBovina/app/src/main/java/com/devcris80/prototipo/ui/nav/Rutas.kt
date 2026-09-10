package com.devcris80.prototipo.ui.nav

sealed class Rutas(val ruta: String) {
    data object ListaAnimales : Rutas("animales")
    data object NuevoAnimal : Rutas("animales/nuevo")
    data object DetalleAnimal : Rutas("animales/{idAnimal}") {
        fun crear(idAnimal: String) = "animales/$idAnimal"
    }
    data object NuevoEvento : Rutas("animales/{idAnimal}/evento") {
        fun crear(idAnimal: String) = "animales/$idAnimal/evento"
    }
    data object EstadoServicio : Rutas("servicio")
}
