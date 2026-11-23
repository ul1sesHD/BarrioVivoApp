package com.mercacerca.barriovivo.ui.cercanos

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mercacerca.barriovivo.R
import com.mercacerca.barriovivo.core.geo.Geo
import com.mercacerca.barriovivo.core.location.LocationProvider
import com.mercacerca.barriovivo.domain.model.Cercania
import com.mercacerca.barriovivo.domain.model.Dia
import com.mercacerca.barriovivo.domain.model.MejorDia
import com.mercacerca.barriovivo.domain.model.TianguisCercano
import com.mercacerca.barriovivo.domain.repository.TianguisRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CercanosUiState(
    val dia: Dia = Dia.hoy(),
    val cargando: Boolean = true,
    val cercanos: List<TianguisCercano> = emptyList(),
    val mejorDia: MejorDia? = null,
    /** False = aún no hay ubicación: las distancias se miden desde el Zócalo. */
    val desdeMiUbicacion: Boolean = false,
    val buscandoUbicacion: Boolean = false,
    @param:StringRes val mensaje: Int? = null,
)

@HiltViewModel
class CercanosViewModel @Inject constructor(
    repo: TianguisRepository,
    private val location: LocationProvider,
) : ViewModel() {

    private val dia = MutableStateFlow(Dia.hoy())
    private val estado = MutableStateFlow(CercanosUiState()) // solo buscando/mensaje

    val state: StateFlow<CercanosUiState> =
        combine(repo.observeMapeables(), dia, location.ultimaConocida, estado) { todos, d, aqui, e ->
            val origen = aqui ?: Geo.ZOCALO
            e.copy(
                dia = d,
                cargando = false,
                cercanos = Cercania.ordenar(origen, todos.filter { it.dia == d }),
                mejorDia = Cercania.mejorDia(origen, todos, hoy = Dia.hoy()),
                desdeMiUbicacion = aqui != null,
            )
        }
            .flowOn(Dispatchers.Default) // ~1,300 distancias: fuera del hilo principal
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CercanosUiState())

    init {
        // Si ya dio permiso antes, no hay que preguntar: refrescamos la ubicación al entrar.
        if (location.tienePermiso()) onUbicacion(permisoConcedido = true)
    }

    fun onDia(d: Dia) {
        dia.value = d
    }

    fun onMensajeMostrado() = estado.update { it.copy(mensaje = null) }

    fun onUbicacion(permisoConcedido: Boolean) {
        if (estado.value.buscandoUbicacion) return
        estado.update { it.copy(buscandoUbicacion = true) }
        viewModelScope.launch {
            val aqui = if (permisoConcedido) location.ubicacionActual() else null
            estado.update {
                it.copy(
                    buscandoUbicacion = false,
                    mensaje = when {
                        !permisoConcedido -> R.string.cercanos_sin_permiso
                        aqui == null -> R.string.mapa_sin_ubicacion
                        else -> null
                    },
                )
            }
        }
    }
}
