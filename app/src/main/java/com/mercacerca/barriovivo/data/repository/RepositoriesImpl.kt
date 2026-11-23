package com.mercacerca.barriovivo.data.repository

import com.mercacerca.barriovivo.core.geo.LatLon
import com.mercacerca.barriovivo.data.local.ReviewDao
import com.mercacerca.barriovivo.data.local.TestimonioDao
import com.mercacerca.barriovivo.data.local.TianguisDao
import com.mercacerca.barriovivo.data.local.TianguisEntity
import com.mercacerca.barriovivo.domain.model.Dia
import com.mercacerca.barriovivo.domain.model.Review
import com.mercacerca.barriovivo.domain.model.Testimonio
import com.mercacerca.barriovivo.domain.model.Tianguis
import com.mercacerca.barriovivo.domain.repository.ReviewRepository
import com.mercacerca.barriovivo.domain.repository.TestimonioRepository
import com.mercacerca.barriovivo.domain.repository.TianguisRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TianguisRepositoryImpl @Inject constructor(
    private val dao: TianguisDao,
) : TianguisRepository {
    override fun observeMapeablesPorDia(dia: Dia): Flow<List<Tianguis>> =
        dao.observeConCoordenadasPorDia(dia.name).map { lista -> lista.mapNotNull { it.toDomain() } }

    override fun observePorId(id: Int): Flow<Tianguis?> = dao.observePorId(id).map { it?.toDomain() }
}

class ReviewRepositoryImpl @Inject constructor(
    private val dao: ReviewDao,
) : ReviewRepository {
    override fun observePorTianguis(tianguisId: Int): Flow<List<Review>> =
        dao.observePorTianguis(tianguisId).map { lista ->
            lista.map { Review(it.id, it.tianguisId, it.autor, it.variedad, it.tamano, it.calidad, it.confianza, it.comentario) }
        }
}

class TestimonioRepositoryImpl @Inject constructor(
    private val dao: TestimonioDao,
) : TestimonioRepository {
    override fun observeTodos(): Flow<List<Testimonio>> =
        dao.observeTodos().map { lista -> lista.map { Testimonio(it.id, it.autor, it.texto) } }
}

/** Entity (Room) → modelo de dominio. Null si el día guardado no es válido. */
internal fun TianguisEntity.toDomain(): Tianguis? {
    val dia = Dia.from(dia) ?: return null
    val punto = if (lat != null && lon != null) LatLon(lat, lon).takeIf { it.esValida } else null
    return Tianguis(id, dia, nombre, ubicacion, horaIni, horaFin, punto, nota)
}
