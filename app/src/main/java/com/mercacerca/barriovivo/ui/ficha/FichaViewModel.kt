package com.mercacerca.barriovivo.ui.ficha

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.mercacerca.barriovivo.domain.model.Promedios
import com.mercacerca.barriovivo.domain.model.Review
import com.mercacerca.barriovivo.domain.model.Tianguis
import com.mercacerca.barriovivo.domain.repository.ReviewRepository
import com.mercacerca.barriovivo.domain.repository.TianguisRepository
import com.mercacerca.barriovivo.ui.navigation.Ficha
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class FichaUiState(
    val cargando: Boolean = true,
    val tianguis: Tianguis? = null,
    val reviews: List<Review> = emptyList(),
    val promedios: Promedios = Promedios.vacio,
)

@HiltViewModel
class FichaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    tianguisRepo: TianguisRepository,
    reviewRepo: ReviewRepository,
) : ViewModel() {

    /** El id llega en la ruta Ficha(id) de Navigation. */
    private val id = savedStateHandle.toRoute<Ficha>().id

    val state: StateFlow<FichaUiState> =
        combine(tianguisRepo.observePorId(id), reviewRepo.observePorTianguis(id)) { t, reviews ->
            FichaUiState(cargando = false, tianguis = t, reviews = reviews, promedios = Promedios.de(reviews))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FichaUiState())
}
