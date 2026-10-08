package com.devcris80.prototipo.domain.model

/**
 * Lleva el codigo de una Chapeta a su forma canónica: mayúsculas y sin espacios, `:` ni `-`.
 * Así el mismo tag coincide venga de la lectura NFC, de un codigo escrito a mano o del ESP32.
 * No exige que sea hexadecimal: un codigo de prueba como "TEST001" sigue siendo válido.
 */
fun normalizarCodigo(codigo: String): String =
    codigo.filterNot { it.isWhitespace() || it == ':' || it == '-' }.uppercase()
