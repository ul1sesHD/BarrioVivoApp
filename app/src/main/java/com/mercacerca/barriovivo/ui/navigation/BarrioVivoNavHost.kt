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
import com.mercacerca.barriovivo.ui.map.MapScreen
import com.mercacerca.barriovivo.ui.placeholder.ProximamenteScreen
import com.mercacerca.barriovivo.ui.welcome.WelcomeScreen
import kotlinx.serialization.Serializable

// Rutas con tipo (type-safe): el compilador avisa si navegas a algo que no existe.
@Serializable data object Bienvenida
@Serializable data object Mapa
@Serializable data object Cercanos
@Serializable data object Viajes
@Serializable data object Perfil

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
    val enBienvenida = destino == null || destino.hasRoute(Bienvenida::class)

    Scaffold(
        bottomBar = {
            if (!enBienvenida) BarraInferior(
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
            composable<Mapa> { MapScreen(contentPadding = padding) }
            composable<Cercanos> { ProximamenteScreen(R.string.nav_cercanos, R.string.proximamente_cercanos, padding) }
            composable<Viajes> {
                ProximamenteScreen(R.string.nav_viajes, R.string.proximamente_viajes, padding, R.drawable.granjero)
            }
            composable<Perfil> {
                ProximamenteScreen(R.string.nav_perfil, R.string.proximamente_perfil, padding, R.drawable.bancos)
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
