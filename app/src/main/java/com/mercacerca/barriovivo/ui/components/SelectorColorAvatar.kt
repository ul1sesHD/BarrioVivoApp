package com.mercacerca.barriovivo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mercacerca.barriovivo.ui.theme.ColoresAvatar

/** Fila de íconos de persona, uno por color; el elegido se ve más grande. */
@Composable
fun SelectorColorAvatar(seleccionado: Int, onElegir: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        ColoresAvatar.forEach { color ->
            val argb = color.toArgb()
            val activo = argb == seleccionado
            Box(
                modifier = Modifier
                    .size(48.dp) // área tocable mínima
                    .clip(CircleShape)
                    .selectable(selected = activo, role = Role.RadioButton, onClick = { onElegir(argb) }),
                contentAlignment = Alignment.Center,
            ) {
                PersonaAvatar(color, size = 36.dp, modifier = Modifier.scale(if (activo) 1.2f else 0.9f))
            }
        }
    }
}
