package com.devcris80.prototipo.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Etapa
import com.devcris80.prototipo.domain.model.PesajePendiente
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.model.Sexo
import com.devcris80.prototipo.domain.repository.AnimalRepository
import com.devcris80.prototipo.domain.repository.CuentaRepository
import com.devcris80.prototipo.domain.usecase.AsignarPesajesPendientes
import com.devcris80.prototipo.domain.usecase.RegistrarAnimal
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class RegistrarAnimalUiState(
    val nombre: String = "",
    val codigo: String = "",
    val codigoSoloLectura: Boolean = false,
    val aviso: String? = null,
    val raza: String = "",
    val sexo: Sexo? = null,
    val etapa: Etapa? = null,
    val fechaNacimiento: Long? = null,
    val fechaNacimientoEsEstimada: Boolean = false,
    val proposito: Proposito? = null,
    val fotoUri: String? = null,
    val idPadre: String? = null,
    val idMadre: String? = null,
    /** Machos y hembras de la finca, incluidos los dados de baja; primero los activos. */
    val candidatosPadre: List<Animal> = emptyList(),
    val candidatosMadre: List<Animal> = emptyList(),
    val codigoYaActivo: Boolean = false,
    val ofertaPesajesPendientes: OfertaPesajesPendientes? = null,
    val errorAsignacionPendientes: Boolean = false,
    /** Solo al editar: el Animal no existe o está dado de baja. */
    val noEditable: Boolean = false,
) {
    val etapasDisponibles: List<Etapa> get() = sexo?.let(Etapa::de).orEmpty()
    val puedeGuardar: Boolean
        get() = nombre.isNotBlank() && raza.isNotBlank() && sexo != null && etapa != null &&
            fechaNacimiento != null && proposito != null
}

sealed interface RegistrarAnimalEvento {
    data object Guardado : RegistrarAnimalEvento
}

data class OfertaPesajesPendientes(
    val idAnimal: String,
    val nombreAnimal: String,
    val codigo: String,
    val pesajes: List<PesajePendiente>,
)

/** Estado del formulario Registrar Animal: validación, etapas según sexo, y guardado con RegistrarAnimal. */
class RegistrarAnimalViewModel(
    codigoPrecargado: String?,
    avisoAnterior: String?,
    private val cuentaRepository: CuentaRepository,
    animalRepository: AnimalRepository,
    private val registrarAnimal: RegistrarAnimal,
    private val asignarPesajesPendientes: AsignarPesajesPendientes,
) : ViewModel() {
    private val _estado = MutableStateFlow(
        RegistrarAnimalUiState(
            codigo = codigoPrecargado.orEmpty(),
            codigoSoloLectura = codigoPrecargado != null,
            aviso = avisoAnterior,
        ),
    )
    val estado: StateFlow<RegistrarAnimalUiState> = _estado.asStateFlow()

    private val _eventos = Channel<RegistrarAnimalEvento>(Channel.BUFFERED)
    val eventos: Flow<RegistrarAnimalEvento> = _eventos.receiveAsFlow()

    init {
        viewModelScope.launch {
            combine(
                animalRepository.observePorSexo(Sexo.MACHO),
                animalRepository.observePorSexo(Sexo.HEMBRA),
            ) { machos, hembras -> machos to hembras }
                .collect { (machos, hembras) ->
                    _estado.update { it.copy(candidatosPadre = machos, candidatosMadre = hembras) }
                }
        }
    }

    fun onNombreCambiado(valor: String) = _estado.update { it.copy(nombre = valor) }
    fun onCodigoCambiado(valor: String) = _estado.update { it.copy(codigo = valor) }
    fun onRazaCambiada(valor: String) = _estado.update { it.copy(raza = valor) }
    fun onFechaNacimientoCambiada(fecha: Long) = _estado.update { it.copy(fechaNacimiento = fecha) }
    fun onFechaEstimadaCambiada(esEstimada: Boolean) = _estado.update { it.copy(fechaNacimientoEsEstimada = esEstimada) }
    fun onPropositoSeleccionado(proposito: Proposito) = _estado.update { it.copy(proposito = proposito) }
    fun onFotoCapturada(uri: String?) = _estado.update { it.copy(fotoUri = uri) }
    fun onPadreSeleccionado(idAnimal: String?) = _estado.update { it.copy(idPadre = idAnimal) }
    fun onMadreSeleccionada(idAnimal: String?) = _estado.update { it.copy(idMadre = idAnimal) }
    fun onCodigoYaActivoVisto() = _estado.update { it.copy(codigoYaActivo = false) }
    fun onErrorAsignacionPendientesVisto() = _estado.update { it.copy(errorAsignacionPendientes = false) }

    fun onSexoSeleccionado(sexo: Sexo) = _estado.update {
        it.copy(sexo = sexo, etapa = null)
    }

    fun onEtapaSeleccionada(etapa: Etapa) = _estado.update { it.copy(etapa = etapa) }

    fun onGuardar() {
        val actual = _estado.value
        if (!actual.puedeGuardar) return
        viewModelScope.launch {
            val idPerfilFinca = cuentaRepository.getPerfilFinca()?.idPerfilFinca ?: return@launch
            val animal = Animal(
                idAnimal = UUID.randomUUID().toString(),
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
            when (val resultado = registrarAnimal(animal, actual.codigo.takeIf { it.isNotBlank() })) {
                is RegistrarAnimal.Resultado.Exito -> {
                    if (resultado.pesajesPendientes.isEmpty()) {
                        _eventos.send(RegistrarAnimalEvento.Guardado)
                    } else {
                        _estado.update {
                            it.copy(
                                ofertaPesajesPendientes = OfertaPesajesPendientes(
                                    idAnimal = animal.idAnimal,
                                    nombreAnimal = animal.nombre,
                                    codigo = actual.codigo,
                                    pesajes = resultado.pesajesPendientes,
                                ),
                            )
                        }
                    }
                }
                is RegistrarAnimal.Resultado.CodigoYaActivo -> _estado.update { it.copy(codigoYaActivo = true) }
            }
        }
    }

    fun onOfertaPesajesPendientesRespondida(asignar: Boolean) {
        val oferta = _estado.value.ofertaPesajesPendientes ?: return
        _estado.update { it.copy(ofertaPesajesPendientes = null) }
        viewModelScope.launch {
            if (asignar) {
                when (asignarPesajesPendientes(oferta.pesajes.map { it.idPesajePendiente }, oferta.idAnimal)) {
                    AsignarPesajesPendientes.Resultado.Asignados -> Unit
                    AsignarPesajesPendientes.Resultado.AnimalNoDisponible,
                    AsignarPesajesPendientes.Resultado.PendienteNoDisponible,
                    -> _estado.update { it.copy(errorAsignacionPendientes = true) }
                }
            }
            _eventos.send(RegistrarAnimalEvento.Guardado)
        }
    }
}
