package com.devcris80.prototipo.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.devcris80.prototipo.ui.screens.CrearCuentaScreen
import com.devcris80.prototipo.ui.screens.DetalleAnimalScreen
import com.devcris80.prototipo.ui.screens.EscanearScreen
import com.devcris80.prototipo.ui.screens.EventoFormScreen
import com.devcris80.prototipo.ui.screens.ListaAnimalesScreen
import com.devcris80.prototipo.ui.screens.LoginScreen
import com.devcris80.prototipo.ui.screens.PesajesPendientesScreen
import com.devcris80.prototipo.ui.screens.RegistrarAnimalScreen
import com.devcris80.prototipo.ui.screens.ServiceStatusScreen
import com.devcris80.prototipo.ui.util.appViewModel

private val RUTAS_CON_NAV_INFERIOR = setOf(
    Rutas.ListaAnimales.ruta,
    Rutas.Escanear.ruta,
    Rutas.EstadoServicio.ruta,
)

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.hierarchy?.firstOrNull()?.route
    val inicioViewModel = appViewModel { container -> InicioViewModel(container.cuentaRepository) }
    val tienePerfil by inicioViewModel.tienePerfil.collectAsState()
    if (tienePerfil == null) return

    Scaffold(
        bottomBar = {
            if (currentRoute in RUTAS_CON_NAV_INFERIOR) {
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
                        selected = currentRoute == Rutas.Escanear.ruta,
                        onClick = {
                            navController.navigate(Rutas.Escanear.ruta) {
                                popUpTo(Rutas.ListaAnimales.ruta)
                            }
                        },
                        icon = { Icon(Icons.Filled.Nfc, contentDescription = "Escanear") },
                        label = { Text("Escanear") },
                    )
                    NavigationBarItem(
                        selected = currentRoute == Rutas.EstadoServicio.ruta,
                        onClick = {
                            navController.navigate(Rutas.EstadoServicio.ruta) {
                                popUpTo(Rutas.ListaAnimales.ruta)
                            }
                        },
                        icon = { Icon(Icons.Filled.Settings, contentDescription = "Conexión") },
                        label = { Text("Conexión") },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (tienePerfil == true) Rutas.ListaAnimales.ruta else Rutas.CrearCuenta.ruta,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Rutas.CrearCuenta.ruta) {
                CrearCuentaScreen(
                    onCuentaCreada = {
                        navController.navigate(Rutas.ListaAnimales.ruta) {
                            popUpTo(Rutas.CrearCuenta.ruta) { inclusive = true }
                        }
                    },
                    onIrALogin = { navController.navigate(Rutas.Login.ruta) },
                )
            }
            composable(Rutas.Login.ruta) {
                LoginScreen(
                    onIniciarSesion = {
                        navController.navigate(Rutas.ListaAnimales.ruta) {
                            popUpTo(Rutas.Login.ruta) { inclusive = true }
                        }
                    },
                    onCrearCuentaNueva = { navController.navigate(Rutas.CrearCuenta.ruta) },
                )
            }
            composable(Rutas.ListaAnimales.ruta) {
                ListaAnimalesScreen(
                    onAnimalClick = { idAnimal -> navController.navigate(Rutas.DetalleAnimal.crear(idAnimal)) },
                    onNuevoAnimalClick = { navController.navigate(Rutas.NuevoAnimal.crear()) },
                    onPesajesPendientesClick = { navController.navigate(Rutas.PesajesPendientes.ruta) },
                )
            }
            composable(Rutas.PesajesPendientes.ruta) {
                PesajesPendientesScreen(
                    onVolver = { navController.popBackStack() },
                    onRegistrarAnimalConCodigo = { codigo -> navController.navigate(Rutas.NuevoAnimal.crear(codigo)) },
                )
            }
            composable(
                route = Rutas.NuevoAnimal.ruta,
                arguments = listOf(
                    navArgument("codigo") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("aviso") { type = NavType.StringType; nullable = true; defaultValue = null },
                ),
            ) {
                RegistrarAnimalScreen(
                    onGuardado = { navController.popBackStack() },
                    onCancelar = { navController.popBackStack() },
                )
            }
            composable(
                route = Rutas.DetalleAnimal.ruta,
                arguments = listOf(navArgument("idAnimal") { type = NavType.StringType }),
            ) { entry ->
                val idAnimal = entry.arguments?.getString("idAnimal").orEmpty()
                DetalleAnimalScreen(
                    onVolver = { navController.popBackStack() },
                    onNuevoEventoClick = { navController.navigate(Rutas.NuevoEvento.crear(idAnimal)) },
                )
            }
            composable(
                route = Rutas.NuevoEvento.ruta,
                arguments = listOf(navArgument("idAnimal") { type = NavType.StringType }),
            ) {
                EventoFormScreen(
                    onGuardado = { navController.popBackStack() },
                )
            }
            composable(Rutas.Escanear.ruta) {
                EscanearScreen(
                    onAbrirAnimal = { idAnimal -> navController.navigate(Rutas.DetalleAnimal.crear(idAnimal)) },
                    onRegistrar = { codigo, aviso -> navController.navigate(Rutas.NuevoAnimal.crear(codigo, aviso)) },
                )
            }
            composable(Rutas.EstadoServicio.ruta) {
                ServiceStatusScreen()
            }
        }
    }
}
