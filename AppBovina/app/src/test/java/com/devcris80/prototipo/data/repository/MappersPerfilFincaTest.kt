package com.devcris80.prototipo.data.repository

import com.devcris80.prototipo.data.local.entity.PerfilFincaEntity
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** La ubicación de la finca viaja con el perfil en la subida y en la descarga (RF-14). */
class MappersPerfilFincaTest {
    private fun filaDelServidor(latitud: Double?, longitud: Double?): JsonObject = buildJsonObject {
        put("id_perfil_finca", "finca-1")
        put("nombre_finca", "La Esperanza")
        put("latitud", latitud)
        put("longitud", longitud)
        put("fecha_modificacion", "2026-10-27T10:00:00Z")
    }

    @Test
    fun laUbicacionSubeConElPerfil() {
        val fila = PerfilFincaEntity("finca-1", "La Esperanza", fechaModificacion = 0, latitud = 4.6097, longitud = -74.0817)
            .aFilaDelServidor()

        assertEquals(4.6097, fila.getValue("latitud").jsonPrimitive.double, 0.0)
        assertEquals(-74.0817, fila.getValue("longitud").jsonPrimitive.double, 0.0)
    }

    @Test
    fun unaFincaSinUbicacionSubeLasCoordenadasNulas() {
        val fila = PerfilFincaEntity("finca-1", "La Esperanza", fechaModificacion = 0).aFilaDelServidor()

        assertEquals(JsonNull, fila.getValue("latitud"))
        assertEquals(JsonNull, fila.getValue("longitud"))
    }

    @Test
    fun laFincaDescargadaTraeSuUbicacion() {
        val entidad = filaDelServidor(4.6097, -74.0817).aPerfilFincaEntity()

        assertEquals(4.6097, entidad.latitud!!, 0.0)
        assertEquals(-74.0817, entidad.longitud!!, 0.0)
        assertTrue(entidad.sincronizado)
    }

    @Test
    fun laFincaDescargadaSinUbicacionLaDejaNula() {
        val entidad = filaDelServidor(null, null).aPerfilFincaEntity()

        assertNull(entidad.latitud)
        assertNull(entidad.longitud)
    }

    @Test
    fun laRespuestaDelServidorActualizaLaUbicacionLocalYMarcaLaFincaSubida() {
        val local = PerfilFincaEntity("finca-1", "La Esperanza", sincronizado = false, fechaModificacion = 0, latitud = 1.0, longitud = 2.0)

        val actualizada = local.conLaDelServidor(filaDelServidor(4.6097, -74.0817))

        assertEquals(4.6097, actualizada.latitud!!, 0.0)
        assertEquals(-74.0817, actualizada.longitud!!, 0.0)
        assertTrue(actualizada.sincronizado)
    }
}
