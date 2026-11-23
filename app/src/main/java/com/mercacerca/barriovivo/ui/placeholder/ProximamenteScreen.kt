package com.mercacerca.barriovivo.ui.placeholder

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mercacerca.barriovivo.R

/** Estado vacío temporal para los destinos que se construyen en fases siguientes. */
@Composable
fun ProximamenteScreen(
    @StringRes titulo: Int,
    @StringRes descripcion: Int,
    contentPadding: PaddingValues,
    @DrawableRes ilustracion: Int = R.drawable.sillas_apiladas,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(contentPadding).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Image(painterResource(ilustracion), contentDescription = null, modifier = Modifier.size(180.dp))
        Text(stringResource(titulo).uppercase(), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
        Text(stringResource(R.string.proximamente), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(descripcion), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    }
}
