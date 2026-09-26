package com.devcris80.prototipo.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Formas y espaciado — sección 1 de spec_compose_tanda1.md.
val BovinaCardRadius = 16.dp
val BovinaPillShape = CircleShape
val BovinaCardPadding = 20.dp
val BovinaMinFieldHeight = 54.dp
val BovinaMinTouchTarget = 52.dp
val BovinaScreenGutter = 16.dp

val BovinaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(BovinaCardRadius),
    large = RoundedCornerShape(BovinaCardRadius),
    extraLarge = RoundedCornerShape(28.dp),
)
