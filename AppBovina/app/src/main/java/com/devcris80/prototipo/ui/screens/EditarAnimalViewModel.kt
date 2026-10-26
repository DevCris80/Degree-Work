package com.devcris80.prototipo.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.model.Sexo
import com.devcris80.prototipo.domain.repository.AnimalRepository
import com.devcris80.prototipo.domain.usecase.ActualizarAnimal
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface EditarAnimalEvento {
    data object Guardado : EditarAnimalEvento
}

/**
 * Estado del formulario Editar Animal. Usa el mismo estado que Registrar Animal: parte de los datos
 * guardados, se carga una sola vez para no pisar lo que la persona va escribiendo, y guarda con
 * ActualizarAnimal. Cambiar el sexo vuelve a pedir la etapa (RN-03).
 */
class EditarAnimalViewModel(
    private val idAnimal: String,
    animalRepository: AnimalRepository,
    private val actualizarAnimal: ActualizarAnimal,
) : ViewModel() {
    private val _estado = MutableStateFlow(RegistrarAnimalUiState())
    val estado: StateFlow<RegistrarAnimalUiState> = _estado.asStateFlow()

    private val _eventos = Channel<EditarAnimalEvento>(Channel.BUFFERED)
    val eventos: Flow<EditarAnimalEvento> = _eventos.receiveAsFlow()

    private var idPerfilFinca: String? = null
    private var animalCargado: Animal? = null

    init {
        viewModelScope.launch {
            val animal = animalRepository.findById(idAnimal)
            if (animal == null || !animal.activo) {
                _estado.update { it.copy(noEditable = true) }
            } else {
                idPerfilFinca = animal.idPerfilFinca
                animalCargado = animal
                val sexoBloqueado = animalRepository.tieneCrias(idAnimal)
                _estado.update { it.alCargar(animal).copy(sexoBloqueado = sexoBloqueado) }
            }
        }
        viewModelScope.launch {
            combine(
                animalRepository.observePorSexo(Sexo.MACHO),
                animalRepository.observePorSexo(Sexo.HEMBRA),
            ) { machos, hembras -> machos.sinElPropio() to hembras.sinElPropio() }
                .collect { (machos, hembras) ->
                    _estado.update { it.copy(candidatosPadre = machos, candidatosMadre = hembras) }
                }
        }
    }

    private fun List<Animal>.sinElPropio() = filter { it.idAnimal != idAnimal }

    private fun RegistrarAnimalUiState.alCargar(animal: Animal) = copy(
        nombre = animal.nombre,
        raza = animal.raza,
        sexo = animal.sexo,
        etapa = animal.etapa,
        fechaNacimiento = animal.fechaNacimiento,
        fechaNacimientoEsEstimada = animal.fechaNacimientoEsEstimada,
        proposito = animal.proposito,
        fotoUri = animal.fotoUri,
        idPadre = animal.idPadre,
        idMadre = animal.idMadre,
    )

    fun onNombreCambiado(valor: String) = _estado.update { it.copy(nombre = valor) }
    fun onRazaCambiada(valor: String) = _estado.update { it.copy(raza = valor) }
    fun onFechaNacimientoCambiada(fecha: Long) = _estado.update { it.copy(fechaNacimiento = fecha) }
    fun onFechaEstimadaCambiada(esEstimada: Boolean) = _estado.update { it.copy(fechaNacimientoEsEstimada = esEstimada) }
    fun onPropositoSeleccionado(proposito: Proposito) = _estado.update { it.copy(proposito = proposito) }
    fun onFotoCapturada(uri: String?) = _estado.update { it.copy(fotoUri = uri) }
    fun onPadreSeleccionado(idPadre: String?) = _estado.update { it.copy(idPadre = idPadre) }
    fun onMadreSeleccionada(idMadre: String?) = _estado.update { it.copy(idMadre = idMadre) }

    fun onSexoSeleccionado(sexo: Sexo) = _estado.update {
        // Elegir el mismo sexo no pide la etapa otra vez: no hay nada que cambiar.
        if (it.sexoBloqueado || it.sexo == sexo) it else it.copy(sexo = sexo, etapa = null)
    }

    fun onEtapaSeleccionada(etapa: Etapa) = _estado.update { it.copy(etapa = etapa) }

    fun onGuardar() {
        val actual = _estado.value
        val idPerfilFinca = idPerfilFinca
        if (!actual.puedeGuardar || idPerfilFinca == null) return
        viewModelScope.launch {
            val animal = Animal(
                idAnimal = idAnimal,
                idPerfilFinca = idPerfilFinca,
                nombre = actual.nombre,
                raza = actual.raza,
                sexo = checkNotNull(actual.sexo),
                etapa = checkNotNull(actual.etapa),
                fechaNacimiento = checkNotNull(actual.fechaNacimiento),
                fechaNacimientoEsEstimada = actual.fechaNacimientoEsEstimada,
                proposito = checkNotNull(actual.proposito),
                fotoUri = actual.fotoUri,
                idPadre = actual.idPadre,
                idMadre = actual.idMadre,
            )
            when (actualizarAnimal(animal)) {
                ActualizarAnimal.Resultado.Actualizado,
                ActualizarAnimal.Resultado.SinCambios,
                -> _eventos.send(EditarAnimalEvento.Guardado)
                ActualizarAnimal.Resultado.NoEditable -> _estado.update { it.copy(noEditable = true) }
                // Llegó una cría mientras se editaba: el formulario vuelve al sexo guardado y lo explica.
                ActualizarAnimal.Resultado.SexoConCrias -> {
                    val cargado = checkNotNull(animalCargado)
                    _estado.update { it.copy(sexo = cargado.sexo, etapa = cargado.etapa, sexoBloqueado = true) }
                }
            }
        }
    }
}
