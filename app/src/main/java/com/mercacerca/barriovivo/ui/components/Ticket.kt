package com.mercacerca.barriovivo.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.mercacerca.barriovivo.ui.theme.AzulNoche
import com.mercacerca.barriovivo.ui.theme.BarrioVivoTheme
import com.mercacerca.barriovivo.ui.theme.Blanco
import kotlin.random.Random

/** Papel de recibo: borde inferior en zigzag (dientes de [diente] de ancho y [alto] de alto). */
class TicketShape(private val diente: Dp = 14.dp, private val alto: Dp = 8.dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val h = with(density) { alto.toPx() }
        val dientes = (size.width / with(density) { diente.toPx() }).toInt().coerceAtLeast(1)
        val paso = size.width / dientes
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height - h)
            for (i in dientes downTo 1) {
                lineTo(i * paso - paso / 2, size.height)
                lineTo((i - 1) * paso, size.height - h)
            }
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * Ticket de compra: papel blanco, zigzag abajo y código de barras al final.
 * El papel siempre es claro, así que el contenido se pinta con el tema claro (también en modo oscuro).
 */
@Composable
fun Ticket(modifier: Modifier = Modifier, folio: Int = 7, content: @Composable ColumnScope.() -> Unit) {
    BarrioVivoTheme(dark = false) {
        Surface(modifier, shape = TicketShape(), color = Blanco, contentColor = AzulNoche, shadowElevation = 8.dp) {
            Column(Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 32.dp)) {
                content()
                Spacer(Modifier.height(16.dp))
                CodigoBarras(Modifier.fillMaxWidth().height(44.dp), semilla = folio)
            }
        }
    }
}

/** Separador "- - - - -" de recibo. */
@Composable
fun LineaPunteada(modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(16.dp)) {
        drawLine(
            color = AzulNoche,
            start = Offset(0f, center.y),
            end = Offset(size.width, center.y),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
        )
    }
}

/** Código de barras decorativo; la misma [semilla] siempre dibuja las mismas barras. */
@Composable
fun CodigoBarras(modifier: Modifier = Modifier, semilla: Int = 7) {
    Canvas(modifier) {
        val rnd = Random(semilla)
        var x = 0f
        while (x < size.width) {
            val ancho = (1 + rnd.nextInt(4)).dp.toPx()
            if (rnd.nextBoolean()) drawRect(AzulNoche, Offset(x, 0f), Size(ancho, size.height))
            x += ancho
        }
    }
}
