package com.mercacerca.barriovivo.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mercacerca.barriovivo.ui.theme.Ambar

/** 5 estrellas que se pueden tocar (cada una con área de 48dp). 0 = sin contestar. */
@Composable
fun EstrellasInput(valor: Int, onCambio: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.selectableGroup()) {
        for (n in 1..5) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .selectable(selected = valor == n, role = Role.RadioButton, onClick = { onCambio(n) })
                    .semantics { contentDescription = "$n de 5" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    tint = if (n <= valor) Ambar else Ambar.copy(alpha = 0.25f),
                    modifier = Modifier.size(36.dp),
                )
            }
        }
    }
}
