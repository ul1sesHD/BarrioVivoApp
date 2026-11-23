package com.mercacerca.barriovivo.domain.model

data class Review(
    val id: Int,
    val tianguisId: Int,
    val autor: String,
    val variedad: Int,
    val tamano: Int,
    val calidad: Int,
    val confianza: Boolean,
    val comentario: String?,
) {
    /** Calificación de esta reseña: promedio de sus 3 preguntas. */
    val promedio: Double get() = (variedad + tamano + calidad) / 3.0
}

/** Resumen de las reseñas de un tianguis. Con 0 reseñas, [vacio]. */
data class Promedios(
    val variedad: Double,
    val tamano: Double,
    val calidad: Double,
    val total: Int,
    /** Cuántas reseñas lo marcaron "de confianza" (0..total). */
    val deConfianza: Int,
) {
    val general: Double get() = if (total == 0) 0.0 else (variedad + tamano + calidad) / 3.0
    val porcentajeConfianza: Int get() = if (total == 0) 0 else Math.round(deConfianza * 100.0 / total).toInt()

    companion object {
        val vacio = Promedios(0.0, 0.0, 0.0, 0, 0)

        fun de(reviews: List<Review>): Promedios {
            if (reviews.isEmpty()) return vacio
            return Promedios(
                variedad = reviews.map { it.variedad }.average(),
                tamano = reviews.map { it.tamano }.average(),
                calidad = reviews.map { it.calidad }.average(),
                total = reviews.size,
                deConfianza = reviews.count { it.confianza },
            )
        }
    }
}
