package com.devcris80.prototipo.domain.model

data class Animal(
    val idAnimal: String,
    val idPerfilFinca: String,
    val nombre: String,
    val raza: String,
    val sexo: Sexo,
    val etapa: Etapa,
    val fechaNacimiento: Long,
    val fechaNacimientoEsEstimada: Boolean = false,
    val proposito: Proposito,
    val fotoUri: String? = null,
    val fechaBaja: Long? = null,
) {
    init {
        require(etapa.sexo == sexo) { "La etapa $etapa no corresponde al sexo $sexo" }
    }
}
