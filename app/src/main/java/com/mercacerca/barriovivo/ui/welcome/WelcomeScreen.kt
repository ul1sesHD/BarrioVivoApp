package com.mercacerca.barriovivo.ui.welcome

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.mercacerca.barriovivo.R
import com.mercacerca.barriovivo.data.seed.SeedData
import com.mercacerca.barriovivo.domain.model.Testimonio
import com.mercacerca.barriovivo.domain.repository.TestimonioRepository
import com.mercacerca.barriovivo.ui.theme.Amarillo
import com.mercacerca.barriovivo.ui.theme.AzulNoche
import com.mercacerca.barriovivo.ui.theme.BarrioVivoTheme
import com.mercacerca.barriovivo.ui.theme.Blanco
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class WelcomeViewModel @Inject constructor(repo: TestimonioRepository) : ViewModel() {
    val testimonios: StateFlow<List<Testimonio>> = repo.observeTodos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@Composable
fun WelcomeScreen(onContinuar: () -> Unit, viewModel: WelcomeViewModel = hiltViewModel()) {
    val testimonios by viewModel.testimonios.collectAsStateWithLifecycle()
    WelcomeContent(testimonios, onContinuar)
}

@Composable
fun WelcomeContent(testimonios: List<Testimonio>, onContinuar: () -> Unit) {
    // La bienvenida siempre es amarilla con la bolsa (es la marca), también en modo oscuro.
    Box(Modifier.fillMaxSize().background(Amarillo)) {
        Image(
            painter = painterResource(R.drawable.bolsa_mercado),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Column(Modifier.fillMaxSize().systemBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
            LogoCartel(Modifier.padding(top = 16.dp))
            // El Box reserva el espacio aunque los testimonios aún no carguen: el botón no brinca.
            Box(Modifier.weight(1f).fillMaxWidth()) {
                ComentariosFlotantes(testimonios, Modifier.fillMaxSize())
            }
            Button(
                onClick = onContinuar,
                border = BorderStroke(2.dp, AzulNoche),
                modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(min = 64.dp),
            ) {
                Text(
                    stringResource(R.string.cta_encuentra_mercado),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** El logo (versión fondo blanco) colgado como cartel. */
@Composable
private fun LogoCartel(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.rotate(-3f),
        shape = MaterialTheme.shapes.medium,
        color = Blanco,
        border = BorderStroke(3.dp, AzulNoche),
        shadowElevation = 8.dp,
    ) {
        Image(
            painter = painterResource(R.drawable.logo_barrio_vivo_blanco),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier.padding(8.dp).size(128.dp),
        )
    }
}

@Preview(showSystemUi = true)
@Composable
private fun WelcomePreview() = BarrioVivoTheme {
    WelcomeContent(SeedData.testimonios.map { Testimonio(it.id, it.autor, it.texto) }) {}
}
