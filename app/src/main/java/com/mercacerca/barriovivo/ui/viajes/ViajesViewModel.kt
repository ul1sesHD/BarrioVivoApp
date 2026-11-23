package com.mercacerca.barriovivo.ui.viajes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mercacerca.barriovivo.domain.model.ResumenViajes
import com.mercacerca.barriovivo.domain.model.Usuario
import com.mercacerca.barriovivo.domain.repository.AuthRepository
import com.mercacerca.barriovivo.domain.repository.ViajeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ViajesUiState(
    val cargando: Boolean = true,
    val usuario: Usuario? = null,
    val resumen: ResumenViajes = ResumenViajes.vacio,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ViajesViewModel @Inject constructor(auth: AuthRepository, viajes: ViajeRepository) : ViewModel() {
    val state: StateFlow<ViajesUiState> = auth.sesion.flatMapLatest { u ->
        if (u == null) flowOf(ViajesUiState(cargando = false))
        else viajes.observeResumen(u.id).map { ViajesUiState(cargando = false, usuario = u, resumen = it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ViajesUiState())
}
