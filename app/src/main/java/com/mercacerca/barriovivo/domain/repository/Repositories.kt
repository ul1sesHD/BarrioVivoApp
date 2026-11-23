package com.mercacerca.barriovivo.domain.repository

import com.mercacerca.barriovivo.core.geo.LatLon
import com.mercacerca.barriovivo.domain.model.BorradorReview
import com.mercacerca.barriovivo.domain.model.Dia
import com.mercacerca.barriovivo.domain.model.ResultadoReview
import com.mercacerca.barriovivo.domain.model.ResultadoViaje
import com.mercacerca.barriovivo.domain.model.ResumenViajes
import com.mercacerca.barriovivo.domain.model.ReviewPropia
import com.mercacerca.barriovivo.domain.model.ResultadoCuenta
import com.mercacerca.barriovivo.domain.model.Review
import com.mercacerca.barriovivo.domain.model.Testimonio
import com.mercacerca.barriovivo.domain.model.Tianguis
import com.mercacerca.barriovivo.domain.model.Usuario
import kotlinx.coroutines.flow.Flow

// Contratos del dominio: la UI depende de estas interfaces, no de Room.

interface TianguisRepository {
    /** Tianguis de un día que SÍ tienen coordenadas (los que se pueden pintar). */
    fun observeMapeablesPorDia(dia: Dia): Flow<List<Tianguis>>

    /** Todos los tianguis con coordenadas, de todos los días (para "Mejor día"). */
    fun observeMapeables(): Flow<List<Tianguis>>

    /** Emite null si el id no existe. */
    fun observePorId(id: Int): Flow<Tianguis?>
}

interface ReviewRepository {
    /** Reseñas de un tianguis, las más nuevas primero. */
    fun observePorTianguis(tianguisId: Int): Flow<List<Review>>

    /** La evaluación de este usuario para ese tianguis, si ya lo evaluó. */
    fun observeMia(tianguisId: Int, usuarioId: Int): Flow<Review?>

    fun observeDeUsuario(usuarioId: Int): Flow<List<ReviewPropia>>

    /** Valida, limpia y guarda (o edita) la evaluación; también sincroniza "de confianza". */
    suspend fun guardar(usuario: Usuario, tianguisId: Int, borrador: BorradorReview): ResultadoReview
}

interface FavoritoRepository {
    fun observeEsFavorito(usuarioId: Int, tianguisId: Int): Flow<Boolean>
    fun observeFavoritos(usuarioId: Int): Flow<List<Tianguis>>
    suspend fun marcar(usuarioId: Int, tianguisId: Int, deConfianza: Boolean)
}

interface TestimonioRepository {
    fun observeTodos(): Flow<List<Testimonio>>
}

interface AuthRepository {
    /** Usuario con sesión abierta, o null (anónimo). */
    val sesion: Flow<Usuario?>

    suspend fun registrar(usuario: String, password: String, colorArgb: Int): ResultadoCuenta
    suspend fun iniciarSesion(usuario: String, password: String): ResultadoCuenta
    suspend fun cambiarColor(colorArgb: Int)
    suspend fun cerrarSesion()
}

interface ViajeRepository {
    fun observeResumen(usuarioId: Int): Flow<ResumenViajes>

    /** Registra que va caminando/en bici desde [origen]; calcula km ida y vuelta y CO2 ahorrado. */
    suspend fun registrar(usuarioId: Int, tianguis: Tianguis, origen: LatLon): ResultadoViaje
}
