package com.mercacerca.barriovivo.data.repository

import com.mercacerca.barriovivo.core.geo.LatLon
import com.mercacerca.barriovivo.data.local.FavoritoDao
import com.mercacerca.barriovivo.data.local.FavoritoEntity
import com.mercacerca.barriovivo.data.local.ReviewDao
import com.mercacerca.barriovivo.data.local.ReviewEntity
import com.mercacerca.barriovivo.data.local.TestimonioDao
import com.mercacerca.barriovivo.data.local.TianguisDao
import com.mercacerca.barriovivo.data.local.TianguisEntity
import com.mercacerca.barriovivo.data.sync.SolicitudSync
import com.mercacerca.barriovivo.domain.model.BorradorReview
import com.mercacerca.barriovivo.domain.model.Dia
import com.mercacerca.barriovivo.domain.model.ResultadoReview
import com.mercacerca.barriovivo.domain.model.ReviewPropia
import com.mercacerca.barriovivo.domain.model.Usuario
import com.mercacerca.barriovivo.domain.model.ValidadorCuenta
import com.mercacerca.barriovivo.domain.model.ValidadorReview
import com.mercacerca.barriovivo.domain.model.Review
import com.mercacerca.barriovivo.domain.model.Testimonio
import com.mercacerca.barriovivo.domain.model.Tianguis
import com.mercacerca.barriovivo.domain.repository.FavoritoRepository
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

    override fun observeMapeables(): Flow<List<Tianguis>> =
        dao.observeConCoordenadas().map { lista -> lista.mapNotNull { it.toDomain() } }

    override fun observePorId(id: Int): Flow<Tianguis?> = dao.observePorId(id).map { it?.toDomain() }
}

class ReviewRepositoryImpl @Inject constructor(
    private val dao: ReviewDao,
    private val favoritos: FavoritoDao,
    private val sync: SolicitudSync = SolicitudSync {},
) : ReviewRepository {
    override fun observePorTianguis(tianguisId: Int): Flow<List<Review>> =
        dao.observePorTianguis(tianguisId).map { lista ->
            lista.map { it.review.toDomain().copy(colorArgb = it.colorIcono?.let(ValidadorCuenta::hexAColor)) }
        }

    override fun observeMia(tianguisId: Int, usuarioId: Int): Flow<Review?> =
        dao.observeMia(tianguisId, usuarioId).map { it?.toDomain() }

    override fun observeDeUsuario(usuarioId: Int): Flow<List<ReviewPropia>> =
        dao.observeDeUsuario(usuarioId).map { lista ->
            lista.map { ReviewPropia(it.review.toDomain(), it.nombre?.takeIf(String::isNotBlank) ?: it.ubicacion) }
        }

    override suspend fun guardar(usuario: Usuario, tianguisId: Int, borrador: BorradorReview): ResultadoReview {
        val comentario = when (val v = ValidadorReview.validar(borrador)) {
            is ValidadorReview.Resultado.Invalida -> return ResultadoReview.Error(v.motivo)
            is ValidadorReview.Resultado.Valida -> v.comentarioLimpio
        }
        val existente = dao.mia(tianguisId, usuario.id)
        dao.guardar(
            ReviewEntity(
                id = existente?.id ?: 0, // mismo id = se edita, no se duplica
                tianguisId = tianguisId,
                autor = usuario.usuario,
                variedad = borrador.variedad,
                tamano = borrador.tamano,
                calidad = borrador.calidad,
                confianza = borrador.confianza,
                comentario = comentario,
                usuarioId = usuario.id,
            )
        )
        // El switch "de confianza" y la lista "Mis tianguis de confianza" son lo mismo.
        if (borrador.confianza) favoritos.agregar(FavoritoEntity(usuario.id, tianguisId))
        else favoritos.quitar(usuario.id, tianguisId)
        sync.solicitar()
        return ResultadoReview.Ok
    }
}

class FavoritoRepositoryImpl @Inject constructor(
    private val dao: FavoritoDao,
    private val reviews: ReviewDao,
    private val sync: SolicitudSync = SolicitudSync {},
) : FavoritoRepository {
    override fun observeEsFavorito(usuarioId: Int, tianguisId: Int): Flow<Boolean> = dao.observeEs(usuarioId, tianguisId)

    override fun observeFavoritos(usuarioId: Int): Flow<List<Tianguis>> =
        dao.observeDeUsuario(usuarioId).map { lista -> lista.mapNotNull { it.toDomain() } }

    override suspend fun marcar(usuarioId: Int, tianguisId: Int, deConfianza: Boolean) {
        if (deConfianza) dao.agregar(FavoritoEntity(usuarioId, tianguisId)) else dao.quitar(usuarioId, tianguisId)
        reviews.actualizarConfianza(tianguisId, usuarioId, deConfianza) // si ya lo evaluó, su voto cambia igual
        sync.solicitar()
    }
}

private fun ReviewEntity.toDomain() =
    Review(id, tianguisId, autor, variedad, tamano, calidad, confianza, comentario, usuarioId)

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
