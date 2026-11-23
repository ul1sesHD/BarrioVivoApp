package com.mercacerca.barriovivo.domain.repository

import com.mercacerca.barriovivo.domain.model.Dia
import com.mercacerca.barriovivo.domain.model.Review
import com.mercacerca.barriovivo.domain.model.Testimonio
import com.mercacerca.barriovivo.domain.model.Tianguis
import kotlinx.coroutines.flow.Flow

// Contratos del dominio: la UI depende de estas interfaces, no de Room.

interface TianguisRepository {
    /** Tianguis de un día que SÍ tienen coordenadas (los que se pueden pintar). */
    fun observeMapeablesPorDia(dia: Dia): Flow<List<Tianguis>>

    /** Emite null si el id no existe. */
    fun observePorId(id: Int): Flow<Tianguis?>
}

interface ReviewRepository {
    /** Reseñas de un tianguis, las más nuevas primero. */
    fun observePorTianguis(tianguisId: Int): Flow<List<Review>>
}

interface TestimonioRepository {
    fun observeTodos(): Flow<List<Testimonio>>
}
