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
 */
@Composable
inline fun <reified VM : ViewModel> appViewModel(
    noinline crear: CreationExtras.(AppContainer) -> VM,
): VM = viewModel {
    val application = checkNotNull(this[APPLICATION_KEY]) as BovinaApplication
    crear(application.container)
}
