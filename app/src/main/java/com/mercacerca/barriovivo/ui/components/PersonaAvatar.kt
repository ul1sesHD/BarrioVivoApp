package com.mercacerca.barriovivo.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mercacerca.barriovivo.ui.theme.AzulNoche
import com.mercacerca.barriovivo.ui.theme.ColoresAvatar
import kotlin.math.absoluteValue

/** Avatar = ícono de persona sobre un círculo de color. Sin fotos, a propósito. */
@Composable
fun PersonaAvatar(color: Color, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Box(
        modifier = modifier
            .size(size)
            .background(color, CircleShape)
            .border(2.dp, AzulNoche, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Person,
            contentDescription = null, // decorativo: el nombre va al lado
            tint = Color.White,
            modifier = Modifier.fillMaxSize().padding(size / 6), // el ícono crece con el círculo
        )
    }
}

/** Color estable para un nombre (testimonios y reseñas que no traen color propio). */
fun colorParaNombre(nombre: String): Color = ColoresAvatar[nombre.hashCode().absoluteValue % ColoresAvatar.size]
