package com.devcris80.prototipo.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.devcris80.prototipo.ui.components.BovinaBottomBar
import com.devcris80.prototipo.ui.components.DestinoBarra
import com.devcris80.prototipo.ui.screens.AsociarChapetaScreen
import com.devcris80.prototipo.ui.screens.BandejaRechazosScreen
import com.devcris80.prototipo.ui.screens.EditarPesajeScreen
import com.devcris80.prototipo.ui.screens.CrearCuentaScreen
import com.devcris80.prototipo.ui.screens.DetalleAnimalScreen
import com.devcris80.prototipo.ui.screens.EditarAnimalScreen
import com.devcris80.prototipo.ui.screens.EscanearScreen
import com.devcris80.prototipo.ui.screens.EventoFormScreen
import com.devcris80.prototipo.ui.screens.EventosScreen
import com.devcris80.prototipo.ui.screens.IndicadoresScreen
import com.devcris80.prototipo.ui.screens.InvitarOperarioScreen
import com.devcris80.prototipo.ui.screens.ListaAnimalesScreen
import com.devcris80.prototipo.ui.screens.LoginScreen
import com.devcris80.prototipo.ui.screens.PesajeFormScreen
import com.devcris80.prototipo.ui.screens.PesosScreen
import com.devcris80.prototipo.ui.screens.RegistrarAnimalScreen
import com.devcris80.prototipo.ui.screens.RegistrarUbicacionFincaScreen
import com.devcris80.prototipo.ui.screens.ResolverConflictoScreen
import com.devcris80.prototipo.ui.util.appViewModel

private val RUTAS_CON_NAV_INFERIOR = setOf(
    Rutas.ListaAnimales.ruta,
    Rutas.Escanear.ruta,
    Rutas.Eventos.ruta,
    Rutas.EstadoServicio.ruta,
)

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.hierarchy?.firstOrNull()?.route
    val inicioViewModel = appViewModel { container -> InicioViewModel(container.cuentaRepository, container.sesionRepository) }
    // InicioViewModel decide una sola vez; después, iniciar sesión navega desde el Login, y perder la
    // sesión (cerrarla o que el servidor rechace renovarla) vuelve al Login desde cualquier pantalla. Crear
    // cuenta también se queda: un intento fallido cierra la sesión que abrió, y la persona sigue en su formulario.
    val puedeEntrar by inicioViewModel.puedeEntrar.collectAsState()
    LaunchedEffect(inicioViewModel) {
        inicioViewModel.sesionPerdida.collect {
            if (navController.currentDestination?.route !in setOf(Rutas.Login.ruta, Rutas.CrearCuenta.ruta)) {
                navController.navigate(Rutas.Login.ruta) { popUpTo(0) { inclusive = true } }
            }
        }
    }
    if (puedeEntrar == null) return

    Scaffold(
        bottomBar = {
            if (currentRoute in RUTAS_CON_NAV_INFERIOR) {
                BovinaBottomBar(
                    destinos = listOf(
                        DestinoBarra(
                            etiqueta = "Animales",
                            icono = Icons.Filled.Pets,
                            seleccionado = currentRoute == Rutas.ListaAnimales.ruta,
                            onClick = {
                                navController.navigate(Rutas.ListaAnimales.ruta) {
                                    popUpTo(Rutas.ListaAnimales.ruta) { inclusive = true }
                                }
                            },
                        ),
                        DestinoBarra(
                            etiqueta = "Escanear",
                            icono = Icons.Filled.Nfc,
                            seleccionado = currentRoute == Rutas.Escanear.ruta,
                            onClick = {
                                navController.navigate(Rutas.Escanear.ruta) {
                                    popUpTo(Rutas.ListaAnimales.ruta)
                                }
                            },
                        ),
                        DestinoBarra(
                            etiqueta = "Eventos",
                            icono = Icons.Filled.EventAvailable,
                            seleccionado = currentRoute == Rutas.Eventos.ruta,
                            onClick = {
                                navController.navigate(Rutas.Eventos.ruta) {
                                    popUpTo(Rutas.ListaAnimales.ruta)
                                }
                            },
                        ),
                        DestinoBarra(
                            etiqueta = "Pesos",
                            icono = Icons.Filled.MonitorWeight,
                            seleccionado = currentRoute == Rutas.EstadoServicio.ruta,
                            onClick = {
                                navController.navigate(Rutas.EstadoServicio.ruta) {
                                    popUpTo(Rutas.ListaAnimales.ruta)
                                }
                            },
                        ),
                    ),
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (puedeEntrar == true) Rutas.ListaAnimales.ruta else Rutas.Login.ruta,
            modifier = Modifier.padding(innerPadding),
        ) {
            // Se llega desde Iniciar sesión, así que al crearla salen las dos de la pila.
            composable(Rutas.CrearCuenta.ruta) {
                CrearCuentaScreen(
                    onCuentaCreada = {
                        navController.navigate(Rutas.ListaAnimales.ruta) {
                            popUpTo(Rutas.Login.ruta) { inclusive = true }
                        }
                    },
                    onIrALogin = { navController.popBackStack(Rutas.Login.ruta, inclusive = false) },
                )
            }
            composable(Rutas.Login.ruta) {
                LoginScreen(
                    onSesionIniciada = {
                        navController.navigate(Rutas.ListaAnimales.ruta) {
                            popUpTo(Rutas.Login.ruta) { inclusive = true }
                        }
                    },
                    onCrearCuenta = { navController.navigate(Rutas.CrearCuenta.ruta) },
                )
            }
            composable(Rutas.ListaAnimales.ruta) {
                ListaAnimalesScreen(
                    onAnimalClick = { idAnimal -> navController.navigate(Rutas.DetalleAnimal.crear(idAnimal)) },
                    onNuevoAnimalClick = { navController.navigate(Rutas.NuevoAnimal.crear()) },
                    onPesajesPendientesClick = {
                        navController.navigate(Rutas.EstadoServicio.ruta) {
                            popUpTo(Rutas.ListaAnimales.ruta)
                        }
                    },
                    onIndicadoresClick = { navController.navigate(Rutas.Indicadores.ruta) },
                    onRegistrarUbicacionClick = {
                        navController.navigate(Rutas.UbicacionFinca.ruta)
                    },
                    onRechazosClick = { navController.navigate(Rutas.Rechazos.ruta) },
                    onInvitarClick = { navController.navigate(Rutas.InvitarOperario.ruta) },
                )
            }
            composable(Rutas.Rechazos.ruta) {
                BandejaRechazosScreen(
                    onVolver = { navController.popBackStack() },
                    onVerAnimal = { idAnimal -> navController.navigate(Rutas.DetalleAnimal.crear(idAnimal)) },
                )
            }
            composable(Rutas.Indicadores.ruta) {
                IndicadoresScreen(
                    onVolver = { navController.popBackStack() },
                    onAnimalClick = { idAnimal -> navController.navigate(Rutas.DetalleAnimal.crear(idAnimal)) },
                    onPesajesPendientesClick = {
                        navController.navigate(Rutas.EstadoServicio.ruta) {
                            popUpTo(Rutas.ListaAnimales.ruta)
                        }
                    },
                )
            }
            composable(
                route = Rutas.NuevoAnimal.ruta,
                arguments = listOf(
                    navArgument("codigo") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("aviso") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("idMadre") { type = NavType.StringType; nullable = true; defaultValue = null },
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
                    onNuevoPesajeClick = { navController.navigate(Rutas.NuevoPesaje.crear(idAnimal)) },
                    onEditarClick = { navController.navigate(Rutas.EditarAnimal.crear(idAnimal)) },
                    onAsociarChapetaClick = { navController.navigate(Rutas.AsociarChapeta.crear(idAnimal = idAnimal)) },
                    onEditarPesajeClick = { idRegistro -> navController.navigate(Rutas.EditarPesaje.crear(idRegistro)) },
                    onAnimalClick = { otro -> navController.navigate(Rutas.DetalleAnimal.crear(otro)) },
                    onResolverConflictoClick = { entidad, idFila ->
                        navController.navigate(Rutas.ResolverConflicto.crear(entidad, idFila))
                    },
                )
            }
            composable(
                route = Rutas.ResolverConflicto.ruta,
                arguments = listOf(
                    navArgument("entidad") { type = NavType.StringType },
                    navArgument("idFila") { type = NavType.StringType },
                ),
            ) {
                ResolverConflictoScreen(
                    onVolver = { navController.popBackStack() },
                    onResuelto = { navController.popBackStack() },
                )
            }
            composable(
                route = Rutas.EditarAnimal.ruta,
                arguments = listOf(navArgument("idAnimal") { type = NavType.StringType }),
            ) {
                EditarAnimalScreen(
                    onGuardado = { navController.popBackStack() },
                    onCancelar = { navController.popBackStack() },
                )
            }
            composable(
                route = Rutas.NuevoEvento.ruta,
                arguments = listOf(
                    navArgument("idAnimal") { type = NavType.StringType; nullable = true; defaultValue = null },
                ),
            ) {
                EventoFormScreen(
                    onGuardado = { navController.popBackStack() },
                    onVolver = { navController.popBackStack() },
                    onRegistrarCria = { idMadre -> navController.navigate(Rutas.NuevoAnimal.crear(idMadre = idMadre)) },
                )
            }
            composable(
                route = Rutas.NuevoPesaje.ruta,
                arguments = listOf(navArgument("idAnimal") { type = NavType.StringType }),
            ) {
                PesajeFormScreen(
                    onGuardado = { navController.popBackStack() },
                    onVolver = { navController.popBackStack() },
                )
            }
            composable(
                route = Rutas.EditarPesaje.ruta,
                arguments = listOf(navArgument("idRegistro") { type = NavType.StringType }),
            ) {
                EditarPesajeScreen(
                    onTerminado = { navController.popBackStack() },
                    onVolver = { navController.popBackStack() },
                )
            }
            composable(Rutas.Escanear.ruta) {
                EscanearScreen(
                    onAbrirAnimal = { idAnimal -> navController.navigate(Rutas.DetalleAnimal.crear(idAnimal)) },
                    onRegistrar = { codigo, aviso -> navController.navigate(Rutas.NuevoAnimal.crear(codigo, aviso)) },
                    onAsociar = { codigo -> navController.navigate(Rutas.AsociarChapeta.crear(codigo = codigo)) },
                )
            }
            composable(
                route = Rutas.AsociarChapeta.ruta,
                arguments = listOf(
                    navArgument("idAnimal") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("codigo") { type = NavType.StringType; nullable = true; defaultValue = null },
                ),
            ) { entry ->
                // Desde el Detalle se vuelve a él; desde Escanear se abre el Detalle del animal que recibió la chapeta.
                val desdeDetalle = entry.arguments?.getString("idAnimal") != null
                AsociarChapetaScreen(
                    onAsociada = { idAnimal ->
                        if (desdeDetalle) {
                            navController.popBackStack()
                        } else {
                            navController.navigate(Rutas.DetalleAnimal.crear(idAnimal)) {
                                popUpTo(Rutas.AsociarChapeta.ruta) { inclusive = true }
                            }
                        }
                    },
                    onVolver = { navController.popBackStack() },
                )
            }
            composable(Rutas.Eventos.ruta) {
                EventosScreen(
                    onRegistrarClick = { navController.navigate(Rutas.NuevoEvento.crear()) },
                )
            }
            composable(Rutas.EstadoServicio.ruta) {
                PesosScreen(
                    onRegistrarAnimalConCodigo = { codigo -> navController.navigate(Rutas.NuevoAnimal.crear(codigo)) },
                )
            }
            composable(Rutas.InvitarOperario.ruta) {
                InvitarOperarioScreen(onVolver = { navController.popBackStack() })
            }
            composable(Rutas.UbicacionFinca.ruta) {
                RegistrarUbicacionFincaScreen(
                    onVolver = { navController.popBackStack() },
                    onUbicacionGuardada = { navController.popBackStack() },
                )
            }
        }
    }
}
