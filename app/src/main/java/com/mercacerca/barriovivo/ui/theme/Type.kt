package com.mercacerca.barriovivo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mercacerca.barriovivo.R

/** Títulos, precios y encabezados: la misma letra del logo. */
val BebasNeue = FontFamily(Font(R.font.bebas_neue_regular))

/** Cuerpo y UI. */
val DmSans = FontFamily(
    Font(R.font.dm_sans_regular, FontWeight.Normal),
    Font(R.font.dm_sans_medium, FontWeight.Medium),
    Font(R.font.dm_sans_bold, FontWeight.Bold),
)

private fun titulo(size: Int, line: Int = size + 4) =
    TextStyle(fontFamily = BebasNeue, fontSize = size.sp, lineHeight = line.sp, letterSpacing = 0.5.sp)

private fun cuerpo(size: Int, line: Int, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontFamily = DmSans, fontSize = size.sp, lineHeight = line.sp, fontWeight = weight)

val BarrioTypography = Typography(
    displayLarge = titulo(64),
    displayMedium = titulo(52),
    displaySmall = titulo(42),
    headlineLarge = titulo(36),
    headlineMedium = titulo(30),
    headlineSmall = titulo(26),
    titleLarge = titulo(24),
    titleMedium = cuerpo(16, 24, FontWeight.Bold),
    titleSmall = cuerpo(14, 20, FontWeight.Bold),
    bodyLarge = cuerpo(16, 24),
    bodyMedium = cuerpo(14, 20),
    bodySmall = cuerpo(12, 16),
    labelLarge = cuerpo(14, 20, FontWeight.Bold),
    labelMedium = cuerpo(12, 16, FontWeight.Medium),
    labelSmall = cuerpo(11, 16, FontWeight.Medium),
)
