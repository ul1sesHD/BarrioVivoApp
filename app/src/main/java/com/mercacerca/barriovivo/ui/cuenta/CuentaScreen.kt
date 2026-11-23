package com.mercacerca.barriovivo.ui.cuenta

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mercacerca.barriovivo.R
import com.mercacerca.barriovivo.domain.model.ResumenViajes
import com.mercacerca.barriovivo.domain.model.ReviewPropia
import com.mercacerca.barriovivo.domain.model.Tianguis
import com.mercacerca.barriovivo.domain.model.Usuario
import com.mercacerca.barriovivo.ui.components.Estrellas
import com.mercacerca.barriovivo.ui.components.PersonaAvatar
import com.mercacerca.barriovivo.ui.components.SelectorColorAvatar
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/** Pestaña Perfil: si no hay sesión muestra el ticket de acceso; si la hay, el perfil. */
@Composable
fun CuentaScreen(
    contentPadding: PaddingValues,
    onAbrirFicha: (Int) -> Unit,
    onVerViajes: () -> Unit,
    viewModel: CuentaViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val usuario = state.usuario

    Box(
        Modifier
            .fillMaxSize()
            .padding(bottom = contentPadding.calculateBottomPadding())
            .imePadding()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(16.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        when {
            state.cargando -> CircularProgressIndicator()
            usuario == null -> AccesoContent(
                form = state.form,
                onUsuario = viewModel::onUsuario,
                onPassword = viewModel::onPassword,
                onColor = viewModel::onColor,
                onEnviar = viewModel::onEnviar,
                onCambiarModo = viewModel::onCambiarModo,
                modifier = Modifier.fillMaxWidth(),
            )
            else -> PerfilContent(
                usuario = usuario,
                misReviews = state.misReviews,
                misFavoritos = state.misFavoritos,
                onColor = viewModel::onCambiarColorPerfil,
                onCerrarSesion = viewModel::onCerrarSesion,
                onAbrirFicha = onAbrirFicha,
                viajes = state.viajes,
                onVerViajes = onVerViajes,
            )
        }
    }
}

@Composable
private fun PerfilContent(
    usuario: Usuario,
    misReviews: List<ReviewPropia>,
    misFavoritos: List<Tianguis>,
    onColor: (Int) -> Unit,
    onCerrarSesion: () -> Unit,
    onAbrirFicha: (Int) -> Unit,
    viajes: ResumenViajes,
    onVerViajes: () -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            stringResource(R.string.nav_perfil).uppercase(),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Cartel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PersonaAvatar(Color(usuario.colorArgb), size = 88.dp)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("@${usuario.usuario}", style = MaterialTheme.typography.headlineLarge)
                    Text(
                        stringResource(R.string.perfil_miembro_desde, fecha(usuario.creadoEn)),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Spacer(Modifier.heightIn(min = 12.dp))
            Text(stringResource(R.string.cuenta_color), style = MaterialTheme.typography.titleSmall)
            SelectorColorAvatar(usuario.colorArgb, onColor)
        }
        Cartel {
            Text(
                pluralStringResource(R.plurals.perfil_mis_favoritos, misFavoritos.size, misFavoritos.size).uppercase(),
                style = MaterialTheme.typography.headlineSmall,
            )
            if (misFavoritos.isEmpty()) Text(stringResource(R.string.perfil_sin_favoritos), style = MaterialTheme.typography.bodyMedium)
            misFavoritos.forEach { t ->
                Renglon(onClick = { onAbrirFicha(t.id) }) {
                    Icon(Icons.Filled.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t.titulo, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(t.dia.largo.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        Cartel {
            Text(
                pluralStringResource(R.plurals.perfil_mis_resenas, misReviews.size, misReviews.size).uppercase(),
                style = MaterialTheme.typography.headlineSmall,
            )
            if (misReviews.isEmpty()) Text(stringResource(R.string.perfil_sin_resenas), style = MaterialTheme.typography.bodyMedium)
            misReviews.forEach { p ->
                Renglon(onClick = { onAbrirFicha(p.review.tianguisId) }) {
                    Column(Modifier.weight(1f)) {
                        Text(p.tituloTianguis, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Estrellas(p.review.promedio, size = 16.dp)
                        p.review.comentario?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        Cartel {
            Text(stringResource(R.string.perfil_mi_co2).uppercase(), style = MaterialTheme.typography.headlineSmall)
            Renglon(onClick = onVerViajes) {
                Text(
                    if (viajes.visitas == 0) stringResource(R.string.perfil_sin_viajes)
                    else pluralStringResource(
                        R.plurals.perfil_co2_resumen, viajes.visitas,
                        String.format(Locale.US, "%.2f", viajes.totalCo2Kg), viajes.visitas,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                )
                Text("›", style = MaterialTheme.typography.headlineSmall)
            }
        }
        OutlinedButton(
            onClick = onCerrarSesion,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.perfil_cerrar_sesion), color = MaterialTheme.colorScheme.onBackground)
        }
        Text(
            stringResource(R.string.perfil_privacidad),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Renglón tocable de al menos 48dp de alto. */
@Composable
private fun Renglon(onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onClick).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

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

private fun fecha(millis: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.forLanguageTag("es-MX")).format(Date(millis))
