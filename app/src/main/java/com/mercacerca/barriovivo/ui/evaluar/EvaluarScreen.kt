package com.mercacerca.barriovivo.ui.evaluar

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mercacerca.barriovivo.R
import com.mercacerca.barriovivo.domain.model.ErrorReview
import com.mercacerca.barriovivo.domain.model.ValidadorReview
import com.mercacerca.barriovivo.ui.components.EstrellasInput

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvaluarScreen(
    onListo: () -> Unit,
    onIniciarSesion: () -> Unit,
    viewModel: EvaluarViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.guardado) { if (state.guardado) onListo() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.evaluar_titulo), style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onListo) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.atras)) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).imePadding()) {
            when {
                state.cargando -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.usuario == null -> SinSesion(onIniciarSesion, Modifier.align(Alignment.Center))
                else -> Formulario(state, viewModel)
            }
        }
    }
}

@Composable
private fun Formulario(state: EvaluarUiState, vm: EvaluarViewModel) {
    val b = state.borrador
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(state.titulo, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Pregunta(R.string.pregunta_variedad, R.string.pregunta_variedad_ayuda, b.variedad, vm::onVariedad)
        Pregunta(R.string.pregunta_tamano, R.string.pregunta_tamano_ayuda, b.tamano, vm::onTamano)
        Pregunta(R.string.pregunta_calidad, R.string.pregunta_calidad_ayuda, b.calidad, vm::onCalidad)

        Cartel {
            Row(
                Modifier.fillMaxWidth().toggleable(value = b.confianza, role = Role.Switch, onValueChange = vm::onConfianza),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.evaluar_confianza), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.evaluar_confianza_ayuda), style = MaterialTheme.typography.bodySmall)
                }
                Switch(checked = b.confianza, onCheckedChange = null) // el renglón entero es el que se toca
            }
        }

        OutlinedTextField(
            value = b.comentario,
            onValueChange = vm::onComentario,
            label = { Text(stringResource(R.string.evaluar_comentario)) },
            supportingText = { Text("${b.comentario.length} / ${ValidadorReview.COMENTARIO_MAX}") },
            isError = b.comentario.length > ValidadorReview.COMENTARIO_MAX,
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
        )

        state.error?.let { Text(mensajeDe(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }

        Button(
            onClick = vm::onGuardar,
            enabled = !state.guardando,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        ) {
            if (state.guardando) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            else Text(
                stringResource(if (state.editando) R.string.evaluar_actualizar else R.string.evaluar_guardar).uppercase(),
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

@Composable
private fun Pregunta(@StringRes titulo: Int, @StringRes ayuda: Int, valor: Int, onCambio: (Int) -> Unit) {
    Cartel {
        Text(stringResource(titulo).uppercase(), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(ayuda), style = MaterialTheme.typography.bodyMedium)
        EstrellasInput(valor, onCambio)
    }
}

@Composable
private fun SinSesion(onIniciarSesion: () -> Unit, modifier: Modifier) {
    Column(modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.evaluar_requiere_sesion), style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onIniciarSesion, modifier = Modifier.heightIn(min = 48.dp)) { Text(stringResource(R.string.ficha_iniciar_sesion)) }
    }
}

@Composable
private fun Cartel(content: @Composable () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) { content() }
    }
}

@Composable
private fun mensajeDe(e: ErrorReview): String = when (e) {
    ErrorReview.FALTA_VARIEDAD -> stringResource(R.string.evaluar_err_falta, stringResource(R.string.pregunta_variedad))
    ErrorReview.FALTA_TAMANO -> stringResource(R.string.evaluar_err_falta, stringResource(R.string.pregunta_tamano))
    ErrorReview.FALTA_CALIDAD -> stringResource(R.string.evaluar_err_falta, stringResource(R.string.pregunta_calidad))
    ErrorReview.COMENTARIO_LARGO -> stringResource(R.string.evaluar_err_largo, ValidadorReview.COMENTARIO_MAX)
}
