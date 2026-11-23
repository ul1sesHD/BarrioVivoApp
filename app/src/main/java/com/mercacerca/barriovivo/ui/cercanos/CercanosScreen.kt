package com.mercacerca.barriovivo.ui.cercanos

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mercacerca.barriovivo.R
import com.mercacerca.barriovivo.core.geo.Geo
import com.mercacerca.barriovivo.core.location.FusedLocationProvider
import com.mercacerca.barriovivo.domain.model.MejorDia
import com.mercacerca.barriovivo.domain.model.Tianguis
import com.mercacerca.barriovivo.domain.model.TianguisCercano
import com.mercacerca.barriovivo.ui.components.SelectorDia
import com.mercacerca.barriovivo.ui.theme.Amarillo
import com.mercacerca.barriovivo.ui.theme.AzulNoche

@Composable
fun CercanosScreen(
    contentPadding: PaddingValues,
    onAbrirFicha: (Tianguis) -> Unit,
    viewModel: CercanosViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var verMejorDia by rememberSaveable { mutableStateOf(false) }

    val pedirPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { r -> viewModel.onUbicacion(permisoConcedido = r.values.any { it }) }

    state.mensaje?.let { msg ->
        val texto = stringResource(msg)
        LaunchedEffect(msg) {
            snackbar.showSnackbar(texto)
            viewModel.onMensajeMostrado()
        }
    }

    Box(Modifier.fillMaxSize().padding(bottom = contentPadding.calculateBottomPadding())) {
        Column(Modifier.fillMaxSize()) {
            SelectorDia(
                titulo = stringResource(R.string.nav_cercanos),
                seleccionado = state.dia,
                onDia = viewModel::onDia,
                subtitulo = stringResource(
                    if (state.desdeMiUbicacion) R.string.cercanos_desde_ti else R.string.cercanos_desde_zocalo,
                    state.cercanos.size,
                    state.dia.largo,
                ),
            )
            if (state.cargando) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (!state.desdeMiUbicacion) item {
                        BannerUbicacion(state.buscandoUbicacion) { pedirPermiso.launch(FusedLocationProvider.PERMISOS) }
                    }
                    state.mejorDia?.let { item { BannerMejorDia(it, onClick = { verMejorDia = true }) } }
                    items(state.cercanos, key = { it.tianguis.id }) { c ->
                        FilaCercano(c, onClick = { onAbrirFicha(c.tianguis) })
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    val mejorDia = state.mejorDia
    if (verMejorDia && mejorDia != null) {
        MejorDiaSheet(
            mejorDia = mejorDia,
            seleccionado = state.dia,
            desdeMiUbicacion = state.desdeMiUbicacion,
            onElegir = { viewModel.onDia(it); verMejorDia = false },
            onCerrar = { verMejorDia = false },
        )
    }
}

@Composable
private fun BannerUbicacion(buscando: Boolean, onActivar: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.cercanos_activa_ubicacion), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.size(8.dp))
            Button(onClick = onActivar, enabled = !buscando, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                if (buscando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else Icon(Icons.Filled.LocationOn, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.cercanos_usar_mi_ubicacion))
            }
        }
    }
}

/** Cartel rosa que invita a abrir el detalle de "Mejor día". */
@Composable
private fun BannerMejorDia(m: MejorDia, onClick: () -> Unit) {
    val radio = m.radioKm.toInt()
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        border = BorderStroke(2.dp, AzulNoche),
        shadowElevation = 4.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            val mejor = m.mejor
            if (mejor != null) {
                Text(
                    stringResource(R.string.mejor_dia_titulo, mejor.dia.largo).uppercase(),
                    style = MaterialTheme.typography.headlineLarge,
                    color = Amarillo,
                )
                Text(
                    pluralStringResource(R.plurals.mejor_dia_conteo, mejor.cantidad, mejor.cantidad, radio),
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                Text(stringResource(R.string.mejor_dia_ninguno, radio), style = MaterialTheme.typography.bodyLarge)
            }
            Text(stringResource(R.string.mejor_dia_ver), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun FilaCercano(c: TianguisCercano, onClick: () -> Unit) {
    val t = c.tianguis
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Distancia como cartelito de precio.
            Surface(shape = MaterialTheme.shapes.small, color = Amarillo, contentColor = AzulNoche, border = BorderStroke(2.dp, AzulNoche)) {
                Column(Modifier.width(84.dp).padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(Geo.formatearDistancia(c.distanciaKm).uppercase(), style = MaterialTheme.typography.headlineSmall, maxLines = 1)
                    Text(
                        if (c.esCaminable) stringResource(R.string.cercanos_minutos, c.minutosCaminando)
                        else stringResource(R.string.cercanos_lejos),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(t.titulo, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (t.nombre != null) {
                    Text(t.ubicacion, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    if (t.horaInicio != null && t.horaFin != null) stringResource(R.string.ficha_horario, t.horaInicio, t.horaFin)
                    else stringResource(R.string.ficha_sin_horario),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
