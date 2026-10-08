package com.devcris80.prototipo.service

import android.util.Log
import com.devcris80.prototipo.domain.model.Pesaje
import com.devcris80.prototipo.domain.usecase.RegistrarPesaje
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.runBlocking
import org.json.JSONException
import org.json.JSONObject

/**
 * Adaptador HTTP delgado sobre RegistrarPesaje: lee el JSON de POST /pesaje, comprueba que
 * los campos existan y sean del tipo correcto, y traduce el resultado de negocio a la respuesta
 * del contrato con el ESP32 (ver docs/contrato-pesaje.md). No contiene lógica de negocio propia.
 */
class PesajeHttpServer(
    port: Int,
    private val registrarPesaje: RegistrarPesaje,
) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response {
        if (session.method != Method.POST || session.uri != RUTA_PESAJE) {
            return jsonResponse(Response.Status.NOT_FOUND, errorBody("ruta no encontrada"))
        }

        val pesaje = try {
            val postData = HashMap<String, String>()
            session.parseBody(postData)
            leerPesaje(JSONObject(postData["postData"] ?: "{}"))
        } catch (e: JSONException) {
            return jsonResponse(Response.Status.BAD_REQUEST, errorBody("body: no es un objeto JSON valido"))
        } catch (e: CuerpoInvalido) {
            return jsonResponse(Response.Status.BAD_REQUEST, errorBody(e.mensaje))
        }

        return try {
            when (val resultado = runBlocking { registrarPesaje(pesaje) }) {
                is RegistrarPesaje.Resultado.Registrado -> jsonResponse(
                    Response.Status.CREATED,
                    JSONObject().put("status", "ok").put("id_registro", resultado.idRegistro).toString(),
                )
                is RegistrarPesaje.Resultado.CodigoSinChapetaActiva -> jsonResponse(
                    Response.Status.NOT_FOUND,
                    errorBody("chip no asociado"),
                )
                RegistrarPesaje.Resultado.SinUsuario -> jsonResponse(
                    Response.Status.SERVICE_UNAVAILABLE,
                    errorBody("sin sesion activa"),
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al procesar POST $RUTA_PESAJE", e)
            jsonResponse(Response.Status.INTERNAL_ERROR, errorBody("error interno"))
        }
    }

    // id_chip es el nombre del campo en el contrato con el ESP32; en la app es el codigo.
    private fun leerPesaje(json: JSONObject) = Pesaje(
        idLectura = json.texto("id_lectura"),
        codigo = json.texto("id_chip"),
        peso = json.numero("peso").toFloat(),
    )

    private fun JSONObject.texto(campo: String): String {
        val valor = opt(campo) ?: throw CuerpoInvalido("$campo: falta")
        if (valor !is String) throw CuerpoInvalido("$campo: debe ser texto")
        if (valor.isBlank()) throw CuerpoInvalido("$campo: no puede estar vacio")
        return valor
    }

    private fun JSONObject.numero(campo: String): Double {
        val valor = opt(campo) ?: throw CuerpoInvalido("$campo: falta")
        return (valor as? Number)?.toDouble() ?: throw CuerpoInvalido("$campo: debe ser un numero")
    }

    private class CuerpoInvalido(val mensaje: String) : Exception(mensaje)

    private fun errorBody(mensaje: String): String =
        JSONObject().put("status", "error").put("message", mensaje).toString()

    private fun jsonResponse(status: Response.IStatus, body: String): Response =
        newFixedLengthResponse(status, "application/json", body)

    companion object {
        const val DEFAULT_PORT = 8080
        const val RUTA_PESAJE = "/pesaje"
        private const val TAG = "PesajeHttpServer"
    }
}
