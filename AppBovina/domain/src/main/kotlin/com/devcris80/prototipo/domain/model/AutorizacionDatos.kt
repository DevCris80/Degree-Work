package com.devcris80.prototipo.domain.model

/**
 * El texto de autorización de tratamiento de datos personales (Ley 1581 de 2012, RF-04, RNF-13) que la persona
 * acepta al crear la cuenta. El servidor guarda la [VERSION] aceptada y la fecha: cambiar el texto exige subir la
 * versión y agregarla al servidor con una migración (`privado.texto_autorizacion`).
 */
object AutorizacionDatos {
    const val VERSION = "1.0"

    const val TEXTO =
        "Versión $VERSION, vigente desde el 30 de octubre de 2026.\n\n" +
            "Responsable del tratamiento. Los autores del proyecto MiGanado, Cristian Steven Villamil Rodríguez y " +
            "Mariana Alvarez, en el trabajo de grado ATG202601008 de la Universidad Católica de Colombia. Contacto: " +
            "proyectovacas13@gmail.com.\n\n" +
            "Datos que se recogen. Su nombre, su correo electrónico y su contraseña; el nombre de su finca y, si " +
            "usted lo registra, su ubicación; las fotos de sus animales; y los registros de su ganado: animales, " +
            "chapetas, pesajes y eventos. No se recogen datos sensibles. La contraseña la guarda Supabase Auth, " +
            "protegida: el equipo no puede leerla.\n\n" +
            "Finalidad. Crear y administrar su cuenta; guardar y sincronizar los registros de su finca entre sus " +
            "celulares y el servidor; compartirlos con los Usuarios de la misma finca que usted invite; enviarle los " +
            "correos que la cuenta necesita, como el de recuperar la contraseña; y evaluar el funcionamiento del " +
            "sistema en el trabajo de grado, con resultados agregados que no lo identifican.\n\n" +
            "Dónde se guardan. En este celular y en servidores de Supabase en São Paulo (Brasil), donde Supabase " +
            "actúa como encargado del tratamiento. Al aceptar, usted autoriza esa transmisión de sus datos fuera de " +
            "Colombia. La conexión viaja cifrada. Sus datos no se venden ni se comparten con otros terceros, salvo " +
            "que lo ordene una autoridad competente.\n\n" +
            "Sus derechos. Como titular puede conocer, actualizar y rectificar sus datos; pedir prueba de esta " +
            "autorización; ser informado del uso que se les da; pedir que se supriman y revocar esta autorización, " +
            "siempre que no exista un deber legal de conservarlos; y presentar quejas ante la Superintendencia de " +
            "Industria y Comercio. Para ejercerlos escriba a proyectovacas13@gmail.com: las consultas se responden " +
            "en máximo 10 días hábiles, y los reclamos en máximo 15 días hábiles (Ley 1581, artículos 14 y 15).\n\n" +
            "Conservación. Sus datos se conservan mientras su cuenta esté activa. Si pide la supresión, se eliminan " +
            "sus datos personales del servidor, salvo los que deban conservarse por un deber legal.\n\n" +
            "Esta autorización es facultativa, pero sin ella no se puede crear la cuenta. Si el texto cambia, se " +
            "publica con una versión nueva. Se guardan la fecha de su aceptación y la versión que aceptó."
}
