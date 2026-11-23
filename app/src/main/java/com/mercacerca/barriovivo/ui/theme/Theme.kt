package com.mercacerca.barriovivo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val Claro = lightColorScheme(
    primary = RosaMexicano, onPrimary = Blanco,
    primaryContainer = Ambar, onPrimaryContainer = AzulNoche,
    secondary = Bermellon, onSecondary = Blanco,
    tertiary = AzulFuerte, onTertiary = Blanco,
    background = Amarillo, onBackground = AzulNoche,
    surface = Papel, onSurface = AzulNoche,
    surfaceVariant = Blanco, onSurfaceVariant = AzulFuerte,
    surfaceContainer = Papel, surfaceContainerLow = Papel, surfaceContainerLowest = Blanco,
    surfaceContainerHigh = Papel, surfaceContainerHighest = Papel,
    secondaryContainer = RosaMexicano, onSecondaryContainer = Blanco,
    outline = AzulNoche,
    error = RojoOscuro, onError = Blanco,
)

private val Oscuro = darkColorScheme(
    primary = RosaMexicano, onPrimary = Blanco,
    primaryContainer = Ambar, onPrimaryContainer = AzulNoche,
    secondary = Ambar, onSecondary = AzulNoche,
    tertiary = AzulClaro, onTertiary = AzulNoche,
    background = AzulNoche, onBackground = Amarillo,
    surface = AzulFuerte, onSurface = Papel,
    surfaceVariant = AzulFuerte, onSurfaceVariant = Amarillo,
    surfaceContainer = AzulFuerte, surfaceContainerLow = AzulFuerte, surfaceContainerLowest = AzulNoche,
    surfaceContainerHigh = AzulFuerte, surfaceContainerHighest = AzulFuerte,
    secondaryContainer = RosaMexicano, onSecondaryContainer = Blanco,
    outline = Amarillo,
    error = Bermellon, onError = Blanco,
)

/** Rejilla de 4dp; tarjetas con radio 16dp (guía Material 3). */
val BarrioShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun BarrioVivoTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    // Sin dynamic color a propósito: la marca manda sobre el wallpaper.
    MaterialTheme(
        colorScheme = if (dark) Oscuro else Claro,
        typography = BarrioTypography,
        shapes = BarrioShapes,
        content = content,
    )
}
