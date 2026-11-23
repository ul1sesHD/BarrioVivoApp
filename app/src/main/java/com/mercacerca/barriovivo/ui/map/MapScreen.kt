package com.mercacerca.barriovivo.ui.map

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mercacerca.barriovivo.R
import com.mercacerca.barriovivo.core.location.FusedLocationProvider
import com.mercacerca.barriovivo.core.ui.abrirComoLlegar
import com.mercacerca.barriovivo.domain.model.Dia
import com.mercacerca.barriovivo.domain.model.Tianguis
import com.mercacerca.barriovivo.ui.theme.AzulNoche

@Composable
fun MapScreen(contentPadding: PaddingValues, viewModel: MapViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }

    // El permiso se pide justo cuando el usuario toca "Cerca de mí" (mínimo privilegio).
    val pedirPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultado -> viewModel.onCercaDeMi(permisoConcedido = resultado.values.any { it }) }

    state.mensaje?.let { msg ->
        val texto = stringResource(msg)
        LaunchedEffect(msg) {
            snackbar.showSnackbar(texto)
            viewModel.onMensajeMostrado()
        }
    }

    // El mapa va debajo de la barra de estado; solo respetamos el espacio de la barra inferior.
    Box(Modifier.fillMaxSize().padding(bottom = contentPadding.calculateBottomPadding())) {
        OsmMap(
            tianguis = state.tianguis,
            miUbicacion = state.miUbicacion,
            camara = state.camara,
            onTianguisClick = viewModel::onTianguisSeleccionado,
            modifier = Modifier.fillMaxSize(),
        )

        SelectorDia(
            seleccionado = state.dia,
            conteo = state.tianguis.size,
            cargando = state.cargando,
            onDia = viewModel::onDiaSeleccionado,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SnackbarHost(snackbar)
            ExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.mapa_cerca_de_mi), style = MaterialTheme.typography.labelLarge) },
                icon = {
                    if (state.buscandoUbicacion) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Filled.LocationOn, contentDescription = null)
                },
                onClick = { pedirPermiso.launch(FusedLocationProvider.PERMISOS) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
            state.seleccionado?.let { t ->
                TarjetaTianguis(
                    tianguis = t,
                    onComoLlegar = {
                        val ok = t.coordenadas?.let { context.abrirComoLlegar(it, t.titulo) } ?: false
                        if (!ok) viewModel.onTianguisSeleccionado(null)
                    },
                    onCerrar = { viewModel.onTianguisSeleccionado(null) },
                )
            }
        }

        Text(
            stringResource(R.string.mapa_osm_credito),
            style = MaterialTheme.typography.labelSmall,
            color = AzulNoche,
            modifier = Modifier.align(Alignment.BottomStart).padding(4.dp),
        )
    }
}

/** Barra amarilla con chips Lun…Dom y cuántos tianguis hay ese día. */
@Composable
private fun SelectorDia(
    seleccionado: Dia,
    conteo: Int,
    cargando: Boolean,
    onDia: (Dia) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.background, shadowElevation = 4.dp) {
        Column(Modifier.statusBarsPadding().padding(vertical = 8.dp)) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(Dia.entries) { dia ->
                    FilterChip(
                        selected = dia == seleccionado,
                        onClick = { onDia(dia) },
                        label = { Text(dia.corto.uppercase(), style = MaterialTheme.typography.titleLarge) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = dia == seleccionado,
                            borderColor = MaterialTheme.colorScheme.outline,
                            borderWidth = 2.dp,
                            selectedBorderWidth = 2.dp,
                            selectedBorderColor = MaterialTheme.colorScheme.outline,
                        ),
                        modifier = Modifier.height(48.dp),
                    )
                }
            }
            Text(
                text = if (cargando) stringResource(R.string.mapa_cargando)
                else stringResource(R.string.mapa_conteo, conteo, seleccionado.largo),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
            )
        }
    }
}

/** Vista previa al tocar un pin. La ficha completa llega en la fase 3. */
@Composable
private fun TarjetaTianguis(tianguis: Tianguis, onComoLlegar: () -> Unit, onCerrar: () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    tianguis.titulo,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onCerrar) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.ficha_cerrar))
                }
            }
            if (tianguis.nombre != null) Text(tianguis.ubicacion, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${tianguis.dia.largo.replaceFirstChar { it.uppercase() }} · " + horario(tianguis),
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onComoLlegar, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Text(stringResource(R.string.ficha_como_llegar))
            }
        }
    }
}

@Composable
private fun horario(t: Tianguis): String =
    if (t.horaInicio != null && t.horaFin != null) stringResource(R.string.ficha_horario, t.horaInicio, t.horaFin)
    else stringResource(R.string.ficha_sin_horario)
