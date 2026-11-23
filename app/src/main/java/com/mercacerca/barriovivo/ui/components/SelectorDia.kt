package com.mercacerca.barriovivo.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mercacerca.barriovivo.domain.model.Dia

/**
 * Encabezado amarillo con chips Lun…Dom. Lo usan Mapa y Cercanos.
 * Se pinta debajo de la barra de estado (edge-to-edge) y deja su espacio.
 */
@Composable
fun SelectorDia(
    seleccionado: Dia,
    onDia: (Dia) -> Unit,
    subtitulo: String,
    modifier: Modifier = Modifier,
    titulo: String? = null,
) {
    Surface(modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.background, shadowElevation = 4.dp) {
        Column(Modifier.statusBarsPadding().padding(vertical = 8.dp)) {
            titulo?.let {
                Text(
                    it.uppercase(),
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(Dia.entries) { dia ->
                    val activo = dia == seleccionado
                    FilterChip(
                        selected = activo,
                        onClick = { onDia(dia) },
                        label = { Text(dia.corto.uppercase(), style = MaterialTheme.typography.titleLarge) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = activo,
                            borderColor = MaterialTheme.colorScheme.outline,
                            selectedBorderColor = MaterialTheme.colorScheme.outline,
                            borderWidth = 2.dp,
                            selectedBorderWidth = 2.dp,
                        ),
                        modifier = Modifier.height(48.dp),
                    )
                }
            }
            Text(
                subtitulo,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp),
            )
        }
    }
}
