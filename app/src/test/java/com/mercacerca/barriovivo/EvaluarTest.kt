package com.mercacerca.barriovivo

import com.mercacerca.barriovivo.data.local.FavoritoDao
import com.mercacerca.barriovivo.data.local.FavoritoEntity
import com.mercacerca.barriovivo.data.local.ReviewConColor
import com.mercacerca.barriovivo.data.local.ReviewConTianguis
import com.mercacerca.barriovivo.data.local.ReviewDao
import com.mercacerca.barriovivo.data.local.ReviewEntity
import com.mercacerca.barriovivo.data.local.TianguisEntity
import com.mercacerca.barriovivo.data.repository.FavoritoRepositoryImpl
import com.mercacerca.barriovivo.data.repository.ReviewRepositoryImpl
import com.mercacerca.barriovivo.domain.model.BorradorReview
import com.mercacerca.barriovivo.domain.model.ErrorReview
import com.mercacerca.barriovivo.domain.model.ResultadoReview
import com.mercacerca.barriovivo.domain.model.Usuario
import com.mercacerca.barriovivo.domain.model.ValidadorReview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EvaluarTest {

    class FakeReviewDao : ReviewDao {
        val filas = mutableListOf<ReviewEntity>()
        override fun observePorTianguis(tianguisId: Int): Flow<List<ReviewConColor>> =
            flowOf(filas.filter { it.tianguisId == tianguisId }.map { ReviewConColor(it, null) })
        override fun observeMia(tianguisId: Int, usuarioId: Int): Flow<ReviewEntity?> = flowOf(filas.firstOrNull { it.tianguisId == tianguisId && it.usuarioId == usuarioId })
        override suspend fun mia(tianguisId: Int, usuarioId: Int) = filas.firstOrNull { it.tianguisId == tianguisId && it.usuarioId == usuarioId }
        override fun observeDeUsuario(usuarioId: Int): Flow<List<ReviewConTianguis>> = emptyFlow()
        override suspend fun guardar(review: ReviewEntity): Long {
            val nueva = if (review.id == 0) review.copy(id = (filas.maxOfOrNull { it.id } ?: 0) + 1) else review
            filas.removeAll { it.id == nueva.id }
            filas += nueva
            return nueva.id.toLong()
        }
        override suspend fun actualizarConfianza(tianguisId: Int, usuarioId: Int, confianza: Boolean) {
            filas.replaceAll { if (it.tianguisId == tianguisId && it.usuarioId == usuarioId) it.copy(confianza = confianza) else it }
        }
        override suspend fun count() = filas.size
        override suspend fun insertAll(items: List<ReviewEntity>) { filas += items }
        override suspend fun deUsuario(usuarioId: Int) = filas.filter { it.usuarioId == usuarioId }
        override suspend fun porRemoteId(remoteId: String) = filas.firstOrNull { it.remoteId == remoteId }
    }

    class FakeFavoritoDao : FavoritoDao {
        val favs = mutableSetOf<Pair<Int, Int>>()
        override fun observeEs(usuarioId: Int, tianguisId: Int) = flowOf((usuarioId to tianguisId) in favs)
        override fun observeDeUsuario(usuarioId: Int): Flow<List<TianguisEntity>> = emptyFlow()
        override suspend fun agregar(favorito: FavoritoEntity) { favs += favorito.usuarioId to favorito.tianguisId }
        override suspend fun quitar(usuarioId: Int, tianguisId: Int) { favs -= usuarioId to tianguisId }
        override suspend fun idsDeUsuario(usuarioId: Int) = favs.filter { it.first == usuarioId }.map { it.second }
    }

    private val marce = Usuario(id = 1, usuario = "marce", colorArgb = 0, creadoEn = 0)

    @Test fun `validador exige las 3 preguntas`() {
        assertEquals(ErrorReview.FALTA_VARIEDAD, (ValidadorReview.validar(BorradorReview(0, 3, 3)) as ValidadorReview.Resultado.Invalida).motivo)
        assertEquals(ErrorReview.FALTA_TAMANO, (ValidadorReview.validar(BorradorReview(3, 0, 3)) as ValidadorReview.Resultado.Invalida).motivo)
        assertEquals(ErrorReview.FALTA_CALIDAD, (ValidadorReview.validar(BorradorReview(3, 3, 6)) as ValidadorReview.Resultado.Invalida).motivo)
        assertTrue(ValidadorReview.validar(BorradorReview(1, 5, 3)) is ValidadorReview.Resultado.Valida)
    }

    @Test fun `limpia el comentario`() {
        assertEquals("Muy bueno, repito", ValidadorReview.limpiarComentario("  Muy   bueno,\trepito \u0007 "))
        assertEquals("línea 1\n\nlínea 2", ValidadorReview.limpiarComentario("línea 1\n\n\n\n  línea 2"))
        assertNull(ValidadorReview.limpiarComentario("   \n  "))
        val largo = ValidadorReview.validar(BorradorReview(3, 3, 3, comentario = "a".repeat(281)))
        assertEquals(ErrorReview.COMENTARIO_LARGO, (largo as ValidadorReview.Resultado.Invalida).motivo)
    }

    @Test fun `guardar dos veces edita la misma evaluacion y sincroniza confianza`() = runTest {
        val reviews = FakeReviewDao()
        val favs = FakeFavoritoDao()
        val repo = ReviewRepositoryImpl(reviews, favs)

        assertEquals(ResultadoReview.Ok, repo.guardar(marce, 90, BorradorReview(3, 4, 5, confianza = true, comentario = " ¡Bien! ")))
        assertEquals(ResultadoReview.Ok, repo.guardar(marce, 90, BorradorReview(5, 5, 5, confianza = false)))

        val mia = reviews.filas.single()
        assertEquals(5, mia.variedad)
        assertEquals("marce", mia.autor)
        assertEquals(1, mia.usuarioId)
        assertNull(mia.comentario)
        assertFalse((1 to 90) in favs.favs)
    }

    @Test fun `evaluacion invalida no se guarda`() = runTest {
        val reviews = FakeReviewDao()
        val r = ReviewRepositoryImpl(reviews, FakeFavoritoDao()).guardar(marce, 90, BorradorReview(0, 4, 5))
        assertEquals(ResultadoReview.Error(ErrorReview.FALTA_VARIEDAD), r)
        assertTrue(reviews.filas.isEmpty())
    }

    @Test fun `marcar de confianza actualiza tambien mi voto`() = runTest {
        val reviews = FakeReviewDao()
        val favs = FakeFavoritoDao()
        ReviewRepositoryImpl(reviews, favs).guardar(marce, 90, BorradorReview(3, 3, 3, confianza = false))
        FavoritoRepositoryImpl(favs, reviews).marcar(1, 90, deConfianza = true)
        assertTrue((1 to 90) in favs.favs)
        assertTrue(reviews.filas.single().confianza)
    }
}
