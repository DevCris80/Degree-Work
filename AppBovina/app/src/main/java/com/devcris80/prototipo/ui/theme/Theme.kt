package com.devcris80.prototipo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Esquema único (claro) con los tokens de Verdant Acre. No hay variante oscura ni color dinámico.
private val BovinaColorScheme = lightColorScheme(
    primary = BovinaPrimary,
    onPrimary = BovinaOnPrimary,
    primaryContainer = BovinaPrimaryContainer,
    onPrimaryContainer = BovinaOnPrimaryContainer,
    inversePrimary = BovinaInversePrimary,
    secondary = BovinaSecondary,
    onSecondary = BovinaOnPrimary,
    secondaryContainer = BovinaSecondaryContainer,
    onSecondaryContainer = BovinaOnSecondaryContainer,
    tertiary = BovinaTertiary,
    onTertiary = BovinaOnPrimary,
    tertiaryContainer = BovinaTertiaryContainer,
    onTertiaryContainer = BovinaOnTertiaryContainer,
    error = BovinaError,
    onError = BovinaOnPrimary,
    errorContainer = BovinaErrorContainer,
    onErrorContainer = BovinaOnErrorContainer,
    background = BovinaBackground,
    onBackground = BovinaOnSurface,
    surface = BovinaSurface,
    onSurface = BovinaOnSurface,
    surfaceVariant = BovinaSurfaceContainerHighest,
    onSurfaceVariant = BovinaOnSurfaceVariant,
    surfaceDim = BovinaSurfaceDim,
    surfaceBright = BovinaSurface,
    surfaceContainerLowest = BovinaSurfaceContainerLowest,
    surfaceContainerLow = BovinaSurfaceContainerLow,
    surfaceContainer = BovinaSurfaceContainer,
    surfaceContainerHigh = BovinaSurfaceContainerHigh,
    surfaceContainerHighest = BovinaSurfaceContainerHighest,
    inverseSurface = BovinaInverseSurface,
    inverseOnSurface = BovinaInverseOnSurface,
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
