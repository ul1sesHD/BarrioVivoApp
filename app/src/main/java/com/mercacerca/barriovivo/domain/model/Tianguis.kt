package com.mercacerca.barriovivo.domain.model

import com.mercacerca.barriovivo.core.geo.LatLon

data class Tianguis(
    val id: Int,
    val dia: Dia,
    val nombre: String?,
    val ubicacion: String,
    val horaInicio: String?,
    val horaFin: String?,
    /** Null cuando el dataset trae coordenadas malas: no se pinta en el mapa. */
    val coordenadas: LatLon?,
    val nota: String?,
) {
    /** Lo que se muestra como título: el nombre si lo tiene, si no la calle. */
    val titulo: String get() = nombre?.takeIf { it.isNotBlank() } ?: ubicacion
}

data class Testimonio(val id: Int, val autor: String, val texto: String)
