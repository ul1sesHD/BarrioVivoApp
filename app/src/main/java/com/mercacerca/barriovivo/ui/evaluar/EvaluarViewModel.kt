package com.mercacerca.barriovivo.ui.evaluar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.mercacerca.barriovivo.domain.model.BorradorReview
import com.mercacerca.barriovivo.domain.model.ErrorReview
import com.mercacerca.barriovivo.domain.model.ResultadoReview
import com.mercacerca.barriovivo.domain.model.Usuario
import com.mercacerca.barriovivo.domain.model.ValidadorReview
import com.mercacerca.barriovivo.domain.repository.AuthRepository
import com.mercacerca.barriovivo.domain.repository.FavoritoRepository
import com.mercacerca.barriovivo.domain.repository.ReviewRepository
import com.mercacerca.barriovivo.domain.repository.TianguisRepository
import com.mercacerca.barriovivo.ui.navigation.Evaluar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EvaluarUiState(
    val cargando: Boolean = true,
    val titulo: String = "",
    val usuario: Usuario? = null,
    val borrador: BorradorReview = BorradorReview(),
    /** True si ya lo había evaluado: el botón dice "Actualizar". */
    val editando: Boolean = false,
    val guardando: Boolean = false,
    val error: ErrorReview? = null,
    /** Se vuelve true al guardar bien: la pantalla regresa sola. */
    val guardado: Boolean = false,
)

@HiltViewModel
class EvaluarViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tianguisRepo: TianguisRepository,
    private val reviewRepo: ReviewRepository,
    private val favoritoRepo: FavoritoRepository,
    private val auth: AuthRepository,
) : ViewModel() {

    private val tianguisId = savedStateHandle.toRoute<Evaluar>().tianguisId
    private val _state = MutableStateFlow(EvaluarUiState())
    val state: StateFlow<EvaluarUiState> = _state.asStateFlow()

    init {
        // Carga una vez: si ya lo evaluó, el formulario arranca con sus respuestas.
        viewModelScope.launch {
            val usuario = auth.sesion.first()
            val titulo = tianguisRepo.observePorId(tianguisId).first()?.titulo.orEmpty()
            val mia = usuario?.let { reviewRepo.observeMia(tianguisId, it.id).first() }
            val deConfianza = usuario?.let { favoritoRepo.observeEsFavorito(it.id, tianguisId).first() } ?: false
            _state.update {
                it.copy(
                    cargando = false,
                    titulo = titulo,
                    usuario = usuario,
                    editando = mia != null,
                    borrador = mia?.let { r ->
                        BorradorReview(r.variedad, r.tamano, r.calidad, deConfianza, r.comentario.orEmpty())
                    } ?: BorradorReview(confianza = deConfianza),
                )
            }
        }
    }

    fun onVariedad(n: Int) = editar { copy(variedad = n) }
    fun onTamano(n: Int) = editar { copy(tamano = n) }
    fun onCalidad(n: Int) = editar { copy(calidad = n) }
    fun onConfianza(v: Boolean) = editar { copy(confianza = v) }
    fun onComentario(t: String) = editar { copy(comentario = t.take(ValidadorReview.COMENTARIO_MAX + 20)) }

    private fun editar(cambio: BorradorReview.() -> BorradorReview) =
        _state.update { it.copy(borrador = it.borrador.cambio(), error = null) }

    fun onGuardar() {
        val s = _state.value
        val usuario = s.usuario ?: return
        if (s.guardando) return
        _state.update { it.copy(guardando = true, error = null) }
        viewModelScope.launch {
            when (val r = reviewRepo.guardar(usuario, tianguisId, s.borrador)) {
                ResultadoReview.Ok -> _state.update { it.copy(guardando = false, guardado = true) }
                is ResultadoReview.Error -> _state.update { it.copy(guardando = false, error = r.motivo) }
            }
        }
    }
}
