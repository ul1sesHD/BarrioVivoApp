package com.mercacerca.barriovivo

import com.mercacerca.barriovivo.core.geo.Geo
import com.mercacerca.barriovivo.core.geo.LatLon
import com.mercacerca.barriovivo.domain.model.Dia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class GeoYDiaTest {

    @Test fun `distancia a si mismo es cero`() {
        assertEquals(0.0, Geo.distanciaKm(Geo.ZOCALO, Geo.ZOCALO), 1e-9)
    }

    @Test fun `zocalo a bellas artes es cerca de 0_9 km`() {
        val bellasArtes = LatLon(19.435244, -99.141155)
        assertEquals(0.88, Geo.distanciaKm(Geo.ZOCALO, bellasArtes), 0.05)
    }

    @Test fun `distancia es simetrica`() {
        val a = LatLon(19.30, -99.20)
        val b = LatLon(19.50, -99.05)
        assertEquals(Geo.distanciaKm(a, b), Geo.distanciaKm(b, a), 1e-9)
    }

    @Test fun `dia acepta acentos, minusculas y espacios`() {
        assertEquals(Dia.MIERCOLES, Dia.from("MIÉRCOLES"))
        assertEquals(Dia.SABADO, Dia.from(" sábado "))
        assertEquals(Dia.LUNES, Dia.from("Lunes"))
        assertNull(Dia.from("FERIADO"))
        assertNull(Dia.from(null))
    }

    @Test fun `hoy mapea domingo y lunes correctamente`() {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        assertEquals(Dia.DOMINGO, Dia.hoy(cal))
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        assertEquals(Dia.LUNES, Dia.hoy(cal))
    }
}
