package com.mercacerca.barriovivo.data.sync

import com.mercacerca.barriovivo.data.local.FavoritoDao
import com.mercacerca.barriovivo.data.local.FavoritoEntity
import com.mercacerca.barriovivo.data.local.ReviewDao
import com.mercacerca.barriovivo.data.local.ReviewEntity
import com.mercacerca.barriovivo.data.local.TianguisDao
import com.mercacerca.barriovivo.data.local.UsuarioDao
import com.mercacerca.barriovivo.data.local.UsuarioEntity
import com.mercacerca.barriovivo.data.local.ViajeDao
import com.mercacerca.barriovivo.data.local.ViajeEntity
import com.mercacerca.barriovivo.data.remote.NubeRemota
import com.mercacerca.barriovivo.data.remote.ResultadoNube
import com.mercacerca.barriovivo.data.remote.ReviewDto
import com.mercacerca.barriovivo.data.remote.ReviewPublicaDto
import com.mercacerca.barriovivo.data.remote.ViajeDto
import com.mercacerca.barriovivo.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** Pide una sincronización "cuando se pueda", sin esperar. Los repositorios la llaman tras escribir. */
fun interface SolicitudSync {
    fun solicitar()
}

/**
 * Sincronización offline-first: Room sigue siendo la fuente de verdad de la pantalla.
 *  1. Baja las reseñas públicas de todos (también sin sesión).
 *  2. Si hay sesión en la nube: la primera vez en este teléfono baja tus favoritos, viajes y reseñas.
 *  3. Sube TU estado completo (reseñas, favoritos, viajes). Subir todo es idempotente: si falla
 *     por falta de internet, la siguiente vez se manda igual. Son pocos datos por persona.
 */
@Singleton
class SincronizadorNube @Inject constructor(
    private val nube: NubeRemota,
    private val usuarios: UsuarioDao,
    private val reviews: ReviewDao,
    private val favoritos: FavoritoDao,
    private val viajes: ViajeDao,
    private val tianguis: TianguisDao,
    @param:ApplicationScope private val appScope: CoroutineScope,
) : SolicitudSync {

    private val mutex = Mutex()

    override fun solicitar() {
        if (nube.activa) appScope.launch { sincronizar() }
    }

    /** True si llegó a hablar con la nube. Nunca lanza: los errores solo se reintentan después. */
    suspend fun sincronizar(): Boolean = mutex.withLock {
        if (!nube.activa) return false
        val publicas = (nube.bajarReviews() as? ResultadoNube.Ok)?.valor ?: return false

        val yo = usuarios.sesionActual()
        val uid = nube.uidActual()
        // Solo se sube si la sesión local y la de la nube son la misma persona.
        if (yo == null || uid == null || yo.remoteId != uid) {
            guardarReviewsDeOtros(publicas, miUid = null)
            return true
        }
        guardarReviewsDeOtros(publicas, miUid = uid)

        if (!yo.syncInicial) {
            bajarLoMio(yo, uid, publicas)
            usuarios.marcarSyncInicial(yo.id)
        }
        subirLoMio(yo, uid)
        true
    }

    private suspend fun guardarReviewsDeOtros(publicas: List<ReviewPublicaDto>, miUid: String?) {
        val existentes = tianguis.ids().toSet() // la reseña debe apuntar a un tianguis del catálogo
        publicas.filter { it.userId != miUid && it.tianguisId in existentes }.forEach { dto ->
            val local = reviews.porRemoteId(dto.id)
            reviews.guardar(dto.toEntity(id = local?.id ?: 0, usuarioId = null))
        }
    }

    /** Primer login en este teléfono: trae lo que ya tenías en la nube (sin borrar lo local). */
    private suspend fun bajarLoMio(yo: UsuarioEntity, uid: String, publicas: List<ReviewPublicaDto>) {
        val existentes = tianguis.ids().toSet()
        val mias = reviews.deUsuario(yo.id).map { it.tianguisId }.toSet()
        publicas.filter { it.userId == uid && it.tianguisId in existentes && it.tianguisId !in mias }
            .forEach { reviews.guardar(it.toEntity(id = 0, usuarioId = yo.id)) }

        (nube.bajarFavoritos() as? ResultadoNube.Ok)?.valor
            ?.filter { it in existentes }
            ?.forEach { favoritos.agregar(FavoritoEntity(yo.id, it)) }

        (nube.bajarViajes() as? ResultadoNube.Ok)?.valor
            ?.filterNot { viajes.existeRemoto(it.id) }
            ?.forEach { viajes.insertar(it.toEntity(yo.id, existentes)) }
    }

    private suspend fun subirLoMio(yo: UsuarioEntity, uid: String) {
        nube.subirReviews(reviews.deUsuario(yo.id).map { it.toDto(uid) })
        nube.reemplazarFavoritos(favoritos.idsDeUsuario(yo.id).toSet())
        val mios = viajes.deUsuario(yo.id).map { v ->
            // Viajes creados antes de la fase 7 no tenían uuid: se les asigna uno fijo antes de subir.
            v.remoteId?.let { v } ?: v.copy(remoteId = UUID.randomUUID().toString()).also {
                viajes.asignarRemoteId(v.id, it.remoteId!!)
            }
        }
        nube.subirViajes(mios.map { it.toDto(uid) })
    }
}

// ---- mapeos Room <-> nube ----

private fun ReviewEntity.toDto(uid: String) =
    ReviewDto(uid, tianguisId, variedad, tamano, calidad, confianza, comentario)

@OptIn(ExperimentalTime::class)
private fun ReviewPublicaDto.toEntity(id: Int, usuarioId: Int?) = ReviewEntity(
    id = id,
    tianguisId = tianguisId,
    autor = autor,
    variedad = variedad,
    tamano = tamano,
    calidad = calidad,
    confianza = confianza,
    comentario = comentario,
    creadoEn = runCatching { Instant.parse(actualizadoEn).toEpochMilliseconds() }.getOrDefault(0L),
    usuarioId = usuarioId,
    remoteId = if (usuarioId == null) this.id else null, // las mías se identifican por (usuario, tianguis)
    colorAutor = colorIcono,
)

@OptIn(ExperimentalTime::class)
private fun ViajeEntity.toDto(uid: String) = ViajeDto(
    id = remoteId!!,
    userId = uid,
    tianguisId = tianguisId,
    tituloTianguis = tituloTianguis,
    distanciaKm = distanciaKm,
    co2Kg = co2Kg,
    fecha = Instant.fromEpochMilliseconds(fecha).toString(),
)

@OptIn(ExperimentalTime::class)
private fun ViajeDto.toEntity(usuarioId: Int, existentes: Set<Int>) = ViajeEntity(
    usuarioId = usuarioId,
    tianguisId = tianguisId?.takeIf { it in existentes },
    tituloTianguis = tituloTianguis,
    distanciaKm = distanciaKm,
    co2Kg = co2Kg,
    fecha = runCatching { Instant.parse(fecha).toEpochMilliseconds() }.getOrDefault(0L),
    remoteId = id,
)
