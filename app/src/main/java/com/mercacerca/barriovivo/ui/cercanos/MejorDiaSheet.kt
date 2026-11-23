package com.mercacerca.barriovivo.ui.cercanos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mercacerca.barriovivo.R
import com.mercacerca.barriovivo.domain.model.ConteoDia
import com.mercacerca.barriovivo.domain.model.Dia
import com.mercacerca.barriovivo.domain.model.MejorDia
import com.mercacerca.barriovivo.ui.theme.Ambar

/** Hoja inferior: cuántos tianguis tienes cerca cada día. Tocar un día lo selecciona. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MejorDiaSheet(
    mejorDia: MejorDia,
    seleccionado: Dia,
    desdeMiUbicacion: Boolean,
    onElegir: (Dia) -> Unit,
    onCerrar: () -> Unit,
) {
    val maximo = mejorDia.ranking.maxOf { it.cantidad }.coerceAtLeast(1)
    ModalBottomSheet(onDismissRequest = onCerrar, containerColor = MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(horizontal = 24.dp).navigationBarsPadding().padding(bottom = 16.dp)) {
            Text(
                stringResource(R.string.mejor_dia).uppercase(),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                stringResource(
                    if (desdeMiUbicacion) R.string.mejor_dia_explicacion else R.string.mejor_dia_explicacion_zocalo,
                    mejorDia.radioKm.toInt(),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(16.dp))
            mejorDia.ranking.forEach { conteo ->
                FilaDia(
                    conteo = conteo,
                    fraccion = conteo.cantidad / maximo.toFloat(),
                    esMejor = conteo == mejorDia.mejor,
                    esSeleccionado = conteo.dia == seleccionado,
                    onClick = { onElegir(conteo.dia) },
                )
            }
        }
    }
}

@Composable
private fun FilaDia(conteo: ConteoDia, fraccion: Float, esMejor: Boolean, esSeleccionado: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            conteo.dia.corto.uppercase(),
            style = MaterialTheme.typography.titleLarge,
            color = if (esSeleccionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(48.dp),
        )
        // Barra proporcional; la del mejor día va en rosa, las demás en ámbar.
        Box(Modifier.weight(1f).height(24.dp)) {
            if (conteo.cantidad > 0) {
                Box(
                    Modifier
                        .fillMaxWidth(fraccion)
                        .fillMaxHeight()
                        .background(if (esMejor) MaterialTheme.colorScheme.primary else Ambar, MaterialTheme.shapes.small)
                )
            }
        }
        Text(
            conteo.cantidad.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (esMejor) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.End,
            modifier = Modifier.width(40.dp),
        )
    }
}
