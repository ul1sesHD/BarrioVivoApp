package com.mercacerca.barriovivo.data.seed

import com.mercacerca.barriovivo.data.local.TestimonioEntity

/** Mismos testimonios que data/04_seed_barriovivo.sql (tabla testimonio). */
object SeedData {
    val testimonios = listOf(
        TestimonioEntity(1, "Marce", "Gracias a MercaCerca encontré el tianguis de mi colonia, ahora compro local cada semana."),
        TestimonioEntity(2, "Don Beto", "Soy marchante y llegó gente nueva que ni sabía que estábamos aquí. ¡Barrio vivo!"),
        TestimonioEntity(3, "Sofía", "Me mudé hace poco y en un toque supe qué día se pone el tianguis cerca de mi casa."),
        TestimonioEntity(4, "Memo", "Me encanta ver cuánto CO2 ahorro caminando al mercado. Enganchadísimo."),
        TestimonioEntity(5, "Kari", "La fruta más fresca y barata de mi zona, y apoyo a los de aquí."),
    )
}
