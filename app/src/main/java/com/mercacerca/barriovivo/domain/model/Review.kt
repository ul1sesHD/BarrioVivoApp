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
    /** Null en las reseñas de ejemplo. */
    val usuarioId: Int? = null,
    /** Color del ícono del autor; null = se calcula por nombre. */
    val colorArgb: Int? = null,
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

/** Reseña propia con el nombre del tianguis (para el perfil). */
data class ReviewPropia(val review: Review, val tituloTianguis: String)

/** Lo que el usuario llenó en "Evaluar". 0 estrellas = sin contestar. */
data class BorradorReview(
    val variedad: Int = 0,
    val tamano: Int = 0,
    val calidad: Int = 0,
    val confianza: Boolean = false,
    val comentario: String = "",
)

enum class ErrorReview { FALTA_VARIEDAD, FALTA_TAMANO, FALTA_CALIDAD, COMENTARIO_LARGO }

sealed interface ResultadoReview {
    data object Ok : ResultadoReview
    data class Error(val motivo: ErrorReview) : ResultadoReview
}

/** Reglas y limpieza de una evaluación (capa domain). */
object ValidadorReview {
    const val COMENTARIO_MAX = 280

    sealed interface Resultado {
        data class Valida(val comentarioLimpio: String?) : Resultado
        data class Invalida(val motivo: ErrorReview) : Resultado
    }

    fun validar(b: BorradorReview): Resultado {
        if (b.variedad !in 1..5) return Resultado.Invalida(ErrorReview.FALTA_VARIEDAD)
        if (b.tamano !in 1..5) return Resultado.Invalida(ErrorReview.FALTA_TAMANO)
        if (b.calidad !in 1..5) return Resultado.Invalida(ErrorReview.FALTA_CALIDAD)
        val limpio = limpiarComentario(b.comentario)
        if (limpio != null && limpio.length > COMENTARIO_MAX) return Resultado.Invalida(ErrorReview.COMENTARIO_LARGO)
        return Resultado.Valida(limpio)
    }

    /** Quita caracteres de control (salvo saltos de línea), espacios repetidos y bordes. Vacío → null. */
    fun limpiarComentario(texto: String): String? =
        texto.replace(Regex("[ \\t]+"), " ")
            .replace(Regex("[\\p{Cc}&&[^\\n]]"), "")
            .replace(Regex("\\n{3,}"), "\n\n")
            .lines().joinToString("\n") { it.trim() }
            .trim()
            .ifEmpty { null }
}
