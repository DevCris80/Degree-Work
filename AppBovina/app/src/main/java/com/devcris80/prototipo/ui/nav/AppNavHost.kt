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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.withTransaction
import com.devcris80.prototipo.data.AppDatabase
import com.devcris80.prototipo.data.PERFIL_FINCA_ID
import com.devcris80.prototipo.data.PerfilFinca
import com.devcris80.prototipo.data.Usuario
import com.devcris80.prototipo.domain.AsociacionChapetaResolver
import com.devcris80.prototipo.ui.screens.DetalleAnimalScreen
import com.devcris80.prototipo.ui.screens.EscanearScreen
import com.devcris80.prototipo.ui.screens.EventoFormScreen
import com.devcris80.prototipo.ui.screens.ListaAnimalesScreen
import com.devcris80.prototipo.ui.screens.LoginScreen
import com.devcris80.prototipo.ui.screens.RegistrarAnimalScreen
import com.devcris80.prototipo.ui.screens.RegistroScreen
import com.devcris80.prototipo.ui.screens.ServiceStatusScreen
import kotlinx.coroutines.launch
import java.util.UUID

private val RUTAS_CON_NAV_INFERIOR = setOf(
    Rutas.ListaAnimales.ruta,
    Rutas.Escanear.ruta,
    Rutas.EstadoServicio.ruta,
)

@Composable
fun AppNavHost(database: AppDatabase) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.hierarchy?.firstOrNull()?.route
    val chapetaResolver = remember { AsociacionChapetaResolver(database.chapetaDao()) }

    var perfilCargado by remember { mutableStateOf(false) }
    var tienePerfil by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        tienePerfil = database.perfilFincaDao().getOnce() != null
        perfilCargado = true
    }
    if (!perfilCargado) return

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
            startDestination = if (tienePerfil) Rutas.ListaAnimales.ruta else Rutas.Registro.ruta,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Rutas.Registro.ruta) {
                RegistroScreen(
                    onCrearCuenta = { nombreUsuario, nombreFinca ->
                        scope.launch {
                            database.withTransaction {
                                database.perfilFincaDao().guardar(PerfilFinca(nombreFinca = nombreFinca))
                                database.usuarioDao().insert(
                                    Usuario(
                                        idUsuario = UUID.randomUUID().toString(),
                                        idPerfilFinca = PERFIL_FINCA_ID,
                                        nombre = nombreUsuario,
                                    ),
                                )
                            }
                            navController.navigate(Rutas.ListaAnimales.ruta) {
                                popUpTo(Rutas.Registro.ruta) { inclusive = true }
                            }
                        }
                    },
                    onIrALogin = { navController.navigate(Rutas.Login.ruta) },
                )
            }
            composable(Rutas.Login.ruta) {
                LoginScreen(
                    usuario = remember { database.usuarioDao().observe() },
                    onIniciarSesion = {
                        navController.navigate(Rutas.ListaAnimales.ruta) {
                            popUpTo(Rutas.Login.ruta) { inclusive = true }
                        }
                    },
                    onCrearCuentaNueva = { navController.navigate(Rutas.Registro.ruta) },
                )
            }
            composable(Rutas.ListaAnimales.ruta) {
                val animales = remember { database.animalDao().observeActivos() }
                val perfilFinca = remember { database.perfilFincaDao().observe() }
                val usuario = remember { database.usuarioDao().observe() }
                val chapetasActivas = remember { database.chapetaDao().observeActivas() }
                ListaAnimalesScreen(
                    perfilFinca = perfilFinca,
                    usuario = usuario,
                    animales = animales,
                    chapetasActivas = chapetasActivas,
                    obtenerUltimoRegistro = { idAnimal -> database.registroDao().observeUltimoByAnimal(idAnimal) },
                    onAnimalClick = { animal -> navController.navigate(Rutas.DetalleAnimal.crear(animal.idAnimal)) },
                    onNuevoAnimalClick = { navController.navigate(Rutas.NuevoAnimal.ruta) },
                )
            }
            composable(Rutas.NuevoAnimal.ruta) {
                RegistrarAnimalScreen(
                    onGuardar = { animal, idChip ->
                        scope.launch {
                            database.animalDao().insert(animal)
                            if (!idChip.isNullOrBlank()) {
                                chapetaResolver.asociar(animal.idAnimal, idChip)
                            }
                        }
                        navController.popBackStack()
                    },
                    onCancelar = { navController.popBackStack() },
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
                    DetalleAnimalScreen(
                        animal = animalActual,
                        eventos = eventos,
                        registros = registros,
                        onVolver = { navController.popBackStack() },
                        onNuevoEventoClick = { navController.navigate(Rutas.NuevoEvento.crear(idAnimal)) },
                        chapetaActiva = remember(idAnimal) { database.chapetaDao().observeActivaByAnimal(idAnimal) },
                        onDarDeBaja = {
                            scope.launch { database.animalDao().darDeBaja(idAnimal, System.currentTimeMillis()) }
                        },
                        onLiberarChapeta = { idChapeta ->
                            scope.launch { database.chapetaDao().desasociar(idChapeta, System.currentTimeMillis()) }
                        },
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
            composable(Rutas.Escanear.ruta) {
                EscanearScreen()
            }
            composable(Rutas.EstadoServicio.ruta) {
                ServiceStatusScreen()
            }
        }
    }
}
