package com.devcris80.prototipo.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.devcris80.prototipo.data.AppDatabase
import com.devcris80.prototipo.ui.screens.AnimalDetailScreen
import com.devcris80.prototipo.ui.screens.AnimalFormScreen
import com.devcris80.prototipo.ui.screens.AnimalListScreen
import com.devcris80.prototipo.ui.screens.EventoFormScreen
import com.devcris80.prototipo.ui.screens.ServiceStatusScreen
import kotlinx.coroutines.launch

@Composable
fun AppNavHost(database: AppDatabase) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.hierarchy?.firstOrNull()?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == Rutas.ListaAnimales.ruta,
                    onClick = {
                        navController.navigate(Rutas.ListaAnimales.ruta) {
                            popUpTo(Rutas.ListaAnimales.ruta) { inclusive = true }
                        }
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Animales") },
                    label = { Text("Animales") },
                )
                NavigationBarItem(
                    selected = currentRoute == Rutas.EstadoServicio.ruta,
                    onClick = {
                        navController.navigate(Rutas.EstadoServicio.ruta) {
                            popUpTo(Rutas.ListaAnimales.ruta)
                        }
                    },
                    icon = { Icon(Icons.Filled.Settings, contentDescription = "Servicio") },
                    label = { Text("Servicio") },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Rutas.ListaAnimales.ruta,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Rutas.ListaAnimales.ruta) {
                val animales = remember { database.animalDao().observeAll() }
                AnimalListScreen(
                    animales = animales,
                    onAnimalClick = { animal -> navController.navigate(Rutas.DetalleAnimal.crear(animal.idAnimal)) },
                    onNuevoAnimalClick = { navController.navigate(Rutas.NuevoAnimal.ruta) },
                )
            }
            composable(Rutas.NuevoAnimal.ruta) {
                AnimalFormScreen(
                    onGuardar = { animal ->
                        scope.launch { database.animalDao().insert(animal) }
                        navController.popBackStack()
                    },
                )
            }
            composable(
                route = Rutas.DetalleAnimal.ruta,
                arguments = listOf(navArgument("idAnimal") { type = NavType.StringType }),
            ) { entry ->
                val idAnimal = entry.arguments?.getString("idAnimal").orEmpty()
                val animalFlow = remember(idAnimal) { database.animalDao().observeById(idAnimal) }
                val animal by animalFlow.collectAsState(initial = null)
                val eventos = remember(idAnimal) { database.eventoDao().observeByAnimal(idAnimal) }
                val registros = remember(idAnimal) { database.registroDao().observeByAnimal(idAnimal) }
                animal?.let { animalActual ->
                    AnimalDetailScreen(
                        animal = animalActual,
                        eventos = eventos,
                        registros = registros,
                        onNuevoEventoClick = { navController.navigate(Rutas.NuevoEvento.crear(idAnimal)) },
                    )
                }
            }
            composable(
                route = Rutas.NuevoEvento.ruta,
                arguments = listOf(navArgument("idAnimal") { type = NavType.StringType }),
            ) { entry ->
                val idAnimal = entry.arguments?.getString("idAnimal").orEmpty()
                EventoFormScreen(
                    idAnimal = idAnimal,
                    onGuardar = { evento ->
                        scope.launch { database.eventoDao().insert(evento) }
                        navController.popBackStack()
                    },
                )
            }
            composable(Rutas.EstadoServicio.ruta) {
                ServiceStatusScreen()
            }
        }
    }
}
