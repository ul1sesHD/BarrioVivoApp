package com.mercacerca.barriovivo.ui.map

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mercacerca.barriovivo.R
import com.mercacerca.barriovivo.core.geo.Geo
import com.mercacerca.barriovivo.core.geo.LatLon
import com.mercacerca.barriovivo.core.location.LocationProvider
import com.mercacerca.barriovivo.domain.model.Dia
import com.mercacerca.barriovivo.domain.model.Tianguis
import com.mercacerca.barriovivo.domain.repository.TianguisRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Orden para mover la cámara. [id] cambia en cada orden, aunque el punto sea el mismo. */
data class OrdenCamara(val destino: LatLon, val zoom: Double, val id: Long = System.nanoTime())

data class MapUiState(
    val dia: Dia = Dia.hoy(),
    val tianguis: List<Tianguis> = emptyList(),
    val cargando: Boolean = true,
    val seleccionado: Tianguis? = null,
    val miUbicacion: LatLon? = null,
    val camara: OrdenCamara? = null,
    val buscandoUbicacion: Boolean = false,
    @param:StringRes val mensaje: Int? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MapViewModel @Inject constructor(
    private val repo: TianguisRepository,
    private val location: LocationProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(MapUiState())
    val state: StateFlow<MapUiState> = _state.asStateFlow()

    init {
        // Cada vez que cambia el día se cambia de consulta (flatMapLatest cancela la anterior).
        viewModelScope.launch {
            _state.map { it.dia }.distinctUntilChanged()
                .flatMapLatest(repo::observeMapeablesPorDia)
                .collect { lista -> _state.update { it.copy(tianguis = lista, cargando = false) } }
        }
    }

    fun onDiaSeleccionado(dia: Dia) = _state.update { it.copy(dia = dia, seleccionado = null) }

    fun onTianguisSeleccionado(t: Tianguis?) = _state.update { it.copy(seleccionado = t) }

    fun onMensajeMostrado() = _state.update { it.copy(mensaje = null) }

    /** Lo llama la UI después de pedir el permiso. Si no hay permiso o falla, centra en el Zócalo. */
    fun onCercaDeMi(permisoConcedido: Boolean) {
        if (_state.value.buscandoUbicacion) return
        _state.update { it.copy(buscandoUbicacion = true) }
        viewModelScope.launch {
            val aqui = if (permisoConcedido) location.ubicacionActual() else null
            _state.update {
                it.copy(
                    buscandoUbicacion = false,
                    miUbicacion = aqui ?: it.miUbicacion,
                    camara = OrdenCamara(aqui ?: Geo.ZOCALO, if (aqui != null) ZOOM_CERCA else ZOOM_CIUDAD),
                    mensaje = when {
                        !permisoConcedido -> R.string.mapa_sin_permiso
                        aqui == null -> R.string.mapa_sin_ubicacion
                        else -> null
                    },
                )
            }
        }
    }

    companion object {
        const val ZOOM_CIUDAD = 12.0
        const val ZOOM_CERCA = 15.0
    }
}
