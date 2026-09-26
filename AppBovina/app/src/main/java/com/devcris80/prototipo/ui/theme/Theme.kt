package com.devcris80.prototipo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Paleta fija de MiGanado (sección 1 de spec_compose_tanda1.md). No hay variante oscura ni
// color dinámico definidos todavía, así que el esquema es único para toda la app.
private val BovinaColorScheme = lightColorScheme(
    primary = BovinaPrimary,
    onPrimary = BovinaOnPrimary,
    primaryContainer = BovinaPrimaryContainer,
    onPrimaryContainer = BovinaOnPrimaryContainer,
    secondary = BovinaSecondary,
    onSecondary = BovinaOnPrimary,
    secondaryContainer = BovinaSecondaryContainer,
    onSecondaryContainer = BovinaOnSecondaryContainer,
    tertiary = BovinaTertiary,
    onTertiary = BovinaOnPrimary,
    error = BovinaError,
    background = BovinaBackground,
    onBackground = BovinaOnSurface,
    surface = BovinaSurface,
    onSurface = BovinaOnSurface,
    surfaceVariant = BovinaSurfaceContainer,
    onSurfaceVariant = BovinaOnSurfaceVariant,
    surfaceContainerLowest = BovinaSurfaceContainerLowest,
    surfaceContainerLow = BovinaSurfaceContainerLow,
    surfaceContainer = BovinaSurfaceContainer,
    outline = BovinaOutline,
    outlineVariant = BovinaOutlineVariant,
)

@Composable
fun Prototipo1Theme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BovinaColorScheme,
        typography = Typography,
        shapes = BovinaShapes,
        content = content
    )
}
