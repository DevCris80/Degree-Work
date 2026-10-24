package com.devcris80.prototipo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devcris80.prototipo.domain.model.Animal
import com.devcris80.prototipo.domain.model.Sexo
import com.devcris80.prototipo.domain.usecase.EventosDelMes
import com.devcris80.prototipo.domain.usecase.EvolucionPeso
import com.devcris80.prototipo.domain.usecase.GrupoInventario
import com.devcris80.prototipo.domain.usecase.IndicadoresHato
import com.devcris80.prototipo.domain.usecase.ObservarEvolucionPeso
import com.devcris80.prototipo.domain.usecase.ObservarIndicadoresHato
import com.devcris80.prototipo.ui.components.BovinaCard
import com.devcris80.prototipo.ui.components.BovinaFilterChip
import com.devcris80.prototipo.ui.components.BovinaTopBar
import com.devcris80.prototipo.ui.components.PillBadge
import com.devcris80.prototipo.ui.theme.BovinaForest
import com.devcris80.prototipo.ui.theme.BovinaMinTouchTarget
import com.devcris80.prototipo.ui.theme.BovinaScreenGutter
import com.devcris80.prototipo.ui.util.aEtiqueta
import com.devcris80.prototipo.ui.util.appViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Indicadores del hato (CU-24): inventario, pesajes pendientes, completitud del historial,
 * evolución de peso de un Animal y Eventos por mes.
 */
@Composable
fun IndicadoresScreen(
    onVolver: () -> Unit,
    onAnimalClick: (idAnimal: String) -> Unit,
    onPesajesPendientesClick: () -> Unit,
) {
    val viewModel = appViewModel { container ->
        IndicadoresViewModel(
            container.observarIndicadoresHato,
            container.observarEvolucionPeso,
            container.animalRepository,
            container.chapetaRepository,
            container.sincronizacionRepository,
        )
    }
    val estado by viewModel.estado.collectAsState()
    val formatoFecha = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = { BovinaTopBar(titulo = "Indicadores", onVolver = onVolver) },
    ) { innerPadding ->
        val indicadores = estado.indicadores
        if (indicadores == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = BovinaScreenGutter),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = BovinaScreenGutter),
        ) {
            item { TarjetaInventario(indicadores.inventario) }
            item {
                TarjetaPesajesPendientes(
                    indicadores = indicadores,
                    formatoFecha = formatoFecha,
                    onClick = onPesajesPendientesClick,
                )
            }
            item { TarjetaCompletitud(indicadores = indicadores, onAnimalClick = onAnimalClick) }
            item {
                TarjetaEvolucionPeso(
                    animales = estado.animalesElegibles,
                    idAnimalElegido = estado.idAnimalElegido,
                    evolucion = estado.evolucionPeso,
                    formatoFecha = formatoFecha,
                    onAnimalElegido = viewModel::onAnimalElegido,
                )
            }
            item { TarjetaEventosPorMes(indicadores.eventosPorMes) }
            item {
                Column(modifier = Modifier.padding(start = 4.dp)) {
                    Text(
                        text = when (estado.cambiosSinSubir) {
                            0 -> "No quedan cambios sin subir."
                            1 -> "Queda 1 cambio sin subir."
                            else -> "Quedan ${estado.cambiosSinSubir} cambios sin subir."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Última sincronización: " +
                            (estado.ultimaSincronizacion?.let { formatoFecha.format(Date(it)) } ?: "nunca"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaInventario(inventario: List<GrupoInventario>) {
    BovinaCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Inventario activo", style = MaterialTheme.typography.headlineSmall)
            if (inventario.isNotEmpty()) {
                PillBadge(
                    texto = "${inventario.sumOf { it.cantidad }} animales",
                    containerColor = BovinaForest,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
        if (inventario.isEmpty()) {
            TextoSinDatos("Todavía no hay animales activos.")
            return@BovinaCard
        }
        Text(
            text = Sexo.entries.joinToString(" · ") { sexo ->
                "${sexo.aEtiqueta()}: ${inventario.filter { it.etapa.sexo == sexo }.sumOf { it.cantidad }}"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        inventario.groupBy { it.proposito }.forEach { (proposito, grupos) ->
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
            FilaConteo(
                etiqueta = proposito.aEtiqueta(),
                cantidad = grupos.sumOf { it.cantidad },
                destacada = true,
            )
            grupos.forEach { FilaConteo(etiqueta = it.etapa.aEtiqueta(), cantidad = it.cantidad) }
        }
    }
}

@Composable
private fun TarjetaPesajesPendientes(
    indicadores: IndicadoresHato,
    formatoFecha: SimpleDateFormat,
    onClick: () -> Unit,
) {
    BovinaCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        val masAntiguo = indicadores.pesajePendienteMasAntiguo
        if (masAntiguo == null) {
            Text(text = "Pesajes pendientes sin conciliar", style = MaterialTheme.typography.titleSmall)
            TextoSinDatos("No hay pesajes pendientes.")
        } else {
            FilaConteo(
                etiqueta = "Pesajes pendientes sin conciliar",
                cantidad = indicadores.pesajesPendientes,
                destacada = true,
            )
            Text(
                text = "El más antiguo llegó el ${formatoFecha.format(Date(masAntiguo.timestamp))} " +
                    "(hace ${diasDesde(masAntiguo.timestamp)} días).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun TarjetaCompletitud(
    indicadores: IndicadoresHato,
    onAnimalClick: (idAnimal: String) -> Unit,
) {
    BovinaCard(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Completitud del historial", style = MaterialTheme.typography.headlineSmall)
        if (indicadores.inventario.isEmpty()) {
            TextoSinDatos("Todavía no hay animales activos.")
            return@BovinaCard
        }
        ListaIncompletos(
            titulo = "Sin chapeta activa",
            animales = indicadores.animalesSinChapetaActiva,
            onAnimalClick = onAnimalClick,
        )
        ListaIncompletos(
            titulo = "Sin pesaje en los últimos ${ObservarIndicadoresHato.DIAS_SIN_PESAJE} días",
            animales = indicadores.animalesSinPesajeReciente,
            onAnimalClick = onAnimalClick,
        )
    }
}

@Composable
private fun ListaIncompletos(
    titulo: String,
    animales: List<Animal>,
    onAnimalClick: (idAnimal: String) -> Unit,
) {
    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
    FilaConteo(etiqueta = titulo, cantidad = animales.size, destacada = true)
    if (animales.isEmpty()) {
        TextoSinDatos("Ninguno.")
    }
    animales.forEach { animal ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = BovinaMinTouchTarget)
                .clickable { onAnimalClick(animal.idAnimal) },
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = animal.nombre,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun TarjetaEvolucionPeso(
    animales: List<AnimalConCodigo>,
    idAnimalElegido: String?,
    evolucion: EvolucionPeso?,
    formatoFecha: SimpleDateFormat,
    onAnimalElegido: (idAnimal: String) -> Unit,
) {
    BovinaCard(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Evolución de peso", style = MaterialTheme.typography.headlineSmall)
        if (animales.isEmpty()) {
            TextoSinDatos("Todavía no hay animales activos.")
            return@BovinaCard
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 8.dp),
        ) {
            // Dos Animales con el mismo nombre se distinguen por su Chapeta.
            val nombresRepetidos = animales.groupingBy { it.animal.nombre }.eachCount().filterValues { it > 1 }.keys
            items(animales, key = { it.animal.idAnimal }) { (animal, codigo) ->
                BovinaFilterChip(
                    texto = when {
                        animal.nombre !in nombresRepetidos -> animal.nombre
                        codigo != null -> "${animal.nombre} #$codigo"
                        else -> "${animal.nombre} (sin chapeta)"
                    },
                    seleccionado = animal.idAnimal == idAnimalElegido,
                    onClick = { onAnimalElegido(animal.idAnimal) },
                )
            }
        }
        when {
            evolucion == null -> TextoSinDatos("Elige un animal para ver sus pesajes.")
            evolucion.registros.isEmpty() -> TextoSinDatos("Este animal no tiene pesajes.")
            else -> {
                val ganancia = evolucion.gananciaDiariaKg
                if (ganancia == null) {
                    TextoSinDatos(
                        if (evolucion.registros.size < 2) {
                            "No hay datos suficientes para la ganancia diaria: hace falta otro pesaje."
                        } else {
                            "No hay datos suficientes para la ganancia diaria: los pesajes están a menos de " +
                                "${ObservarEvolucionPeso.DIAS_MINIMOS} día entre el primero y el último."
                        },
                    )
                } else {
                    val kgPorDia = String.format(Locale.getDefault(), "%.1f", ganancia)
                    Text(
                        text = "Ganancia diaria promedio: $kgPorDia kg/día",
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
                evolucion.registros.forEach { registro ->
                    FilaDato(
                        etiqueta = formatoFecha.format(Date(registro.timestamp)),
                        valor = "${registro.peso} kg",
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaEventosPorMes(eventosPorMes: List<EventosDelMes>) {
    val formatoMes = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    BovinaCard(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Eventos por mes", style = MaterialTheme.typography.headlineSmall)
        if (eventosPorMes.isEmpty()) {
            TextoSinDatos("No hay eventos registrados.")
            return@BovinaCard
        }
        eventosPorMes.forEach { delMes ->
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
            FilaConteo(
                etiqueta = formatoMes.format(Date(delMes.mes.inicio())).replaceFirstChar(Char::uppercase),
                cantidad = delMes.porTipo.values.sum(),
                destacada = true,
            )
            delMes.porTipo.entries.sortedByDescending { it.value }.forEach { (tipo, cantidad) ->
                FilaConteo(etiqueta = tipo, cantidad = cantidad)
            }
        }
    }
}

@Composable
private fun FilaConteo(etiqueta: String, cantidad: Int, destacada: Boolean = false) =
    FilaDato(etiqueta = etiqueta, valor = cantidad.toString(), destacada = destacada)

@Composable
private fun FilaDato(etiqueta: String, valor: String, destacada: Boolean = false) {
    val estilo = if (destacada) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = etiqueta, style = estilo, modifier = Modifier.weight(1f))
        Text(text = valor, style = estilo)
    }
}

@Composable
private fun TextoSinDatos(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
}

private fun diasDesde(timestamp: Long): Long =
    TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - timestamp).coerceAtLeast(0)
