package com.mercacerca.barriovivo.ui.cuenta

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mercacerca.barriovivo.R
import com.mercacerca.barriovivo.domain.model.ErrorCuenta
import com.mercacerca.barriovivo.domain.model.ResultadoCuenta
import com.mercacerca.barriovivo.domain.model.ValidadorCuenta
import com.mercacerca.barriovivo.ui.components.LineaPunteada
import com.mercacerca.barriovivo.ui.components.SelectorColorAvatar
import com.mercacerca.barriovivo.ui.components.Ticket

/** Login y registro en un mismo "ticket"; cambia de modo con el botón de abajo. */
@Composable
fun AccesoContent(
    form: AccesoForm,
    onUsuario: (String) -> Unit,
    onPassword: (String) -> Unit,
    onColor: (Int) -> Unit,
    onEnviar: () -> Unit,
    onCambiarModo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val registro = form.modo == ModoAcceso.REGISTRO
    var verPassword by rememberSaveable { mutableStateOf(false) }

    Ticket(modifier, folio = if (registro) 21 else 7) {
        Text(
            stringResource(R.string.app_name).uppercase(),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            stringResource(if (registro) R.string.cuenta_ticket_registro else R.string.cuenta_ticket_acceso),
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        LineaPunteada()
        Text(stringResource(R.string.cuenta_para_que), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = form.usuario,
                onValueChange = onUsuario,
                label = { Text(stringResource(R.string.cuenta_usuario)) },
                supportingText = if (registro) {
                    { Text(stringResource(R.string.cuenta_usuario_ayuda, ValidadorCuenta.USUARIO_MIN, ValidadorCuenta.USUARIO_MAX)) }
                } else null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth().semantics {
                    contentType = if (registro) ContentType.NewUsername else ContentType.Username
                },
            )
            OutlinedTextField(
                value = form.password,
                onValueChange = onPassword,
                label = { Text(stringResource(R.string.cuenta_password)) },
                supportingText = if (registro) {
                    { Text(stringResource(R.string.cuenta_password_ayuda, ValidadorCuenta.PASSWORD_MIN)) }
                } else null,
                singleLine = true,
                visualTransformation = if (verPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { verPassword = !verPassword }) {
                        Text(stringResource(if (verPassword) R.string.cuenta_ocultar else R.string.cuenta_ver))
                    }
                },
                keyboardOptions = KeyboardOptions(
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onEnviar() }),
                modifier = Modifier.fillMaxWidth().semantics {
                    contentType = if (registro) ContentType.NewPassword else ContentType.Password
                },
            )
            if (registro) {
                Text(stringResource(R.string.cuenta_color), style = MaterialTheme.typography.titleSmall)
                SelectorColorAvatar(form.colorArgb, onColor)
            }
            form.fallo?.let {
                Text(mensajeDe(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                onClick = onEnviar,
                enabled = !form.enviando && form.usuario.isNotBlank() && form.password.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            ) {
                if (form.enviando) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(
                        stringResource(if (registro) R.string.cuenta_crear else R.string.cuenta_entrar).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
        }
        LineaPunteada()
        TextButton(onClick = onCambiarModo, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(stringResource(if (registro) R.string.cuenta_ya_tengo else R.string.cuenta_no_tengo))
        }
    }
}

@Composable
private fun mensajeDe(r: ResultadoCuenta): String = when (r) {
    is ResultadoCuenta.Bloqueado -> stringResource(R.string.cuenta_err_bloqueado, r.segundos)
    is ResultadoCuenta.Error -> when (r.motivo) {
        ErrorCuenta.USUARIO_CORTO -> stringResource(R.string.cuenta_err_usuario_corto, ValidadorCuenta.USUARIO_MIN)
        ErrorCuenta.USUARIO_LARGO -> stringResource(R.string.cuenta_err_usuario_largo, ValidadorCuenta.USUARIO_MAX)
        ErrorCuenta.USUARIO_CARACTERES -> stringResource(R.string.cuenta_err_usuario_caracteres)
        ErrorCuenta.PASSWORD_CORTA -> stringResource(R.string.cuenta_err_password_corta, ValidadorCuenta.PASSWORD_MIN)
        ErrorCuenta.PASSWORD_LARGA -> stringResource(R.string.cuenta_err_password_larga, ValidadorCuenta.PASSWORD_MAX)
        ErrorCuenta.PASSWORD_IGUAL_USUARIO -> stringResource(R.string.cuenta_err_password_igual)
        ErrorCuenta.USUARIO_OCUPADO -> stringResource(R.string.cuenta_err_ocupado)
        ErrorCuenta.CREDENCIALES_INCORRECTAS -> stringResource(R.string.cuenta_err_credenciales)
        ErrorCuenta.SIN_CONEXION -> stringResource(R.string.cuenta_err_sin_conexion)
        ErrorCuenta.ERROR_NUBE -> stringResource(R.string.cuenta_err_nube)
    }
    ResultadoCuenta.Ok -> ""
}
