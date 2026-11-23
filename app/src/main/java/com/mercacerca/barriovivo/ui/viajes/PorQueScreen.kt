package com.mercacerca.barriovivo.ui.viajes

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mercacerca.barriovivo.R
import com.mercacerca.barriovivo.ui.components.GranjeroDice
import com.mercacerca.barriovivo.ui.theme.AzulFuerte
import com.mercacerca.barriovivo.ui.theme.Bermellon
import com.mercacerca.barriovivo.ui.theme.Cafe
import com.mercacerca.barriovivo.ui.theme.RosaMexicano

private data class Beneficio(@StringRes val titulo: Int, @StringRes val texto: Int, val color: Color)

private val beneficios = listOf(
    Beneficio(R.string.por_que_1_titulo, R.string.por_que_1_texto, RosaMexicano),
    Beneficio(R.string.por_que_2_titulo, R.string.por_que_2_texto, Cafe),
    Beneficio(R.string.por_que_3_titulo, R.string.por_que_3_texto, Bermellon),
    Beneficio(R.string.por_que_4_titulo, R.string.por_que_4_texto, AzulFuerte),
)

/** "¿Por qué ir al mercado?" contado por el granjero. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PorQueScreen(onAtras: () -> Unit, onBuscarTianguis: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.por_que_titulo), style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onAtras) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.atras)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            GranjeroDice(stringResource(R.string.por_que_granjero))
            beneficios.forEach { b ->
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(b.titulo).uppercase(), style = MaterialTheme.typography.headlineMedium, color = b.color)
                        Text(stringResource(b.texto), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            Image(
                painterResource(R.drawable.cartel_amor),
                contentDescription = null, // decorativo
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = onBuscarTianguis, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text(stringResource(R.string.cta_encuentra_mercado).uppercase(), style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
