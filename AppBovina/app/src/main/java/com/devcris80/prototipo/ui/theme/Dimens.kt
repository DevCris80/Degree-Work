package com.devcris80.prototipo.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Formas y espaciado de DESIGN.md: contenedores de 24dp, campos y botones en píldora.
val BovinaCardRadius = 24.dp
val BovinaPillShape = CircleShape
val BovinaCardPadding = 20.dp
val BovinaMinFieldHeight = 52.dp
val BovinaMinTouchTarget = 52.dp
val BovinaScreenGutter = 16.dp

val BovinaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(BovinaCardRadius),
    large = RoundedCornerShape(BovinaCardRadius),
    extraLarge = RoundedCornerShape(32.dp),
)
