package com.mercacerca.barriovivo.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Todo lo que la app le pide a la nube. Hay dos implementaciones:
 * [SupabaseRemota] (si hay llaves en local.properties) y [NubeApagada] (si no).
 * El resto de la app no sabe cuál está usando: sigue funcionando offline igual.
 */
interface NubeRemota {
    val activa: Boolean

    /** Crea la cuenta en Supabase Auth y abre sesión. Devuelve el uid. */
    suspend fun registrar(usuario: String, password: String, colorHex: String): ResultadoNube<String>
    suspend fun iniciarSesion(usuario: String, password: String): ResultadoNube<PerfilDto>
    suspend fun cerrarSesion()
    /** Espera a que se restaure la sesión guardada y devuelve el uid (o null). */
    suspend fun uidActual(): String?

    suspend fun cambiarColor(colorHex: String): ResultadoNube<Unit>

    suspend fun subirReviews(reviews: List<ReviewDto>): ResultadoNube<Unit>
    suspend fun bajarReviews(): ResultadoNube<List<ReviewPublicaDto>>

    /** Deja en la nube exactamente estos favoritos (agrega los nuevos y borra los quitados). */
    suspend fun reemplazarFavoritos(tianguisIds: Set<Int>): ResultadoNube<Unit>
    suspend fun bajarFavoritos(): ResultadoNube<Set<Int>>

    suspend fun subirViajes(viajes: List<ViajeDto>): ResultadoNube<Unit>
    suspend fun bajarViajes(): ResultadoNube<List<ViajeDto>>

    companion object {
        /**
         * Supabase Auth pide correo; la app solo usa usuario. Se arma un correo interno que el
         * usuario nunca ve. ".invalid" es un dominio reservado (RFC 2606): nadie puede tenerlo.
         * Si tu proyecto lo rechazara, cambia SOLO esta línea por un dominio tuyo.
         */
        fun correoInterno(usuario: String) = "$usuario@users.barriovivo.invalid"
    }
}

sealed interface ResultadoNube<out T> {
    data class Ok<T>(val valor: T) : ResultadoNube<T>
    data object CredencialesInvalidas : ResultadoNube<Nothing>
    data object UsuarioOcupado : ResultadoNube<Nothing>
    /** Sin internet o servidor caído: la app sigue offline. */
    data object SinConexion : ResultadoNube<Nothing>
    data object SinSesion : ResultadoNube<Nothing>
    data class Error(val detalle: String?) : ResultadoNube<Nothing>
}

/** Implementación para cuando no hay llaves: todo es no-op y la app queda 100% offline. */
object NubeApagada : NubeRemota {
    override val activa = false
    override suspend fun registrar(usuario: String, password: String, colorHex: String) = ResultadoNube.SinConexion
    override suspend fun iniciarSesion(usuario: String, password: String) = ResultadoNube.SinConexion
    override suspend fun cerrarSesion() = Unit
    override suspend fun uidActual(): String? = null
    override suspend fun cambiarColor(colorHex: String) = ResultadoNube.SinConexion
    override suspend fun subirReviews(reviews: List<ReviewDto>) = ResultadoNube.SinConexion
    override suspend fun bajarReviews() = ResultadoNube.SinConexion
    override suspend fun reemplazarFavoritos(tianguisIds: Set<Int>) = ResultadoNube.SinConexion
    override suspend fun bajarFavoritos() = ResultadoNube.SinConexion
    override suspend fun subirViajes(viajes: List<ViajeDto>) = ResultadoNube.SinConexion
    override suspend fun bajarViajes() = ResultadoNube.SinConexion
}

// ---- DTOs: mismos nombres de columna que data/supabase/001_barrio_vivo.sql ----

@Serializable
data class PerfilDto(
    val id: String,
    val usuario: String,
    @SerialName("color_icono") val colorIcono: String,
)

@Serializable
data class ReviewDto(
    @SerialName("user_id") val userId: String,
    @SerialName("tianguis_id") val tianguisId: Int,
    val variedad: Int,
    val tamano: Int,
    val calidad: Int,
    val confianza: Boolean,
    val comentario: String?,
)

@Serializable
data class ReviewPublicaDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("tianguis_id") val tianguisId: Int,
    val variedad: Int,
    val tamano: Int,
    val calidad: Int,
    val confianza: Boolean,
    val comentario: String? = null,
    @SerialName("actualizado_en") val actualizadoEn: String,
    val autor: String,
    @SerialName("color_icono") val colorIcono: String,
)

@Serializable
data class FavoritoDto(
    @SerialName("user_id") val userId: String,
    @SerialName("tianguis_id") val tianguisId: Int,
)

@Serializable
data class ViajeDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("tianguis_id") val tianguisId: Int?,
    @SerialName("tianguis_titulo") val tituloTianguis: String,
    @SerialName("distancia_km") val distanciaKm: Double,
    @SerialName("co2_kg") val co2Kg: Double,
    /** ISO-8601, p. ej. 2026-11-23T21:44:00Z */
    val fecha: String,
)
