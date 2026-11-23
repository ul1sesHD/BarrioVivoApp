package com.mercacerca.barriovivo.ui.welcome

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.mercacerca.barriovivo.core.ui.rememberReduceMotion
import com.mercacerca.barriovivo.domain.model.Testimonio
import com.mercacerca.barriovivo.ui.components.PersonaAvatar
import com.mercacerca.barriovivo.ui.components.colorParaNombre
import com.mercacerca.barriovivo.ui.theme.Amarillo
import com.mercacerca.barriovivo.ui.theme.AzulNoche
import com.mercacerca.barriovivo.ui.theme.Bermellon
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.random.Random

private val ANCHO_TARJETA = 216.dp
private val ALTO_CARRIL = 124.dp
private val SEPARACION = 20.dp

/**
 * Pared de cartelitos en movimiento.
 * 1) Entrada: cada tarjeta brinca rápido entre posiciones/giros al azar (desorden volátil).
 * 2) Luego se acomoda con un spring y queda flotando lento en su carril; los carriles
 *    alternan dirección y algunos van en diagonal, para que se puedan leer.
 * Con "quitar animaciones" activo, las tarjetas aparecen quietas.
 */
@Composable
fun ComentariosFlotantes(
    testimonios: List<Testimonio>,
    modifier: Modifier = Modifier,
    cantidad: Int = 10,
) {
    if (testimonios.isEmpty()) return
    val reduceMotion = rememberReduceMotion()
    val reloj = rememberReloj(activo = !reduceMotion)
    val tarjetas = remember(testimonios, cantidad) { List(cantidad) { testimonios[it % testimonios.size] } }

    BoxWithConstraints(modifier.clipToBounds()) {
        val px = with(LocalDensity.current) {
            Medidas(ANCHO_TARJETA.toPx(), ALTO_CARRIL.toPx(), SEPARACION.toPx(), 1.dp.toPx())
        }
        val ancho = constraints.maxWidth.toFloat()
        val alto = constraints.maxHeight.toFloat()
        val carriles = (alto / px.altoCarril).toInt().coerceIn(1, cantidad)
        val porCarril = ceil(cantidad / carriles.toFloat()).toInt()
        // Largo del "circuito" de un carril: cabe la pantalla + una tarjeta, y todas las tarjetas sin encimarse.
        val circuito = maxOf(ancho + px.anchoTarjeta, porCarril * (px.anchoTarjeta + px.separacion))
        val margenSuperior = (alto - carriles * px.altoCarril) / 2

        tarjetas.forEachIndexed { i, testimonio ->
            val carril = i % carriles
            val ruta = Ruta(
                circuito = circuito,
                xInicial = (i / carriles) * circuito / porCarril + carril * px.anchoTarjeta / 3,
                y = margenSuperior + carril * px.altoCarril,
                direccion = if (carril % 2 == 0) -1 else 1,
                velocidad = (14 + (carril * 7) % 12) * px.dp,           // 14–25 dp/s: lento, legible
                pendiente = if (carril % 3 == 2) 28 * px.dp else 0f,   // algunos carriles en diagonal
                anchoTarjeta = px.anchoTarjeta,
                rotacion = ((i * 37) % 9 - 4).toFloat(),                // -4°…4°, como cartel colgado
            )
            key(i) {
                TarjetaFlotante(testimonio, ruta, reloj, indice = i, animar = !reduceMotion, ancho, alto)
            }
        }
    }
}

private data class Medidas(val anchoTarjeta: Float, val altoCarril: Float, val separacion: Float, val dp: Float)

private data class Ruta(
    val circuito: Float,
    val xInicial: Float,
    val y: Float,
    val direccion: Int,
    val velocidad: Float,
    val pendiente: Float,
    val anchoTarjeta: Float,
    val rotacion: Float,
) {
    /** Posición en el segundo [t]: avanza y da la vuelta al salir de la pantalla. */
    fun posicion(t: Float): Offset {
        val avance = (xInicial + direccion * velocidad * t).mod(circuito)
        val progreso = avance / circuito
        return Offset(avance - anchoTarjeta, y + (progreso - 0.5f) * pendiente)
    }
}

@Composable
private fun TarjetaFlotante(
    testimonio: Testimonio,
    ruta: Ruta,
    reloj: State<Float>,
    indice: Int,
    animar: Boolean,
    anchoArea: Float,
    altoArea: Float,
) {
    val desorden = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val giro = remember { Animatable(0f) }
    val opacidad = remember { Animatable(if (animar) 0f else 1f) }

    LaunchedEffect(animar) {
        if (!animar) return@LaunchedEffect
        val rnd = Random(indice * 31 + 7)
        fun saltoAlAzar() = Offset(
            (rnd.nextFloat() - 0.5f) * anchoArea,
            (rnd.nextFloat() - 0.5f) * altoArea * 0.6f,
        )
        val rapido = spring<Offset>(dampingRatio = 0.45f, stiffness = 900f)
        val rapidoGiro = spring<Float>(dampingRatio = 0.45f, stiffness = 900f)

        desorden.snapTo(saltoAlAzar())
        giro.snapTo(rnd.nextFloat() * 60f - 30f)
        delay(indice * 60L)
        launch { opacidad.animateTo(1f, tween(150)) }
        repeat(2) { // desorden: rápido y volátil
            coroutineScope {
                launch { desorden.animateTo(saltoAlAzar(), rapido) }
                launch { giro.animateTo(rnd.nextFloat() * 50f - 25f, rapidoGiro) }
            }
        }
        coroutineScope { // se acomoda en su carril con rebote suave
            launch { desorden.animateTo(Offset.Zero, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessVeryLow)) }
            launch { giro.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessVeryLow)) }
        }
    }

    CartelitoComentario(
        testimonio = testimonio,
        modifier = Modifier
            // Lecturas dentro de lambdas: se actualiza solo layout/dibujo, sin recomponer cada frame.
            .offset {
                val p = ruta.posicion(reloj.value) + desorden.value
                IntOffset(p.x.roundToInt(), p.y.roundToInt())
            }
            .graphicsLayer {
                rotationZ = ruta.rotacion + giro.value
                alpha = opacidad.value
            }
            .width(ANCHO_TARJETA),
    )
}

/** Cartelito de precio de tianguis: amarillo, borde grueso, nombre en lettering rojo. */
@Composable
fun CartelitoComentario(testimonio: Testimonio, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = Amarillo,
        contentColor = AzulNoche,
        border = BorderStroke(2.dp, AzulNoche),
        shadowElevation = 6.dp,
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PersonaAvatar(colorParaNombre(testimonio.autor), size = 32.dp)
                Spacer(Modifier.width(8.dp))
                Text(testimonio.autor.uppercase(), style = MaterialTheme.typography.headlineSmall, color = Bermellon)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = testimonio.texto,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Segundos transcurridos, sincronizado con los frames de la pantalla. */
@Composable
private fun rememberReloj(activo: Boolean): State<Float> {
    val segundos = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(activo) {
        if (!activo) return@LaunchedEffect
        val inicio = withFrameNanos { it } - (segundos.floatValue * 1e9f).toLong()
        while (isActive) withFrameNanos { segundos.floatValue = (it - inicio) / 1e9f }
    }
    return segundos
}
