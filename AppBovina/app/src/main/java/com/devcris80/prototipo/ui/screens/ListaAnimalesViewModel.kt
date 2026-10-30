package com.devcris80.prototipo.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.EntidadConRevisiones
import com.devcris80.prototipo.domain.model.Proposito
import com.devcris80.prototipo.domain.model.Registro
import com.devcris80.prototipo.domain.model.Rol
import com.devcris80.prototipo.domain.repository.AnimalRepository
import com.devcris80.prototipo.domain.repository.ChapetaRepository
import com.devcris80.prototipo.domain.repository.ConflictoRepository
import com.devcris80.prototipo.domain.repository.CuentaRepository
import com.devcris80.prototipo.domain.repository.RechazoRepository
import com.devcris80.prototipo.domain.repository.RegistroRepository
import com.devcris80.prototipo.domain.repository.ResultadoSincronizacion
import com.devcris80.prototipo.domain.repository.SesionRepository
import com.devcris80.prototipo.domain.repository.SincronizacionRepository
import com.devcris80.prototipo.domain.usecase.ObservarPesajesPendientes
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AnimalConResumen(
    val animal: Animal,
    val codigoChapeta: String?,
    val ultimoRegistro: Registro?,
)

data class ListaAnimalesUiState(
    val nombreFinca: String = "",
    val nombreUsuario: String = "",
    val rol: Rol? = null,
    /** Solo el Ganadero edita los datos de la finca (matriz 2.1). */
    val puedeRegistrarUbicacion: Boolean = false,
    /** Solo el Ganadero genera códigos de invitación (RN-18). */
    val puedeInvitar: Boolean = false,
    /** Solo el Ganadero quita Operarios de la finca (RF-37). */
    val puedeQuitarOperarios: Boolean = false,
    val animales: List<AnimalConResumen> = emptyList(),
    val filtro: Proposito? = null,
    val pesajesPendientes: Int = 0,
    /** Los Animales con algún Sibling sin resolver (RN-17). */
    val animalesEnConflicto: Set<String> = emptySet(),
    /** Los cambios que el servidor no aceptó y nadie ha corregido (RN-24). */
    val rechazosAbiertos: Int = 0,
    /** Las filas con cambios guardados en el celular que el servidor todavía no confirmó. */
    val cambiosSinSubir: Int = 0,
    val sincronizando: Boolean = false,
    /** Lo que pasó con la última sincronización, hasta que la pantalla lo muestra. */
    val avisoSincronizacion: String? = null,
)

private data class SincronizacionManual(val sincronizando: Boolean = false, val aviso: String? = null)

/**
 * Estado de la Lista de Animales: finca, usuario, animales activos con su Chapeta y último Registro. Desde
 * aquí se sincroniza a mano ("Sincronizar ahora").
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ListaAnimalesViewModel(
    cuentaRepository: CuentaRepository,
    animalRepository: AnimalRepository,
    chapetaRepository: ChapetaRepository,
    registroRepository: RegistroRepository,
    observarPesajesPendientes: ObservarPesajesPendientes,
    private val sesionRepository: SesionRepository,
    private val sincronizacionRepository: SincronizacionRepository,
    conflictoRepository: ConflictoRepository,
    rechazoRepository: RechazoRepository,
) : ViewModel() {

    private val filtro = MutableStateFlow<Proposito?>(null)
    private val sincronizacion = MutableStateFlow(SincronizacionManual())

    private val animalesConResumen = combine(
        animalRepository.observeActivos(),
        chapetaRepository.observeActivas(),
    ) { animales, chapetas -> animales to chapetas }
        .flatMapLatest { (animales, chapetas) ->
            val codigoPorAnimal = chapetas.associateBy({ it.idAnimal }, { it.codigo })
            if (animales.isEmpty()) {
                flowOf(emptyList())
            } else {
                combine(
                    animales.map { animal ->
                        registroRepository.observeUltimoByAnimal(animal.idAnimal).map { registro ->
                            AnimalConResumen(animal, codigoPorAnimal[animal.idAnimal], registro)
                        }
                    },
                ) { it.toList() }
            }
        }

    val estado: StateFlow<ListaAnimalesUiState> = combine(
        cuentaRepository.observePerfilFinca(),
        cuentaRepository.observeUsuario(),
        animalesConResumen,
        filtro,
        observarPesajesPendientes(),
    ) { perfil, usuario, animales, filtroActual, pendientes ->
        ListaAnimalesUiState(
            nombreFinca = perfil?.nombreFinca.orEmpty(),
            nombreUsuario = usuario?.nombre.orEmpty(),
            rol = usuario?.rol,
            puedeRegistrarUbicacion = usuario?.rol == Rol.GANADERO,
            puedeInvitar = usuario?.rol == Rol.GANADERO,
            puedeQuitarOperarios = usuario?.rol == Rol.GANADERO,
            animales = animales.filter { filtroActual == null || it.animal.proposito == filtroActual },
            filtro = filtroActual,
            pesajesPendientes = pendientes.size,
        )
    }.combine(conflictoRepository.observeFilasEnConflicto(EntidadConRevisiones.ANIMAL)) { estado, enConflicto ->
        estado.copy(animalesEnConflicto = enConflicto)
    }.combine(rechazoRepository.observeAbiertos()) { estado, rechazos ->
        estado.copy(rechazosAbiertos = rechazos.size)
    }.combine(sincronizacionRepository.observeEstado()) { estado, sync ->
        estado.copy(cambiosSinSubir = sync.cambiosSinSubir)
    }.combine(sincronizacion) { estado, sync ->
        estado.copy(sincronizando = sync.sincronizando, avisoSincronizacion = sync.aviso)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ListaAnimalesUiState())

    fun onFiltroSeleccionado(proposito: Proposito?) {
        filtro.value = proposito
    }

    fun onSincronizar() {
        if (sincronizacion.value.sincronizando) return
        sincronizacion.value = SincronizacionManual(sincronizando = true)
        viewModelScope.launch {
            val resultado = sincronizacionRepository.sincronizar()
            // El trabajo en segundo plano espera la señal y sincroniza solo. Con los datos al día, sube y baja las
            // fotos: el aviso no las espera, y siguen aunque se salga de la pantalla.
            if (resultado is ResultadoSincronizacion.Completa || resultado == ResultadoSincronizacion.SinConexion) {
                sincronizacionRepository.programar()
            }
            sincronizacion.value = SincronizacionManual(aviso = resultado.aviso())
        }
    }

    fun onAvisoMostrado() {
        sincronizacion.update { it.copy(aviso = null) }
    }

    // AppNavHost vuelve al Login al perderse la sesión.
    fun onCerrarSesion() {
        viewModelScope.launch { sesionRepository.cerrarSesion() }
    }
}

private fun ResultadoSincronizacion.aviso(): String = when (this) {
    is ResultadoSincronizacion.Completa -> buildString {
        append("Sincronizado.")
        if (enConflicto > 0) append(" $enConflicto en conflicto.")
        if (rechazados > 0) append(" $rechazados rechazados.")
    }
    ResultadoSincronizacion.SinConexion -> "Sin señal. Lo que falta se sincroniza después."
    ResultadoSincronizacion.NoSePudo -> "No se pudo sincronizar. Intenta de nuevo."
    ResultadoSincronizacion.CuentaQuitada -> "Tu cuenta ya no pertenece a esta finca."
}
