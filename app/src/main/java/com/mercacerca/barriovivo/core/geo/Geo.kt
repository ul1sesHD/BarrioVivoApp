package com.mercacerca.barriovivo.core.geo

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class LatLon(val lat: Double, val lon: Double) {
    val esValida: Boolean get() = lat in -90.0..90.0 && lon in -180.0..180.0
}

object Geo {
    /** Fallback cuando no hay permiso de ubicación: el Zócalo de la CDMX. */
    val ZOCALO = LatLon(19.432608, -99.133209)

    private const val RADIO_TIERRA_KM = 6371.0088

    /** Distancia en línea recta (km) entre dos puntos, fórmula de Haversine. */
    fun distanciaKm(a: LatLon, b: LatLon): Double {
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLon = Math.toRadians(b.lon - a.lon)
        val h = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLon / 2).pow(2)
        return 2 * RADIO_TIERRA_KM * asin(sqrt(h.coerceIn(0.0, 1.0)))
    }
}
