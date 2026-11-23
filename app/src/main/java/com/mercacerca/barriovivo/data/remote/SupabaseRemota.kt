package com.mercacerca.barriovivo.data.remote

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthErrorCode
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.IOException

/** [NubeRemota] sobre Supabase: Auth (correo interno + contraseña) y PostgREST (tablas con RLS). */
class SupabaseRemota(private val client: SupabaseClient) : NubeRemota {

    override val activa = true

    override suspend fun uidActual(): String? {
        client.auth.awaitInitialization()
        return client.auth.currentUserOrNull()?.id
    }

    override suspend fun registrar(usuario: String, password: String, colorHex: String) = intentar {
        client.auth.signUpWith(Email) {
            email = NubeRemota.correoInterno(usuario)
            this.password = password
            // El trigger crear_perfil() lee estos datos para crear la fila en public.perfil.
            data = buildJsonObject {
                put("usuario", usuario)
                put("color", colorHex)
            }
        }
        uidActual() ?: throw IllegalStateException("Sin sesión tras el registro: desactiva 'Confirm email' en Supabase")
    }

    override suspend fun iniciarSesion(usuario: String, password: String) = intentar {
        client.auth.signInWith(Email) {
            email = NubeRemota.correoInterno(usuario)
            this.password = password
        }
        val uid = uidActual() ?: throw IllegalStateException("Sin sesión")
        client.from(PERFIL).select { filter { eq("id", uid) } }.decodeSingle<PerfilDto>()
    }

    override suspend fun cerrarSesion() {
        runCatching { client.auth.signOut() } // si no hay internet, la sesión local igual se cierra
    }

    override suspend fun cambiarColor(colorHex: String) = conSesion { uid ->
        client.from(PERFIL).update({ set("color_icono", colorHex) }) { filter { eq("id", uid) } }
        Unit
    }

    override suspend fun subirReviews(reviews: List<ReviewDto>) = conSesion {
        if (reviews.isNotEmpty()) client.from(REVIEW).upsert(reviews) { onConflict = "user_id,tianguis_id" }
        Unit
    }

    override suspend fun bajarReviews() = intentar {
        client.from(REVIEW_PUBLICA).select().decodeList<ReviewPublicaDto>()
    }

    override suspend fun reemplazarFavoritos(tianguisIds: Set<Int>) = conSesion { uid ->
        val enNube = client.from(FAVORITO).select { filter { eq("user_id", uid) } }
            .decodeList<FavoritoDto>().map { it.tianguisId }.toSet()
        val nuevos = tianguisIds - enNube
        val quitados = enNube - tianguisIds
        if (nuevos.isNotEmpty()) client.from(FAVORITO).upsert(nuevos.map { FavoritoDto(uid, it) })
        if (quitados.isNotEmpty()) client.from(FAVORITO).delete {
            filter {
                eq("user_id", uid)
                isIn("tianguis_id", quitados.toList())
            }
        }
        Unit
    }

    override suspend fun bajarFavoritos() = conSesion { uid ->
        client.from(FAVORITO).select { filter { eq("user_id", uid) } }
            .decodeList<FavoritoDto>().map { it.tianguisId }.toSet()
    }

    override suspend fun subirViajes(viajes: List<ViajeDto>) = conSesion {
        if (viajes.isNotEmpty()) client.from(VIAJE).upsert(viajes) { onConflict = "id" }
        Unit
    }

    override suspend fun bajarViajes() = conSesion { uid ->
        client.from(VIAJE).select { filter { eq("user_id", uid) } }.decodeList<ViajeDto>()
    }

    // ---- manejo de errores en un solo lugar ----

    private suspend fun <T> conSesion(bloque: suspend (uid: String) -> T): ResultadoNube<T> {
        val uid = uidActual() ?: return ResultadoNube.SinSesion
        return intentar { bloque(uid) }
    }

    private suspend fun <T> intentar(bloque: suspend () -> T): ResultadoNube<T> = try {
        ResultadoNube.Ok(bloque())
    } catch (e: CancellationException) {
        throw e
    } catch (e: AuthRestException) {
        when (e.errorCode) {
            AuthErrorCode.UserAlreadyExists, AuthErrorCode.EmailExists -> ResultadoNube.UsuarioOcupado
            AuthErrorCode.InvalidCredentials -> ResultadoNube.CredencialesInvalidas
            else -> ResultadoNube.Error(e.errorCode?.name)
        }
    } catch (_: HttpRequestException) {
        ResultadoNube.SinConexion
    } catch (_: IOException) {
        ResultadoNube.SinConexion
    } catch (e: RestException) {
        ResultadoNube.Error(e.error) // p. ej. un CHECK o una policy de RLS que dijo que no
    } catch (e: IllegalStateException) {
        ResultadoNube.Error(e.message)
    }

    private companion object {
        const val PERFIL = "perfil"
        const val REVIEW = "review"
        const val REVIEW_PUBLICA = "review_publica"
        const val FAVORITO = "favorito"
        const val VIAJE = "viaje"
    }
}
