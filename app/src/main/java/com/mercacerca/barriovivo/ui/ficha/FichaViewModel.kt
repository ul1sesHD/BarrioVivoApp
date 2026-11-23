package com.mercacerca.barriovivo.ui.ficha

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.mercacerca.barriovivo.domain.model.Promedios
import com.mercacerca.barriovivo.domain.model.Review
import com.mercacerca.barriovivo.domain.model.Tianguis
import com.mercacerca.barriovivo.domain.repository.AuthRepository
import com.mercacerca.barriovivo.core.location.LocationProvider
import com.mercacerca.barriovivo.domain.model.ResultadoViaje
import com.mercacerca.barriovivo.domain.repository.FavoritoRepository
import com.mercacerca.barriovivo.domain.repository.ViajeRepository
import com.mercacerca.barriovivo.domain.repository.ReviewRepository
import com.mercacerca.barriovivo.domain.repository.TianguisRepository
import com.mercacerca.barriovivo.ui.navigation.Ficha
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Aviso de una sola vez tras intentar sumar un viaje. */
sealed interface AvisoViaje {
    data class Registrado(val co2Kg: Double) : AvisoViaje
    data object YaHoy : AvisoViaje
    data object SinUbicacion : AvisoViaje
    data class MuyLejos(val km: Double) : AvisoViaje
}

data class FichaUiState(
    val cargando: Boolean = true,
    val tianguis: Tianguis? = null,
    val reviews: List<Review> = emptyList(),
    val promedios: Promedios = Promedios.vacio,
    val conSesion: Boolean = false,
    val esDeConfianza: Boolean = false,
    /** True si este usuario ya lo evaluó (el botón dice "Editar mi evaluación"). */
    val yaLoEvalue: Boolean = false,
    val aviso: AvisoViaje? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FichaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    tianguisRepo: TianguisRepository,
    reviewRepo: ReviewRepository,
    private val auth: AuthRepository,
    private val favoritoRepo: FavoritoRepository,
    private val viajeRepo: ViajeRepository,
    private val location: LocationProvider,
) : ViewModel() {

    private val aviso = MutableStateFlow<AvisoViaje?>(null)

    /** El id llega en la ruta Ficha(id) de Navigation. */
    private val id = savedStateHandle.toRoute<Ficha>().id

    val state: StateFlow<FichaUiState> =
        combine(
            tianguisRepo.observePorId(id),
            reviewRepo.observePorTianguis(id),
            auth.sesion.flatMapLatest { u ->
                // Lo "mío" solo existe con sesión: confianza y si ya lo evalué.
                if (u == null) flowOf(Mio(conSesion = false))
                else combine(favoritoRepo.observeEsFavorito(u.id, id), reviewRepo.observeMia(id, u.id)) { fav, mia ->
                    Mio(conSesion = true, esDeConfianza = fav, yaLoEvalue = mia != null)
                }
            },
            aviso,
        ) { t, reviews, mio, aviso ->
            FichaUiState(
                aviso = aviso,
                cargando = false,
                tianguis = t,
                reviews = reviews,
                promedios = Promedios.de(reviews),
                conSesion = mio.conSesion,
                esDeConfianza = mio.esDeConfianza,
                yaLoEvalue = mio.yaLoEvalue,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FichaUiState())

    private data class Mio(val conSesion: Boolean, val esDeConfianza: Boolean = false, val yaLoEvalue: Boolean = false)

    /** Marca o desmarca "de confianza". Sin sesión no hace nada (la UI manda a iniciar sesión). */
    fun onAlternarConfianza() {
        viewModelScope.launch {
            val u = auth.sesion.first() ?: return@launch
            favoritoRepo.marcar(u.id, id, !state.value.esDeConfianza)
        }
    }

    /** "Voy caminando": mide ida y vuelta desde donde estás y lo suma al ticket de viajes. */
    fun onRegistrarViaje(permisoConcedido: Boolean) {
        viewModelScope.launch {
            val u = auth.sesion.first() ?: return@launch
            val t = state.value.tianguis ?: return@launch
            val origen = if (permisoConcedido) location.ubicacionActual() else null
            aviso.value = when {
                origen == null -> AvisoViaje.SinUbicacion
                else -> when (val r = viajeRepo.registrar(u.id, t, origen)) {
                    is ResultadoViaje.Ok -> AvisoViaje.Registrado(r.viaje.co2Kg)
                    ResultadoViaje.YaRegistradoHoy -> AvisoViaje.YaHoy
                    ResultadoViaje.SinCoordenadas -> AvisoViaje.SinUbicacion
                    is ResultadoViaje.DemasiadoLejos -> AvisoViaje.MuyLejos(r.km)
                }
            }
        }
    }

    fun onAvisoMostrado() {
        aviso.value = null
    }
}
