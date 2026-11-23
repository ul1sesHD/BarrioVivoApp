package com.mercacerca.barriovivo.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mercacerca.barriovivo.ui.theme.Ambar
import kotlin.math.roundToInt

/** 5 estrellas de solo lectura; el valor se redondea a la estrella más cercana. */
@Composable
fun Estrellas(valor: Double, modifier: Modifier = Modifier, size: Dp = 20.dp, color: Color = Ambar) {
    val llenas = valor.roundToInt().coerceIn(0, 5)
    // Para TalkBack se lee "4 de 5 estrellas" en lugar de 5 íconos sueltos.
    Row(modifier.semantics(mergeDescendants = true) { contentDescription = "$llenas de 5 estrellas" }) {
        repeat(5) { i ->
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                // La estrella "vacía" es la misma, deslavada (el set básico de íconos no trae la hueca).
                tint = if (i < llenas) color else color.copy(alpha = 0.25f),
                modifier = Modifier.size(size),
            )
        }
    }
}
