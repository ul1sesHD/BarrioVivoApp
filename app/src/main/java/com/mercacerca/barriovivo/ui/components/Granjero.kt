package com.mercacerca.barriovivo.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mercacerca.barriovivo.R
import com.mercacerca.barriovivo.ui.theme.AzulNoche
import com.mercacerca.barriovivo.ui.theme.Blanco

/** El granjero "hablando": globo de diálogo arriba y su ilustración abajo. */
@Composable
fun GranjeroDice(texto: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = Blanco,
            contentColor = AzulNoche,
            border = BorderStroke(3.dp, AzulNoche),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(texto, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
        }
        ColaGlobo()
        Image(
            painter = painterResource(R.drawable.granjero),
            contentDescription = stringResource(R.string.granjero_descripcion),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(180.dp)
                .clip(MaterialTheme.shapes.large)
                .border(3.dp, AzulNoche, MaterialTheme.shapes.large),
        )
    }
}

/** El piquito del globo, apuntando hacia el granjero. */
@Composable
private fun ColaGlobo() {
    Canvas(Modifier.size(width = 28.dp, height = 16.dp)) {
        val pico = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width / 2, size.height)
            lineTo(size.width, 0f)
        }
        drawPath(pico, Blanco)
        drawPath(pico, AzulNoche, style = Stroke(width = 3.dp.toPx()))
        // Tapa la línea de arriba para que el pico se vea unido al globo.
        drawLine(Blanco, start = androidx.compose.ui.geometry.Offset(3.dp.toPx(), 0f),
            end = androidx.compose.ui.geometry.Offset(size.width - 3.dp.toPx(), 0f), strokeWidth = 4.dp.toPx())
    }
}
