package com.mercacerca.barriovivo.domain.model

import com.mercacerca.barriovivo.core.geo.Geo
import com.mercacerca.barriovivo.core.geo.LatLon
import kotlin.math.ceil

/** Un tianguis con su distancia (km, en línea recta) desde el usuario. */
data class TianguisCercano(val tianguis: Tianguis, val distanciaKm: Double) {
    /** Minutos caminando a paso normal (≈ 4.8 km/h). Al menos 1. */
    val minutosCaminando: Int get() = ceil(distanciaKm / Cercania.VELOCIDAD_CAMINANDO_KMH * 60).toInt().coerceAtLeast(1)

    /** Más de una hora y media caminando ya no es "cerca": la UI no muestra minutos. */
    val esCaminable: Boolean get() = minutosCaminando <= 90
}

data class ConteoDia(val dia: Dia, val cantidad: Int)

/** Cuántos tianguis hay a menos de [radioKm] cada día, y cuál conviene más. */
data class MejorDia(val ranking: List<ConteoDia>, val radioKm: Double, val mejor: ConteoDia?)

object Cercania {
    const val RADIO_MEJOR_DIA_KM = 2.0
    const val VELOCIDAD_CAMINANDO_KMH = 4.8

    /** Del más cercano al más lejano (Haversine). Los que no tienen coordenadas se omiten. */
    fun ordenar(origen: LatLon, tianguis: List<Tianguis>): List<TianguisCercano> =
        tianguis.mapNotNull { t -> t.coordenadas?.let { TianguisCercano(t, Geo.distanciaKm(origen, it)) } }
            .sortedBy { it.distanciaKm }

    /**
     * Cuenta, por día, los tianguis dentro del radio. El mejor es el de más tianguis;
     * si empatan, gana el que llega antes contando desde [hoy]. Null si no hay ninguno cerca.
     */
    fun mejorDia(
        origen: LatLon,
        tianguis: List<Tianguis>,
        hoy: Dia,
        radioKm: Double = RADIO_MEJOR_DIA_KM,
    ): MejorDia {
        val cerca = tianguis.filter { t -> t.coordenadas?.let { Geo.distanciaKm(origen, it) <= radioKm } == true }
        val porDia = cerca.groupingBy { it.dia }.eachCount()
        val ranking = Dia.entries.map { ConteoDia(it, porDia[it] ?: 0) }
        val diasDesdeHoy = { d: Dia -> (d.ordinal - hoy.ordinal + 7) % 7 }
        val mejor = ranking.filter { it.cantidad > 0 }
            .sortedWith(compareByDescending<ConteoDia> { it.cantidad }.thenBy { diasDesdeHoy(it.dia) })
            .firstOrNull()
        return MejorDia(ranking, radioKm, mejor)
    }
}
