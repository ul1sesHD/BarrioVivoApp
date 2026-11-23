package com.mercacerca.barriovivo

import com.mercacerca.barriovivo.data.seed.SeedData
import com.mercacerca.barriovivo.domain.model.Promedios
import com.mercacerca.barriovivo.domain.model.Review
import org.junit.Assert.assertEquals
import org.junit.Test

class PromediosTest {

    private fun review(v: Int, t: Int, c: Int, conf: Boolean) = Review(0, 1, "Ana", v, t, c, conf, null)

    @Test fun `sin reviews es vacio y no divide entre cero`() {
        val p = Promedios.de(emptyList())
        assertEquals(0, p.total)
        assertEquals(0.0, p.general, 0.0)
        assertEquals(0, p.porcentajeConfianza)
    }

    @Test fun `promedia cada pregunta y el general`() {
        // Tianguis 90 del seed: (3,5,5,conf) y (5,2,4,conf)
        val p = Promedios.de(listOf(review(3, 5, 5, true), review(5, 2, 4, true)))
        assertEquals(4.0, p.variedad, 1e-9)
        assertEquals(3.5, p.tamano, 1e-9)
        assertEquals(4.5, p.calidad, 1e-9)
        assertEquals(4.0, p.general, 1e-9)
        assertEquals(100, p.porcentajeConfianza)
    }

    @Test fun `porcentaje de confianza redondea`() {
        val p = Promedios.de(listOf(review(1, 1, 1, true), review(1, 1, 1, false), review(1, 1, 1, false)))
        assertEquals(1, p.deConfianza)
        assertEquals(33, p.porcentajeConfianza)
    }

    @Test fun `seed trae las 47 reseñas del sql con calificaciones validas`() {
        assertEquals(47, SeedData.reviews.size)
        SeedData.reviews.forEach { r ->
            listOf(r.variedad, r.tamano, r.calidad).forEach { assert(it in 1..5) }
        }
    }
}
