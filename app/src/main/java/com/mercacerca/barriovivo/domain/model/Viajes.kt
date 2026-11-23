package com.mercacerca.barriovivo.domain.model

import com.mercacerca.barriovivo.core.geo.Geo
import com.mercacerca.barriovivo.core.geo.LatLon
import java.util.Calendar

/** Una ida al tianguis caminando o en bici (en lugar de en coche). */
data class Viaje(
    val id: Int,
    val tianguisId: Int?,
    val tituloTianguis: String,
    val distanciaKm: Double,
    val co2Kg: Double,
    val fecha: Long,
)

data class ResumenViajes(val viajes: List<Viaje>) {
    val visitas: Int get() = viajes.size
    val totalKm: Double get() = viajes.sumOf { it.distanciaKm }
    val totalCo2Kg: Double get() = viajes.sumOf { it.co2Kg }

    companion object {
        val vacio = ResumenViajes(emptyList())
    }
}

sealed interface ResultadoViaje {
    data class Ok(val viaje: Viaje) : ResultadoViaje
    /** Solo cuenta una visita por tianguis al día. */
    data object YaRegistradoHoy : ResultadoViaje
    data object SinCoordenadas : ResultadoViaje
    /** Más de [Co2.MAX_KM_IDA_Y_VUELTA]: no es creíble que haya ido caminando. */
    data class DemasiadoLejos(val km: Double) : ResultadoViaje
}

object Co2 {
    /**
     * kg de CO2e que emite un auto a gasolina promedio por km. Es la constante a ajustar:
     * cámbiala aquí y todo (ficha, ticket, perfil) se recalcula con el nuevo factor.
     */
    const val KG_POR_KM = 0.17

    /** Tope para contar un viaje "caminando o en bici": 10 km de ida, 10 de vuelta. */
    const val MAX_KM_IDA_Y_VUELTA = 20.0

    fun ahorradoKg(km: Double): Double = km * KG_POR_KM

    /** Ida y vuelta en línea recta (Haversine × 2): una estimación conservadora del recorrido. */
    fun distanciaIdaYVueltaKm(origen: LatLon, destino: LatLon): Double = 2 * Geo.distanciaKm(origen, destino)

    /** 00:00 del día de [millis] en la zona horaria del teléfono. */
    fun inicioDelDia(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
