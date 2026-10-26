package com.devcris80.prototipo.domain.usecase

import com.devcris80.prototipo.domain.model.AutorizacionDatos
import com.devcris80.prototipo.domain.model.CuentaNueva
import com.devcris80.prototipo.domain.repository.ResultadoCreacionCuenta
import com.devcris80.prototipo.domain.repository.SesionRepository

/**
 * Crea la cuenta de una finca nueva (CU-01): la identidad en Supabase Auth y, en el servidor, la finca con su
 * primer Usuario Ganadero (RN-20). Sin aceptar el texto de autorización de la Ley 1581 (RF-04), o con el formulario
 * incompleto, no llama al servidor. La contraseña se envía tal cual; el resto, sin espacios de más.
 */
class CrearCuenta(private val sesionRepository: SesionRepository) {
    suspend operator fun invoke(cuenta: CuentaNueva): ResultadoCreacionCuenta {
        cuenta.faltaParaCrear?.let { return ResultadoCreacionCuenta.Incompleta(it) }
        return sesionRepository.crearCuenta(
            nombre = cuenta.nombre.trim(),
            nombreFinca = cuenta.nombreFinca.trim(),
            correo = cuenta.correo.trim(),
            contrasena = cuenta.contrasena,
            versionAutorizacion = AutorizacionDatos.VERSION,
        )
    }
}
