package com.devcris80.prototipo.domain.model

/**
 * Las entidades que el celular sube al servidor. `Usuario` no está: solo lo cambian funciones del servidor. `FOTO` es
 * la foto de un Animal, que sube aparte de su fila: su Rechazo lleva el `idAnimal`.
 */
enum class EntidadSincronizada { PERFIL_FINCA, ANIMAL, EVENTO, REGISTRO, CHAPETA, PESAJE_PENDIENTE, FOTO }

/** Un cambio local que el servidor no aceptó y espera que una persona lo corrija. */
data class Rechazo(
    val idRechazo: String,
    val entidad: EntidadSincronizada,
    val idFila: String,
    /** La fila completa que se intentó subir, en JSON. */
    val contenido: String,
    val motivo: String,
    val fechaRechazo: Long,
    /** El código de error con que empieza el motivo (supabase/README.md). */
    val codigo: String,
    /** El Animal de lo rechazado: el propio, el de su Chapeta, Evento o Registro, o el de su foto. */
    val idAnimal: String?,
    /** El código de la Chapeta rechazada. */
    val codigoChapeta: String?,
    /** Salió del celular porque el servidor rechazó su Animal (ADR-0004, regla 7). */
    val porSuAnimal: Boolean,
)

/** Por qué el servidor no aceptó el cambio, en los términos en que se le explica a la persona. */
sealed interface MotivoRechazo {
    /** El código ya tiene una Chapeta activa en la finca (RN-01, E-07); nulo si ese Animal no está en el celular. */
    data class CodigoEnOtroAnimal(val nombreOtroAnimal: String?) : MotivoRechazo

    /** El Pesaje pendiente ya se concilió en otro celular. */
    data object YaConciliado : MotivoRechazo

    /** El rol o la finca del usuario no permiten el cambio. */
    data object SinPermiso : MotivoRechazo

    /** El servidor rechazó el Animal del que dependía. */
    data object SeRechazoSuAnimal : MotivoRechazo

    /** Storage no acepta el archivo de la foto. */
    data object FotoNoAceptada : MotivoRechazo

    data class Otro(val mensaje: String) : MotivoRechazo
}

/** Lo que la bandeja le propone a la persona para corregir un Rechazo (CU-22). Todas terminan cerrándolo. */
enum class AccionSugerida {
    /** Asociar otro código al Animal, o cerrar si el código sí es del otro Animal. */
    ASOCIAR_OTRO_CODIGO,

    /** La fila local ya volvió a la versión del servidor: solo falta aceptarla. */
    ACEPTAR_LA_DEL_SERVIDOR,

    /** Cerrar sin reenviar el cambio. */
    CERRAR,
}

/** Un Rechazo abierto como lo muestra la bandeja. [nombreAnimal] es nulo si su Animal ya no está en el celular. */
data class RechazoEnBandeja(
    val rechazo: Rechazo,
    val nombreAnimal: String?,
    val motivo: MotivoRechazo,
    val accion: AccionSugerida,
)
