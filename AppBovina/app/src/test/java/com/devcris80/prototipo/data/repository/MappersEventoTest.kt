package com.devcris80.prototipo.data.repository

import com.devcris80.prototipo.data.local.entity.EventoEntity
import com.devcris80.prototipo.domain.model.Evento
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Los campos de cada tipo de Evento viajan con la fila en la subida y en la descarga (RF-29). */
class MappersEventoTest {
    private fun vacunacion() = EventoEntity(
        idEvento = "evento-1",
        idAnimal = "animal-1",
        idUsuario = "usuario-1",
        tipoEvento = "Vacunación",
        fecha = 0,
        detalle = "Vacunación del ciclo I",
        biologico = "Aftosa",
        dosis = "2 ml",
        via = "Subcutánea",
        fechaModificacion = 0,
        idVersion = "version-1",
    )

    private fun filaDelServidor(
        biologico: String?,
        dosis: String?,
        via: String?,
        idCriaResultante: String?,
    ): JsonObject = buildJsonObject {
        put("id_evento", "evento-1")
        put("id_animal", "animal-1")
        put("id_usuario", "usuario-1")
        put("tipo_evento", "Vacunación")
        put("fecha", "2026-10-27T10:00:00Z")
        put("detalle", "Vacunación del ciclo I")
        put("biologico", biologico)
        put("dosis", dosis)
        put("via", via)
        put("id_cria_resultante", idCriaResultante)
        put("fecha_modificacion", "2026-10-27T10:00:00Z")
        put("rev", 3)
    }

    @Test
    fun losCamposDeLaVacunacionSubenConElEvento() {
        val fila = vacunacion().aFilaDelServidor()

        assertEquals("Aftosa", fila.getValue("biologico").jsonPrimitive.content)
        assertEquals("2 ml", fila.getValue("dosis").jsonPrimitive.content)
        assertEquals("Subcutánea", fila.getValue("via").jsonPrimitive.content)
        assertEquals(JsonNull, fila.getValue("id_cria_resultante"))
    }

    @Test
    fun laCriaDeUnPartoSubeConElEvento() {
        val fila = vacunacion().copy(tipoEvento = "Parto", biologico = null, dosis = null, via = null, idCriaResultante = "cria-1")
            .aFilaDelServidor()

        assertEquals("cria-1", fila.getValue("id_cria_resultante").jsonPrimitive.content)
        assertEquals(JsonNull, fila.getValue("biologico"))
    }

    @Test
    fun elEventoDescargadoTraeSusCampos() {
        val entidad = filaDelServidor("Aftosa", "2 ml", "Subcutánea", null).aEventoEntity()

        assertEquals("Aftosa", entidad.biologico)
        assertEquals("2 ml", entidad.dosis)
        assertEquals("Subcutánea", entidad.via)
        assertNull(entidad.idCriaResultante)
        assertTrue(entidad.sincronizado)
    }

    @Test
    fun elEventoDescargadoSinCamposLosDejaNulos() {
        val entidad = filaDelServidor(null, null, null, null).aEventoEntity()

        assertNull(entidad.biologico)
        assertNull(entidad.dosis)
        assertNull(entidad.via)
        assertNull(entidad.idCriaResultante)
    }

    @Test
    fun laRespuestaDelServidorActualizaLosCamposLocalesYMarcaElEventoSubido() {
        val local = vacunacion().copy(sincronizado = false, biologico = "Brucelosis", idCriaResultante = "otra")

        val actualizado = local.conLaDelServidor(filaDelServidor("Aftosa", "2 ml", "Subcutánea", null))

        assertEquals("Aftosa", actualizado.biologico)
        assertNull(actualizado.idCriaResultante)
        assertEquals(3, actualizado.rev)
        assertTrue(actualizado.sincronizado)
    }

    @Test
    fun elMapeoDeRoomAlDominioConservaLosCampos() {
        val evento: Evento = vacunacion().copy(idCriaResultante = "cria-1").toDomain()

        assertEquals("Aftosa", evento.biologico)
        assertEquals("2 ml", evento.dosis)
        assertEquals("Subcutánea", evento.via)
        assertEquals("cria-1", evento.idCriaResultante)
        assertEquals("cria-1", evento.toEntity(fechaModificacion = 1L, idVersion = "v").idCriaResultante)
    }
}
