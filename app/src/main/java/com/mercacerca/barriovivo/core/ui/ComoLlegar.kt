package com.mercacerca.barriovivo.core.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.mercacerca.barriovivo.core.geo.LatLon
import java.util.Locale

/**
 * Abre la app de mapas del teléfono (Google Maps, Waze, OsmAnd…) apuntando al destino.
 * Si no hay ninguna, abre la ruta en el navegador (OpenStreetMap). Devuelve false si nada la pudo abrir.
 */
fun Context.abrirComoLlegar(destino: LatLon, etiqueta: String): Boolean {
    // Locale.US: el decimal siempre con punto (en es-MX algunos teléfonos usan coma).
    val coords = String.format(Locale.US, "%.6f,%.6f", destino.lat, destino.lon)
    val geo = Uri.parse("geo:$coords?q=$coords(${Uri.encode(etiqueta)})")
    val web = Uri.parse("https://www.openstreetmap.org/directions?route=%3B${coords.replace(",", "%2C")}")
    return listOf(geo, web).any { uri ->
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }
}
