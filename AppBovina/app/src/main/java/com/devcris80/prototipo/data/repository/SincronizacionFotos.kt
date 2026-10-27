package com.devcris80.prototipo.data.repository

import com.devcris80.prototipo.data.local.AppDatabase
import com.devcris80.prototipo.data.local.FotosLocales
import com.devcris80.prototipo.data.remote.AlmacenFotos
import com.devcris80.prototipo.data.remote.esFaltaDeConexion
import kotlinx.coroutines.CancellationException

/**
 * Sube las fotos por subir y baja las que cambiaron en otros celulares de la finca (RF-38, CU-20 pasos 6 y 8). Cada
 * Animal tiene su archivo en `<finca>/<idAnimal>.webp` (supabase/README.md). Cambiar la foto reemplaza el archivo:
 * gana la última que sube.
 *
 * Una foto que falla queda para la próxima sincronización y las demás siguen; sin señal, se deja todo para la próxima
 * (lanza la excepción). Una foto local por subir nunca se reemplaza por la del servidor.
 */
internal class SincronizacionFotos(
    private val almacen: AlmacenFotos,
    private val fotos: FotosLocales,
    private val database: AppDatabase,
) {
    private val animales = database.animalDao()

    suspend fun sincronizar() {
        val finca = database.perfilFincaDao().getOnce()?.idPerfilFinca ?: return
        val subidas = subir(finca)
        bajar(finca, subidas)
    }

    /** Devuelve el tamaño de cada foto que subió, por Animal. */
    private suspend fun subir(finca: String): Map<String, Int> {
        val subidas = mutableMapOf<String, Int>()
        for (animal in animales.findFotosPorSubir()) {
            val fotoUri = checkNotNull(animal.fotoUri)
            val webp = fotos.comoWebp(fotoUri)
            // Si ya no se puede leer, tampoco se podrá en la próxima. La pantalla la muestra vacía.
            if (webp == null) {
                animales.marcarFotoSubida(animal.idAnimal, fotoUri)
                continue
            }
            intentar { almacen.subir(ruta(finca, animal.idAnimal), webp) } ?: continue
            animales.marcarFotoSubida(animal.idAnimal, fotoUri)
            subidas[animal.idAnimal] = webp.size
        }
        return subidas
    }

    private suspend fun bajar(finca: String, subidas: Map<String, Int>) {
        for (guardada in almacen.listar(finca)) {
            val idAnimal = guardada.nombre.removeSuffix(".webp")
            val local = animales.findById(idAnimal) ?: continue
            if (local.fotoPorSubir || local.fotoVersion == guardada.version) continue
            // La que este celular acaba de subir: Storage no dice qué versión le dio al subirla, así que la reconoce
            // por su tamaño en vez de volver a bajarla.
            if (subidas[idAnimal]?.toLong() == guardada.tamano) {
                animales.guardarFotoDelServidor(idAnimal, local.fotoUri, guardada.version)
                continue
            }
            val webp = intentar { almacen.descargar(ruta(finca, idAnimal)) } ?: continue
            animales.guardarFotoDelServidor(idAnimal, fotos.guardar(idAnimal, webp), guardada.version)
        }
    }

    /** Lo que devuelve [llamada], o nulo si el servidor la rechazó. Sin señal, lanza: tampoco pasaría la siguiente. */
    private suspend fun <T : Any> intentar(llamada: suspend () -> T): T? = try {
        llamada()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        if (e.esFaltaDeConexion()) throw e
        null
    }

    private fun ruta(finca: String, idAnimal: String) = "$finca/$idAnimal.webp"
}
