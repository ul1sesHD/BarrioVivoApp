package com.mercacerca.barriovivo.data.seed

import com.mercacerca.barriovivo.data.local.TianguisEntity
import com.mercacerca.barriovivo.domain.model.Dia
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Lee assets/tianguis_cdmx.json: `{ "campos": [...], "datos": [[...], ...] }`.
 * Ubica cada columna por nombre (no por posición) y descarta renglones sin id, día o ubicación.
 * Las coordenadas inválidas se guardan como null: el registro existe, pero no se pinta en el mapa.
 */
object TianguisJsonParser {

    fun parse(json: String): List<TianguisEntity> {
        val raiz = Json.parseToJsonElement(json).jsonObject
        val columna = raiz.getValue("campos").jsonArray
            .mapIndexed { i, campo -> campo.jsonPrimitive.content to i }
            .toMap()

        fun JsonArray.texto(campo: String): String? =
            columna[campo]?.let(::getOrNull)?.jsonPrimitive?.contentOrNull?.trim()?.takeIf { it.isNotEmpty() }

        fun JsonArray.numero(campo: String): Double? =
            columna[campo]?.let(::getOrNull)?.jsonPrimitive?.doubleOrNull

        return raiz.getValue("datos").jsonArray.mapNotNull { elemento ->
            val r = elemento.jsonArray
            val id = columna["id"]?.let(r::getOrNull)?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
            val dia = Dia.from(r.texto("dia")) ?: return@mapNotNull null
            val ubicacion = r.texto("ubicacion") ?: return@mapNotNull null
            val (lat, lon) = coordenadasValidas(r.numero("lat"), r.numero("lon"))
            TianguisEntity(
                id = id,
                dia = dia.name,
                nombre = r.texto("nombre"),
                ubicacion = ubicacion,
                horaIni = r.texto("ini"),
                horaFin = r.texto("fin"),
                lat = lat,
                lon = lon,
                nota = r.texto("nota"),
            )
        }
    }

    /** Ambas o ninguna: si falta una, o están fuera de rango, se descartan las dos. */
    private fun coordenadasValidas(lat: Double?, lon: Double?): Pair<Double?, Double?> =
        if (lat != null && lon != null && lat in -90.0..90.0 && lon in -180.0..180.0 && (lat != 0.0 || lon != 0.0)) {
            lat to lon
        } else {
            null to null
        }
}
