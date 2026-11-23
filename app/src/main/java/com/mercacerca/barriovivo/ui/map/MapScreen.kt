package com.mercacerca.barriovivo.ui.map

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mercacerca.barriovivo.R
import com.mercacerca.barriovivo.core.location.FusedLocationProvider
import com.mercacerca.barriovivo.domain.model.Tianguis
import com.mercacerca.barriovivo.ui.components.SelectorDia
import com.mercacerca.barriovivo.ui.theme.AzulNoche

@Composable
fun MapScreen(
    contentPadding: PaddingValues,
    onAbrirFicha: (Tianguis) -> Unit,
    viewModel: MapViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
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
            onTianguisClick = onAbrirFicha,
            modifier = Modifier.fillMaxSize(),
        )

        SelectorDia(
            seleccionado = state.dia,
            onDia = viewModel::onDiaSeleccionado,
            subtitulo = if (state.cargando) stringResource(R.string.mapa_cargando)
            else stringResource(R.string.mapa_conteo, state.tianguis.size, state.dia.largo),
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
        }

        Text(
            stringResource(R.string.mapa_osm_credito),
            style = MaterialTheme.typography.labelSmall,
            color = AzulNoche,
            modifier = Modifier.align(Alignment.BottomStart).padding(4.dp),
        )
    }
}
