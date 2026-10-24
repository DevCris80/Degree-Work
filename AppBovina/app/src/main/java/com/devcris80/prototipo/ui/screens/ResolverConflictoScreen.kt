package com.devcris80.prototipo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.createSavedStateHandle
import com.devcris80.prototipo.domain.model.Conflicto
import com.devcris80.prototipo.domain.model.Eleccion
import com.devcris80.prototipo.domain.model.EntidadConRevisiones
import com.devcris80.prototipo.domain.model.FilaEnConflicto
import com.devcris80.prototipo.ui.components.BannerInformativo
import com.devcris80.prototipo.ui.components.BovinaCard
import com.devcris80.prototipo.ui.components.BovinaPrimaryButton
import com.devcris80.prototipo.ui.components.BovinaTopBar
import com.devcris80.prototipo.ui.theme.BovinaScreenGutter
import com.devcris80.prototipo.ui.theme.Prototipo1Theme
import com.devcris80.prototipo.ui.util.aEtiqueta
import com.devcris80.prototipo.ui.util.appViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ResolverConflictoScreen(
    onVolver: () -> Unit,
    onResuelto: () -> Unit,
) {
    val viewModel = appViewModel { container ->
        val savedState = createSavedStateHandle()
        ResolverConflictoViewModel(
            EntidadConRevisiones.valueOf(checkNotNull(savedState.get<String>("entidad"))),
            checkNotNull(savedState.get<String>("idFila")),
            container.conflictoRepository,
            container.resolverConflicto,
        )
    }
    val estado by viewModel.estado.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.eventos.collect { evento ->
            when (evento) {
                ResolverConflictoEvento.Resuelto -> onResuelto()
            }
        }
    }

    ResolverConflictoContent(
        estado = estado,
        onVolver = onVolver,
        onEleccionCambiada = viewModel::onEleccionCambiada,
        onResolver = viewModel::onResolver,
    )
}

@Composable
private fun ResolverConflictoContent(
    estado: ResolverConflictoUiState,
    onVolver: () -> Unit,
    onEleccionCambiada: (Eleccion) -> Unit,
    onResolver: () -> Unit,
) {
    val formatoFecha = androidx.compose.runtime.remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val conflicto = estado.conflicto

    Scaffold(
        topBar = { BovinaTopBar(titulo = "Resolver conflicto", onVolver = onVolver) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(BovinaScreenGutter),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (conflicto == null || estado.yaNoExiste) {
                Text(
                    text = "Este conflicto ya no existe.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }
            BannerInformativo(
                texto = "Dos personas editaron esto a la vez. Elija la versión que queda; las demás se conservan " +
                    "en el historial.",
                icon = Icons.Filled.Info,
            )
            if (estado.hayNuevos) {
                BannerInformativo(
                    texto = "Llegaron más ediciones después de abrir esta pantalla. Quedarán sin resolver.",
                    icon = Icons.Filled.Info,
                )
            }

            val diferentes = conflicto.camposQueCambian()
            OpcionDeVersion(
                titulo = "Versión vigente",
                campos = conflicto.vigente.campos(),
                diferentes = diferentes,
                seleccionada = estado.eleccion == Eleccion.Vigente,
                onClick = { onEleccionCambiada(Eleccion.Vigente) },
            )
            conflicto.siblings.forEachIndexed { indice, siblingEnConflicto ->
                val eleccion = Eleccion.DeSibling(siblingEnConflicto.sibling.idVersion)
                OpcionDeVersion(
                    titulo = "Edición ${indice + 1} · recibida el " +
                        formatoFecha.format(Date(siblingEnConflicto.sibling.fechaRecepcion)),
                    campos = siblingEnConflicto.fila.campos(),
                    diferentes = diferentes,
                    seleccionada = estado.eleccion == eleccion,
                    onClick = { onEleccionCambiada(eleccion) },
                )
            }

            BovinaPrimaryButton(
                texto = "Conservar esta versión",
                onClick = onResolver,
                enabled = estado.puedeResolver,
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Filled.CheckCircle,
            )
        }
    }
}

@Composable
private fun OpcionDeVersion(
    titulo: String,
    campos: List<Pair<String, String>>,
    diferentes: Set<String>,
    seleccionada: Boolean,
    onClick: () -> Unit,
) {
    BovinaCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        containerColor = if (seleccionada) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLowest
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = seleccionada, onClick = onClick)
            Text(text = titulo, style = MaterialTheme.typography.titleSmall)
        }
        campos.forEach { (etiqueta, valor) ->
            val cambia = etiqueta in diferentes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = etiqueta,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = valor,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (cambia) FontWeight.Bold else FontWeight.Normal,
                    color = if (cambia) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/** Las etiquetas de los campos cuyo valor no es el mismo en la vigente y en algún Sibling. */
private fun Conflicto.camposQueCambian(): Set<String> {
    val vigente = this.vigente.campos().toMap()
    return siblings
        .flatMap { sibling -> sibling.fila.campos().filter { (etiqueta, valor) -> vigente[etiqueta] != valor } }
        .map { it.first }
        .toSet()
}

private val FORMATO_FECHA_HORA = ThreadLocal.withInitial { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
private val FORMATO_FECHA = ThreadLocal.withInitial { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

/** Los campos de la fila que se comparan, en el orden en que se muestran. */
private fun FilaEnConflicto.campos(): List<Pair<String, String>> {
    val fechaHora = checkNotNull(FORMATO_FECHA_HORA.get())
    val fecha = checkNotNull(FORMATO_FECHA.get())
    return when (this) {
        is FilaEnConflicto.DeAnimal -> listOf(
            "Nombre" to animal.nombre,
            "Raza" to animal.raza,
            "Sexo" to animal.sexo.aEtiqueta(),
            "Etapa" to animal.etapa.aEtiqueta(),
            "Nacimiento" to fecha.format(Date(animal.fechaNacimiento)) +
                if (animal.fechaNacimientoEsEstimada) " (estimada)" else "",
            "Propósito" to animal.proposito.aEtiqueta(),
            "Padre" to (animal.idPadre?.let(::codigoCorto) ?: "Sin registrar"),
            "Madre" to (animal.idMadre?.let(::codigoCorto) ?: "Sin registrar"),
        )
        is FilaEnConflicto.DeEvento -> listOf(
            "Tipo" to evento.tipoEvento,
            "Fecha" to fechaHora.format(Date(evento.fecha)),
            "Detalle" to evento.detalle,
        )
        is FilaEnConflicto.DeRegistro -> listOf(
            "Peso" to "${registro.peso} kg",
            "Fecha de medición" to fechaHora.format(Date(registro.timestamp)),
        )
    }
}

// El padre y la madre son otros Animales: la fila solo trae su id, así que se muestran por sus primeros caracteres.
private fun codigoCorto(idAnimal: String) = "#" + idAnimal.take(8)

@Preview(showBackground = true)
@Composable
private fun ResolverConflictoPreview() {
    val animal = com.devcris80.prototipo.domain.model.Animal(
        idAnimal = "animal-1",
        idPerfilFinca = "finca",
        nombre = "Lucero",
        raza = "Holstein",
        sexo = com.devcris80.prototipo.domain.model.Sexo.HEMBRA,
        etapa = com.devcris80.prototipo.domain.model.Etapa.VACA,
        fechaNacimiento = 1_600_000_000_000,
        proposito = com.devcris80.prototipo.domain.model.Proposito.LECHE,
    )
    val sibling = com.devcris80.prototipo.domain.model.Sibling(
        idVersion = "v1",
        entidad = EntidadConRevisiones.ANIMAL,
        idFila = "animal-1",
        revBase = 1,
        contenido = "{}",
        idUsuario = "usuario-2",
        fechaRecepcion = 1_700_000_000_000,
    )
    Prototipo1Theme {
        ResolverConflictoContent(
            estado = ResolverConflictoUiState(
                conflicto = Conflicto(
                    entidad = EntidadConRevisiones.ANIMAL,
                    idFila = "animal-1",
                    vigente = FilaEnConflicto.DeAnimal(animal),
                    siblings = listOf(
                        com.devcris80.prototipo.domain.model.SiblingEnConflicto(
                            sibling,
                            FilaEnConflicto.DeAnimal(animal.copy(raza = "Simmental")),
                        ),
                    ),
                ),
                eleccion = Eleccion.DeSibling("v1"),
            ),
            onVolver = {},
            onEleccionCambiada = {},
            onResolver = {},
        )
    }
}
