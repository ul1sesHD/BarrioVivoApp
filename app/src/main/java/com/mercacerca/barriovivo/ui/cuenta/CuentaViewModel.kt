package com.mercacerca.barriovivo.ui.cuenta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mercacerca.barriovivo.domain.model.ResultadoCuenta
import com.mercacerca.barriovivo.domain.model.ResumenViajes
import com.mercacerca.barriovivo.domain.model.ReviewPropia
import com.mercacerca.barriovivo.domain.model.Tianguis
import com.mercacerca.barriovivo.domain.model.Usuario
import com.mercacerca.barriovivo.domain.model.ValidadorCuenta
import com.mercacerca.barriovivo.domain.repository.AuthRepository
import com.mercacerca.barriovivo.domain.repository.FavoritoRepository
import com.mercacerca.barriovivo.domain.repository.ReviewRepository
import com.mercacerca.barriovivo.domain.repository.ViajeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ModoAcceso { LOGIN, REGISTRO }

data class AccesoForm(
    val modo: ModoAcceso = ModoAcceso.LOGIN,
    val usuario: String = "",
    val password: String = "",
    val colorArgb: Int = ValidadorCuenta.COLOR_DEFAULT,
    val enviando: Boolean = false,
    /** Último resultado fallido (Error o Bloqueado) para mostrarlo; null si no hay. */
    val fallo: ResultadoCuenta? = null,
)

data class CuentaUiState(
    val cargando: Boolean = true,
    val usuario: Usuario? = null,
    val form: AccesoForm = AccesoForm(),
    val misReviews: List<ReviewPropia> = emptyList(),
    val misFavoritos: List<Tianguis> = emptyList(),
    val viajes: ResumenViajes = ResumenViajes.vacio,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CuentaViewModel @Inject constructor(
    private val auth: AuthRepository,
    reviewRepo: ReviewRepository,
    favoritoRepo: FavoritoRepository,
    viajeRepo: ViajeRepository,
) : ViewModel() {

    private val form = MutableStateFlow(AccesoForm())

    private data class Actividad(
        val reviews: List<ReviewPropia> = emptyList(),
        val favoritos: List<Tianguis> = emptyList(),
        val viajes: ResumenViajes = ResumenViajes.vacio,
    )

    /** Mis reseñas, tianguis de confianza y viajes; vacíos si no hay sesión. */
    private val actividad = auth.sesion.flatMapLatest { u ->
        if (u == null) flowOf(Actividad())
        else combine(
            reviewRepo.observeDeUsuario(u.id),
            favoritoRepo.observeFavoritos(u.id),
            viajeRepo.observeResumen(u.id),
            ::Actividad,
        )
    }

    val state: StateFlow<CuentaUiState> = combine(auth.sesion, form, actividad) { u, f, a ->
        CuentaUiState(false, u, f, a.reviews, a.favoritos, a.viajes)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CuentaUiState())

    fun onUsuario(v: String) = form.update { it.copy(usuario = v.take(ValidadorCuenta.USUARIO_MAX), fallo = null) }
    fun onPassword(v: String) = form.update { it.copy(password = v.take(ValidadorCuenta.PASSWORD_MAX), fallo = null) }
    fun onColor(argb: Int) = form.update { it.copy(colorArgb = argb) }

    fun onCambiarModo() = form.update {
        it.copy(modo = if (it.modo == ModoAcceso.LOGIN) ModoAcceso.REGISTRO else ModoAcceso.LOGIN, password = "", fallo = null)
    }

    fun onEnviar() {
        val f = form.value
        if (f.enviando || f.usuario.isBlank() || f.password.isEmpty()) return
        form.update { it.copy(enviando = true, fallo = null) }
        viewModelScope.launch {
            val r = when (f.modo) {
                ModoAcceso.LOGIN -> auth.iniciarSesion(f.usuario, f.password)
                ModoAcceso.REGISTRO -> auth.registrar(f.usuario, f.password, f.colorArgb)
            }
            // La contraseña nunca se queda en el estado después de usarla.
            form.update {
                if (r is ResultadoCuenta.Ok) AccesoForm() else it.copy(enviando = false, password = "", fallo = r)
            }
        }
    }

    fun onCambiarColorPerfil(argb: Int) {
        viewModelScope.launch { auth.cambiarColor(argb) }
    }

    fun onCerrarSesion() {
        viewModelScope.launch { auth.cerrarSesion() }
    }
}
