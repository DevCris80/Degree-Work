package com.devcris80.prototipo.data.repository

import androidx.room.withTransaction
import com.devcris80.prototipo.data.archivos.FotosLocales
import com.devcris80.prototipo.data.local.AppDatabase
import com.devcris80.prototipo.data.local.entity.RechazoEntity
import com.devcris80.prototipo.data.remote.AlmacenFotos
import com.devcris80.prototipo.data.remote.FotoRechazada
import com.devcris80.prototipo.data.remote.esFaltaDeConexion
import com.devcris80.prototipo.data.remote.salvoCancelacion
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Sube las fotos por subir y baja las que cambiaron en otros celulares de la finca (RF-38, CU-20 pasos 6 y 8). Cada
 * Animal tiene su archivo en `<finca>/<idAnimal>.webp` (supabase/README.md). Cambiar la foto reemplaza el archivo:
 * gana la última que sube.
 *
 * Una foto que falla queda para la próxima sincronización y las demás siguen; sin señal, se deja todo para la próxima
 * (lanza la excepción). Una foto local por subir nunca se reemplaza por la del servidor. Una que Storage no aceptará
 * nunca ([FotoRechazada]) queda en la bandeja como Rechazo, igual que una fila.
 */
internal class SincronizacionFotos(
    private val almacen: AlmacenFotos,
    private val fotos: FotosLocales,
    private val database: AppDatabase,
    private val clock: () -> Long,
    private val nuevoIdRechazo: () -> String,
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
                animales.quitarFotoPorSubir(animal.idAnimal, fotoUri)
                continue
            }
            val subida = try {
                intentar { almacen.subir(ruta(finca, animal.idAnimal), webp) }
            } catch (rechazo: FotoRechazada) {
                rechazar(animal.idAnimal, fotoUri, rechazo)
                continue
            }
            subida ?: continue
            animales.quitarFotoPorSubir(animal.idAnimal, fotoUri)
            subidas[animal.idAnimal] = webp.size
        }
        return subidas
    }

    private suspend fun bajar(finca: String, subidas: Map<String, Int>) {
        for (guardada in almacen.listar(finca)) {
            val idAnimal = guardada.nombre.removeSuffix(".webp")
            val local = animales.findById(idAnimal) ?: continue
            if (local.fotoPorSubir || local.fechaFotoServidor == guardada.fechaModificacion) continue
            // La que este celular acaba de subir: Storage no dice qué versión le dio al subirla, así que la reconoce
            // por su tamaño en vez de volver a bajarla.
            if (subidas[idAnimal]?.toLong() == guardada.tamano) {
                animales.guardarFotoDelServidor(idAnimal, local.fotoUri, guardada.fechaModificacion)
                continue
            }
            val webp = intentar { almacen.descargar(ruta(finca, idAnimal)) } ?: continue
            animales.guardarFotoDelServidor(idAnimal, fotos.guardar(idAnimal, webp), guardada.fechaModificacion)
        }
    }

    /**
     * Como una fila que el servidor rechazó (RN-24): queda en la bandeja y no se reintenta. Al dejar de estar por subir,
     * la foto del servidor, si hay una, baja en este mismo paso, como la fila vuelve a la versión del servidor.
     */
    private suspend fun rechazar(idAnimal: String, fotoUri: String, rechazo: FotoRechazada) = database.withTransaction {
        database.rechazoDao().insert(
            RechazoEntity(
                idRechazo = nuevoIdRechazo(),
                entidad = "foto",
                idFila = idAnimal,
                contenido = buildJsonObject {
                    put("id_animal", idAnimal)
                    put("foto_uri", fotoUri)
                }.toString(),
                motivo = "${rechazo.codigo}: ${rechazo.motivo}",
                fechaRechazo = clock(),
            ),
        )
        animales.quitarFotoPorSubir(idAnimal, fotoUri)
    }

    /**
     * Lo que devuelve [llamada], o nulo si falló y puede pasar en otro intento. Sin señal, lanza: tampoco pasaría la
     * siguiente. Una [FotoRechazada] también sigue: no pasará nunca.
     */
    private suspend fun <T : Any> intentar(llamada: suspend () -> T): T? = salvoCancelacion(
        bloque = { llamada() },
        alFallar = { if (it.esFaltaDeConexion() || it is FotoRechazada) throw it else null },
    )

    private fun ruta(finca: String, idAnimal: String) = "$finca/$idAnimal.webp"
}
