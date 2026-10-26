package com.devcris80.prototipo.domain.model

/**
 * El texto de autorización de tratamiento de datos personales (Ley 1581 de 2012, RF-04, RNF-13) que la persona
 * acepta al crear la cuenta. El servidor guarda la [VERSION] aceptada y la fecha: cambiar el texto exige subir la
 * versión. Es provisional: el texto definitivo lo entrega el equipo (⚠ en la SRS).
 */
object AutorizacionDatos {
    const val VERSION = "0.1-provisional"

    const val TEXTO =
        "Texto provisional, versión $VERSION.\n\n" +
            "Al crear la cuenta autorizas al equipo del proyecto MiGanado, responsable del tratamiento, a recoger, " +
            "guardar y usar tu nombre, tu correo electrónico, el nombre y la ubicación de tu finca, y los registros " +
            "de tu ganado (animales, pesajes y eventos), conforme a la Ley 1581 de 2012.\n\n" +
            "Finalidad: identificarte, guardar los datos de tu finca y compartirlos con los Usuarios de la misma " +
            "finca a los que invites.\n\n" +
            "Los datos se guardan en este celular y en servidores de Supabase en São Paulo (Brasil). No se " +
            "venden ni se comparten con terceros.\n\n" +
            "Como titular puedes conocer, actualizar, rectificar y pedir que se supriman tus datos, y revocar " +
            "esta autorización, escribiendo a [correo de contacto por definir]. Sin esta autorización no se " +
            "puede crear la cuenta."
}
