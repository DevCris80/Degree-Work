package com.devcris80.prototipo.ui.util

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import com.devcris80.prototipo.AppContainer
import com.devcris80.prototipo.BovinaApplication

/**
 * Crea el ViewModel de una pantalla con dependencias del [AppContainer]. Es la única forma de
 * crear ViewModels en la app:
 *
 *     val viewModel = appViewModel { container -> EscanearViewModel(container.resolverDestinoEscaneo) }
 *
 * El bloque recibe los [CreationExtras], así que un ViewModel que necesite los argumentos de
 * navegación puede pedir `createSavedStateHandle()` ahí mismo.
 *
 * El contenedor es el de la cuenta con la sesión abierta cuando se crea el ViewModel (ADR-0006). Uno que vive fuera
 * de una pantalla, y por eso sobrevive a un cambio de cuenta, pasa una [key] que cambia con la cuenta.
 */
@Composable
inline fun <reified VM : ViewModel> appViewModel(
    key: String? = null,
    noinline crear: CreationExtras.(AppContainer) -> VM,
): VM = viewModel(key = key) {
    val application = checkNotNull(this[APPLICATION_KEY]) as BovinaApplication
    crear(application.container)
}
