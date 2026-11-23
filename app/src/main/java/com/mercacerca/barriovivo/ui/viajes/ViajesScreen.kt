package com.mercacerca.barriovivo.ui.viajes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mercacerca.barriovivo.R
import com.mercacerca.barriovivo.domain.model.Co2
import com.mercacerca.barriovivo.domain.model.ResumenViajes
import com.mercacerca.barriovivo.domain.model.Usuario
import com.mercacerca.barriovivo.domain.model.Viaje
import com.mercacerca.barriovivo.ui.components.GranjeroDice
import com.mercacerca.barriovivo.ui.components.LineaPunteada
import com.mercacerca.barriovivo.ui.components.Ticket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ViajesScreen(
    contentPadding: PaddingValues,
    onPorQue: () -> Unit,
    onIniciarSesion: () -> Unit,
    viewModel: ViajesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val usuario = state.usuario
    val r = state.resumen

    Column(
        Modifier
            .fillMaxSize()
            .padding(bottom = contentPadding.calculateBottomPadding())
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            stringResource(R.string.nav_viajes_titulo).uppercase(),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
        )
        if (state.cargando) return@Column

        GranjeroDice(
            when {
                usuario == null -> stringResource(R.string.granjero_sin_sesion)
                r.visitas == 0 -> stringResource(R.string.granjero_sin_viajes, usuario.usuario)
                else -> pluralStringResource(R.plurals.granjero_gracias, r.visitas, usuario.usuario, r.visitas, kg(r.totalCo2Kg))
            }
        )

        if (usuario == null) {
            Button(onClick = onIniciarSesion, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Text(stringResource(R.string.ficha_iniciar_sesion))
            }
        } else {
            TicketDeViajes(usuario, r)
        }

        OutlinedButton(
            onClick = onPorQue,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        ) {
            Text(stringResource(R.string.por_que_titulo), color = MaterialTheme.colorScheme.onBackground)
        }
    }
}

/** La lista de visitas se lee como un ticket de compra; el total es el CO₂ ahorrado. */
@Composable
private fun TicketDeViajes(usuario: Usuario, r: ResumenViajes) {
    Ticket(Modifier.fillMaxWidth(), folio = usuario.id) {
        Text(
            stringResource(R.string.app_name).uppercase(),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            stringResource(R.string.viajes_ticket_encabezado, usuario.usuario),
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        LineaPunteada()
        if (r.viajes.isEmpty()) {
            Text(stringResource(R.string.viajes_vacio), style = MaterialTheme.typography.bodyMedium)
        } else {
            r.viajes.forEach { RenglonViaje(it) }
        }
        LineaPunteada()
        Total(stringResource(R.string.viajes_visitas), r.visitas.toString())
        Total(stringResource(R.string.viajes_km), km(r.totalKm))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(stringResource(R.string.viajes_total_co2), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            Text(
                "${kg(r.totalCo2Kg)} KG",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            stringResource(R.string.viajes_nota_factor, Co2.KG_POR_KM.toString()),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun RenglonViaje(v: Viaje) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(fecha(v.fecha), style = MaterialTheme.typography.labelSmall)
            Text(v.tituloTianguis, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(km(v.distanciaKm), style = MaterialTheme.typography.bodyMedium)
            Text("${kg(v.co2Kg)} kg", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun Total(etiqueta: String, valor: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(etiqueta, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Text(valor, style = MaterialTheme.typography.titleSmall)
    }
}

private fun kg(valor: Double) = String.format(Locale.US, "%.2f", valor)
private fun km(valor: Double) = String.format(Locale.US, "%.1f km", valor)
private fun fecha(millis: Long) = SimpleDateFormat("d MMM yyyy · HH:mm", Locale.forLanguageTag("es-MX")).format(Date(millis))
