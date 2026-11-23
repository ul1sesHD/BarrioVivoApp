package com.mercacerca.barriovivo.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mercacerca.barriovivo.R
import com.mercacerca.barriovivo.ui.cercanos.CercanosScreen
import com.mercacerca.barriovivo.ui.cuenta.CuentaScreen
import com.mercacerca.barriovivo.ui.evaluar.EvaluarScreen
import com.mercacerca.barriovivo.ui.ficha.FichaScreen
import com.mercacerca.barriovivo.ui.map.MapScreen
import com.mercacerca.barriovivo.ui.viajes.PorQueScreen
import com.mercacerca.barriovivo.ui.viajes.ViajesScreen
import com.mercacerca.barriovivo.ui.welcome.WelcomeScreen
import kotlinx.serialization.Serializable

// Rutas con tipo (type-safe): el compilador avisa si navegas a algo que no existe.
@Serializable data object Bienvenida
@Serializable data object Mapa
@Serializable data object Cercanos
@Serializable data object Viajes
@Serializable data object Perfil
@Serializable data class Ficha(val id: Int)
@Serializable data class Evaluar(val tianguisId: Int)
@Serializable data object PorQue

private enum class Pestana(val ruta: Any, @StringRes val etiqueta: Int, val icono: ImageVector) {
    MAPA(Mapa, R.string.nav_mapa, Icons.Filled.Place),
    CERCANOS(Cercanos, R.string.nav_cercanos, Icons.AutoMirrored.Filled.List),
    VIAJES(Viajes, R.string.nav_viajes, Icons.Filled.ShoppingCart),
    PERFIL(Perfil, R.string.nav_perfil, Icons.Filled.Person),
}

@Composable
fun BarrioVivoNavHost(nav: NavHostController = rememberNavController()) {
    val entrada by nav.currentBackStackEntryAsState()
    val destino = entrada?.destination
    // La barra inferior solo aparece en las 4 pestañas (no en Bienvenida ni en la Ficha).
    val enPestana = destino != null && Pestana.entries.any { destino.hasRoute(it.ruta::class) }

    Scaffold(
        bottomBar = {
            if (enPestana) BarraInferior(
                esActual = { p -> destino.hasRoute(p.ruta::class) },
                onClick = { p -> nav.irAPestana(p.ruta) },
            )
        },
    ) { padding ->
        NavHost(nav, startDestination = Bienvenida) {
            composable<Bienvenida> {
                WelcomeScreen(onContinuar = {
                    nav.navigate(Mapa) { popUpTo(Bienvenida) { inclusive = true } }
                })
            }
            composable<Mapa> { MapScreen(contentPadding = padding, onAbrirFicha = { nav.navigate(Ficha(it.id)) }) }
            composable<Ficha> {
                FichaScreen(
                    onAtras = nav::navigateUp,
                    onIniciarSesion = { nav.irAPestana(Perfil) },
                    onEvaluar = { nav.navigate(Evaluar(it)) },
                    onVerViajes = { nav.irAPestana(Viajes) },
                )
            }
            composable<Evaluar> {
                EvaluarScreen(onListo = nav::navigateUp, onIniciarSesion = { nav.irAPestana(Perfil) })
            }
            composable<Cercanos> { CercanosScreen(contentPadding = padding, onAbrirFicha = { nav.navigate(Ficha(it.id)) }) }
            composable<Viajes> {
                ViajesScreen(
                    contentPadding = padding,
                    onPorQue = { nav.navigate(PorQue) },
                    onIniciarSesion = { nav.irAPestana(Perfil) },
                )
            }
            composable<PorQue> {
                PorQueScreen(onAtras = nav::navigateUp, onBuscarTianguis = { nav.irAPestana(Cercanos) })
            }
            composable<Perfil> {
                CuentaScreen(
                    contentPadding = padding,
                    onAbrirFicha = { nav.navigate(Ficha(it)) },
                    onVerViajes = { nav.irAPestana(Viajes) },
                )
            }
        }
    }
}

@Composable
private fun BarraInferior(esActual: (Pestana) -> Boolean, onClick: (Pestana) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        Pestana.entries.forEach { p ->
            NavigationBarItem(
                selected = esActual(p),
                onClick = { onClick(p) },
                icon = { Icon(p.icono, contentDescription = null) },
                label = { Text(stringResource(p.etiqueta)) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
}

/** Patrón estándar de barra inferior: Mapa es la base, una sola copia de cada pestaña y se conserva su estado. */
private fun NavHostController.irAPestana(ruta: Any) = navigate(ruta) {
    popUpTo(Mapa) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
