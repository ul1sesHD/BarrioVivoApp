package com.mercacerca.barriovivo.ui.ficha

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mercacerca.barriovivo.R
import com.mercacerca.barriovivo.core.ui.abrirComoLlegar
import com.mercacerca.barriovivo.domain.model.Promedios
import com.mercacerca.barriovivo.domain.model.Review
import com.mercacerca.barriovivo.domain.model.Tianguis
import com.mercacerca.barriovivo.ui.components.Estrellas
import com.mercacerca.barriovivo.ui.components.PersonaAvatar
import com.mercacerca.barriovivo.ui.components.colorParaNombre
import com.mercacerca.barriovivo.ui.theme.Bermellon
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FichaScreen(onAtras: () -> Unit, viewModel: FichaViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val sinMapas = stringResource(R.string.sin_app_mapas)
    val pideSesion = stringResource(R.string.ficha_confianza_requiere_sesion)

    // El diálogo "¿Quieres ir?" sale una sola vez por visita (sobrevive a girar la pantalla).
    var preguntarSiVa by rememberSaveable { mutableStateOf(true) }

    val irAlTianguis: (Tianguis) -> Unit = { t ->
        val ok = t.coordenadas?.let { context.abrirComoLlegar(it, t.titulo) } ?: false
        if (!ok) scope.launch { snackbar.showSnackbar(sinMapas) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ficha_titulo), style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onAtras) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.atras))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        val t = state.tianguis
        when {
            state.cargando -> Centrado(padding) { CircularProgressIndicator() }
            t == null -> Centrado(padding) { Text(stringResource(R.string.ficha_no_existe)) }
            else -> {
                ContenidoFicha(
                    tianguis = t,
                    promedios = state.promedios,
                    reviews = state.reviews,
                    padding = padding,
                    onComoLlegar = { irAlTianguis(t) },
                    onConfianza = { scope.launch { snackbar.showSnackbar(pideSesion) } },
                )
                if (preguntarSiVa && t.coordenadas != null) {
                    DialogoQuieresIr(
                        tianguis = t,
                        onSi = { preguntarSiVa = false; irAlTianguis(t) },
                        onNo = { preguntarSiVa = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun ContenidoFicha(
    tianguis: Tianguis,
    promedios: Promedios,
    reviews: List<Review>,
    padding: PaddingValues,
    onComoLlegar: () -> Unit,
    onConfianza: () -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Encabezado(tianguis) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onComoLlegar,
                    enabled = tianguis.coordenadas != null,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) {
                    Icon(Icons.Filled.Place, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.ficha_como_llegar), style = MaterialTheme.typography.titleLarge)
                }
                OutlinedButton(
                    onClick = onConfianza,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.ficha_marcar_confianza), color = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
        item { TarjetaPromedios(promedios) }
        if (reviews.isEmpty()) {
            item { SinReviews() }
        } else {
            item {
                Text(stringResource(R.string.ficha_resenas).uppercase(), style = MaterialTheme.typography.headlineMedium)
            }
            items(reviews, key = { it.id }) { TarjetaReview(it) }
        }
    }
}

/** Cartel con nombre, calle, día y horario. */
@Composable
private fun Encabezado(t: Tianguis) {
    Cartel {
        Text(t.titulo, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
        if (t.nombre != null) Text(t.ubicacion, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Etiqueta(t.dia.largo.uppercase())
            Etiqueta(
                if (t.horaInicio != null && t.horaFin != null) stringResource(R.string.ficha_horario, t.horaInicio, t.horaFin)
                else stringResource(R.string.ficha_sin_horario)
            )
        }
        t.nota?.let {
            Spacer(Modifier.height(8.dp))
            Text("⚠ ${it.lowercase().replaceFirstChar(Char::uppercase)}", style = MaterialTheme.typography.bodySmall, color = Bermellon)
        }
    }
}

@Composable
private fun TarjetaPromedios(p: Promedios) {
    Cartel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (p.total == 0) "–" else p.general.unDecimal(),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Estrellas(p.general, size = 24.dp)
                Text(pluralStringResource(R.plurals.ficha_total_resenas, p.total, p.total), style = MaterialTheme.typography.bodyMedium)
                if (p.total > 0) {
                    Text(stringResource(R.string.ficha_pct_confianza, p.porcentajeConfianza), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        if (p.total > 0) {
            Spacer(Modifier.height(8.dp))
            FilaPromedio(stringResource(R.string.pregunta_variedad), p.variedad)
            FilaPromedio(stringResource(R.string.pregunta_tamano), p.tamano)
            FilaPromedio(stringResource(R.string.pregunta_calidad), p.calidad)
        }
    }
}

@Composable
private fun FilaPromedio(etiqueta: String, valor: Double) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(etiqueta, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Estrellas(valor, size = 18.dp)
        Text(valor.unDecimal(), style = MaterialTheme.typography.titleSmall, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
    }
}

@Composable
private fun TarjetaReview(r: Review) {
    Cartel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PersonaAvatar(colorParaNombre(r.autor), size = 36.dp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(r.autor.uppercase(), style = MaterialTheme.typography.titleLarge, color = Bermellon)
                Estrellas(r.promedio, size = 16.dp)
            }
            if (r.confianza) Etiqueta(stringResource(R.string.ficha_de_confianza))
        }
        r.comentario?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.ficha_desglose, r.variedad, r.tamano, r.calidad),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SinReviews() {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Image(painterResource(R.drawable.bancos), contentDescription = null, modifier = Modifier.size(120.dp))
        Text(stringResource(R.string.ficha_sin_resenas), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    }
}

@Composable
private fun DialogoQuieresIr(tianguis: Tianguis, onSi: () -> Unit, onNo: () -> Unit) {
    AlertDialog(
        onDismissRequest = onNo,
        title = { Text(stringResource(R.string.ficha_quieres_ir), style = MaterialTheme.typography.headlineMedium) },
        text = { Text(tianguis.titulo, style = MaterialTheme.typography.bodyLarge) },
        confirmButton = { Button(onClick = onSi) { Text(stringResource(R.string.ficha_si_vamos)) } },
        dismissButton = { TextButton(onClick = onNo) { Text(stringResource(R.string.ficha_ahorita_no)) } },
    )
}

// ---- piezas visuales chicas -------------------------------------------------

/** Tarjeta tipo cartel: papel, borde grueso, radio 16dp. */
@Composable
private fun Cartel(content: @Composable () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun Etiqueta(texto: String) {
    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primary) {
        Text(
            texto,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun Centrado(padding: PaddingValues, content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { content() }
}

private fun Double.unDecimal() = String.format(Locale.US, "%.1f", this)
