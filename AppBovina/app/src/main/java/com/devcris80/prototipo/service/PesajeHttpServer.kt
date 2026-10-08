package com.devcris80.prototipo.service

import android.util.Log
import com.devcris80.prototipo.domain.usecase.RegistrarPesaje
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.runBlocking
import org.json.JSONException
import org.json.JSONObject

/**
 * Adaptador HTTP delgado sobre RegistrarPesaje: parsea el JSON de POST /registro-peso
 * y traduce el resultado de negocio a la respuesta 200/404 del contrato del endpoint. No
 * contiene lógica de negocio propia (ver sección 5 de la spec).
 */
class PesajeHttpServer(
    port: Int,
    private val registrarPesaje: RegistrarPesaje,
) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response {
        if (session.method != Method.POST || session.uri != RUTA_REGISTRO_PESO) {
            return jsonResponse(Response.Status.NOT_FOUND, errorBody("ruta no encontrada"))
        }

        // id_chip es el nombre del campo en el contrato con el ESP32; en la app es el codigo.
        val (codigo, peso) = try {
            val postData = HashMap<String, String>()
            session.parseBody(postData)
            val json = JSONObject(postData["postData"] ?: "{}")
            json.getString("id_chip") to json.getDouble("peso").toFloat()
        } catch (e: JSONException) {
            return jsonResponse(Response.Status.BAD_REQUEST, errorBody("body invalido"))
        }

        return try {
            when (val resultado = runBlocking { registrarPesaje(codigo, peso) }) {
                is RegistrarPesaje.Resultado.Exito -> jsonResponse(
                    Response.Status.OK,
                    JSONObject().put("status", "ok").put("id_registro", resultado.idRegistro).toString(),
                )
                is RegistrarPesaje.Resultado.CodigoSinChapetaActiva -> jsonResponse(
                    Response.Status.NOT_FOUND,
                    errorBody("chip no asociado"),
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al procesar POST $RUTA_REGISTRO_PESO", e)
            jsonResponse(Response.Status.INTERNAL_ERROR, errorBody("error interno"))
        }
    }

    private fun errorBody(mensaje: String): String =
        JSONObject().put("status", "error").put("message", mensaje).toString()

    private fun jsonResponse(status: Response.IStatus, body: String): Response =
        newFixedLengthResponse(status, "application/json", body)

    companion object {
        const val DEFAULT_PORT = 8080
        const val RUTA_REGISTRO_PESO = "/registro-peso"
        private const val TAG = "PesajeHttpServer"
    }
}
