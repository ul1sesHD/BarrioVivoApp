package com.mercacerca.barriovivo

import com.mercacerca.barriovivo.core.geo.Geo
import com.mercacerca.barriovivo.core.geo.LatLon
import com.mercacerca.barriovivo.domain.model.Cercania
import com.mercacerca.barriovivo.domain.model.Dia
import com.mercacerca.barriovivo.domain.model.Tianguis
import com.mercacerca.barriovivo.domain.model.TianguisCercano
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CercaniaTest {

    private val origen = Geo.ZOCALO

    /** Tianguis a ~[km] al norte del Zócalo (1° de latitud ≈ 111.2 km). */
    private fun t(id: Int, dia: Dia, km: Double?) = Tianguis(
        id, dia, null, "Calle $id", "08:00", "17:00",
        km?.let { LatLon(origen.lat + it / 111.2, origen.lon) }, null,
    )

    @Test fun `ordena del mas cercano al mas lejano y omite sin coordenadas`() {
        val r = Cercania.ordenar(origen, listOf(t(1, Dia.LUNES, 3.0), t(2, Dia.LUNES, 0.5), t(3, Dia.LUNES, null)))
        assertEquals(listOf(2, 1), r.map { it.tianguis.id })
        assertEquals(0.5, r.first().distanciaKm, 0.01)
    }

    @Test fun `mejor dia cuenta solo dentro del radio`() {
        val lista = listOf(
            t(1, Dia.SABADO, 0.5), t(2, Dia.SABADO, 1.9), t(3, Dia.SABADO, 5.0),
            t(4, Dia.MARTES, 1.0),
        )
        val m = Cercania.mejorDia(origen, lista, hoy = Dia.LUNES)
        assertEquals(Dia.SABADO, m.mejor?.dia)
        assertEquals(2, m.mejor?.cantidad)
        assertEquals(7, m.ranking.size)
        assertEquals(1, m.ranking.first { it.dia == Dia.MARTES }.cantidad)
    }

    @Test fun `empate gana el dia mas proximo desde hoy`() {
        val lista = listOf(t(1, Dia.LUNES, 1.0), t(2, Dia.JUEVES, 1.0))
        assertEquals(Dia.JUEVES, Cercania.mejorDia(origen, lista, hoy = Dia.MIERCOLES).mejor?.dia)
        assertEquals(Dia.LUNES, Cercania.mejorDia(origen, lista, hoy = Dia.LUNES).mejor?.dia)
    }

    @Test fun `sin tianguis cerca no hay mejor dia`() {
        assertNull(Cercania.mejorDia(origen, listOf(t(1, Dia.LUNES, 10.0)), hoy = Dia.LUNES).mejor)
    }

    @Test fun `minutos caminando y formato de distancia`() {
        assertEquals(15, TianguisCercano(t(1, Dia.LUNES, 1.2), 1.2).minutosCaminando)
        assertEquals(1, TianguisCercano(t(1, Dia.LUNES, 0.0), 0.0).minutosCaminando)
        assertEquals("850 m", Geo.formatearDistancia(0.853))
        assertEquals("10 m", Geo.formatearDistancia(0.001))
        assertEquals("1.2 km", Geo.formatearDistancia(1.234))
        assertEquals("2979 km", Geo.formatearDistancia(2979.3))
        assertEquals(false, TianguisCercano(t(1, Dia.LUNES, 8.0), 8.0).esCaminable)
    }
}
