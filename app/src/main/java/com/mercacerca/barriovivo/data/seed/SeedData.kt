package com.mercacerca.barriovivo.data.seed

import com.mercacerca.barriovivo.data.local.ReviewEntity
import com.mercacerca.barriovivo.data.local.TestimonioEntity

/** Datos de ejemplo: los mismos de data/04_seed_barriovivo.sql. */
object SeedData {
    val testimonios = listOf(
        TestimonioEntity(1, "Marce", "Gracias a MercaCerca encontré el tianguis de mi colonia, ahora compro local cada semana."),
        TestimonioEntity(2, "Don Beto", "Soy marchante y llegó gente nueva que ni sabía que estábamos aquí. ¡Barrio vivo!"),
        TestimonioEntity(3, "Sofía", "Me mudé hace poco y en un toque supe qué día se pone el tianguis cerca de mi casa."),
        TestimonioEntity(4, "Memo", "Me encanta ver cuánto CO2 ahorro caminando al mercado. Enganchadísimo."),
        TestimonioEntity(5, "Kari", "La fruta más fresca y barata de mi zona, y apoyo a los de aquí."),
    )

    // Fecha fija para que las reseñas demo queden ordenadas por id y debajo de las reales.
    private const val FECHA_SEED = 1_763_942_400_000L // 2025-11-24

    private fun r(id: Int, t: Int, autor: String, v: Int, ta: Int, c: Int, conf: Boolean, com: String) =
        ReviewEntity(id, t, autor, v, ta, c, conf, com, FECHA_SEED + id)

    val reviews = listOf(
        r(1, 90, "Luis", 3, 5, 5, true, "Muy surtido de fruta y verdura, llegas temprano y está fresquísimo."),
        r(2, 90, "Ana", 5, 2, 4, true, "Chico pero bien surtido, atención de diez."),
        r(3, 94, "Kari", 3, 2, 3, false, "Pasillo amplio, fácil de recorrer con el carrito."),
        r(4, 94, "Kari", 3, 3, 3, false, "Calidad excelente en verduras, repito cada semana."),
        r(5, 94, "Ana", 3, 2, 3, false, "Chico pero bien surtido, atención de diez."),
        r(6, 107, "Ana", 5, 5, 3, true, "Precios justos y producto de temporada muy bueno."),
        r(7, 107, "Marce", 5, 3, 4, true, "Pasillo amplio, fácil de recorrer con el carrito."),
        r(8, 107, "Pau", 5, 2, 5, true, "Un poco lleno a mediodía pero vale la pena."),
        r(9, 111, "Diego", 3, 2, 5, true, "Chico pero bien surtido, atención de diez."),
        r(10, 111, "Rodo", 4, 2, 5, true, "Buen ambiente, los marchantes te dan buen trato."),
        r(11, 111, "Kari", 3, 3, 4, false, "Calidad excelente en verduras, repito cada semana."),
        r(12, 139, "Diego", 4, 5, 4, true, "Un poco lleno a mediodía pero vale la pena."),
        r(13, 139, "Sofía", 3, 3, 3, false, "Chico pero bien surtido, atención de diez."),
        r(14, 154, "Sofía", 4, 4, 5, true, "Encontré cosas que no hay en el súper, muy local."),
        r(15, 154, "Kari", 5, 2, 3, true, "Calidad excelente en verduras, repito cada semana."),
        r(16, 154, "Kari", 3, 4, 3, false, "Encontré cosas que no hay en el súper, muy local."),
        r(17, 160, "Beto", 5, 2, 5, true, "Chico pero bien surtido, atención de diez."),
        r(18, 175, "Rodo", 5, 4, 5, true, "Encontré cosas que no hay en el súper, muy local."),
        r(19, 175, "Memo", 4, 2, 3, false, "Un poco lleno a mediodía pero vale la pena."),
        r(20, 231, "Rodo", 5, 2, 3, true, "Un poco lleno a mediodía pero vale la pena."),
        r(21, 231, "Beto", 5, 5, 4, true, "Pasillo amplio, fácil de recorrer con el carrito."),
        r(22, 231, "Rodo", 3, 5, 4, false, "Variedad enorme, encuentras de todo en un solo lugar."),
        r(23, 233, "Marce", 4, 2, 3, false, "Un poco lleno a mediodía pero vale la pena."),
        r(24, 328, "Luis", 3, 5, 4, false, "Encontré cosas que no hay en el súper, muy local."),
        r(25, 328, "Sofía", 3, 5, 4, false, "Calidad excelente en verduras, repito cada semana."),
        r(26, 328, "Kari", 3, 5, 5, true, "Un poco lleno a mediodía pero vale la pena."),
        r(27, 359, "Luis", 5, 5, 3, true, "Variedad enorme, encuentras de todo en un solo lugar."),
        r(28, 359, "Ana", 3, 3, 3, false, "Precios justos y producto de temporada muy bueno."),
        r(29, 360, "Ana", 5, 3, 4, true, "Un poco lleno a mediodía pero vale la pena."),
        r(30, 360, "Rodo", 3, 5, 5, true, "La comida preparada está deliciosa, recomendado."),
        r(31, 430, "Rodo", 4, 3, 5, true, "Calidad excelente en verduras, repito cada semana."),
        r(32, 430, "Kari", 5, 2, 4, true, "Calidad excelente en verduras, repito cada semana."),
        r(33, 430, "Memo", 4, 5, 4, true, "Buen ambiente, los marchantes te dan buen trato."),
        r(34, 431, "Diego", 4, 2, 3, false, "Buen ambiente, los marchantes te dan buen trato."),
        r(35, 431, "Rodo", 4, 3, 3, false, "La comida preparada está deliciosa, recomendado."),
        r(36, 431, "Marce", 3, 2, 3, false, "Chico pero bien surtido, atención de diez."),
        r(37, 449, "Luis", 3, 4, 5, true, "Muy surtido de fruta y verdura, llegas temprano y está fresquísimo."),
        r(38, 449, "Beto", 3, 5, 3, false, "Un poco lleno a mediodía pero vale la pena."),
        r(39, 449, "Luis", 5, 4, 4, true, "Buen ambiente, los marchantes te dan buen trato."),
        r(40, 486, "Luis", 4, 5, 4, true, "Un poco lleno a mediodía pero vale la pena."),
        r(41, 486, "Sofía", 3, 2, 5, true, "La comida preparada está deliciosa, recomendado."),
        r(42, 490, "Diego", 5, 3, 5, true, "Muy surtido de fruta y verdura, llegas temprano y está fresquísimo."),
        r(43, 490, "Ana", 5, 4, 3, true, "Calidad excelente en verduras, repito cada semana."),
        r(44, 505, "Pau", 4, 2, 5, true, "Un poco lleno a mediodía pero vale la pena."),
        r(45, 505, "Pau", 4, 3, 4, true, "Precios justos y producto de temporada muy bueno."),
        r(46, 505, "Rodo", 5, 4, 5, true, "Precios justos y producto de temporada muy bueno."),
        r(47, 521, "Diego", 3, 5, 5, true, "Precios justos y producto de temporada muy bueno."),
    )
}
