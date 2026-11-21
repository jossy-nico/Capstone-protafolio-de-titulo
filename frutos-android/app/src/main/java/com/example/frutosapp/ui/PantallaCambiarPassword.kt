
package com.example.frutosapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff

import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable

import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

import com.example.frutosapp.api.RetroFistCliente

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaCambiarPassword(
    usuarioId: Long,
    onNavigateBack: () -> Unit,
    onPasswordChanged: () -> Unit
) {

    val scope = rememberCoroutineScope()

    // ==========================================
    // CAMPOS DEL FORMULARIO
    // ==========================================

    var passwordActual by rememberSaveable {
        mutableStateOf("")
    }

    var passwordNueva by rememberSaveable {
        mutableStateOf("")
    }

    var confirmarPassword by rememberSaveable {
        mutableStateOf("")
    }

    // ==========================================
    // VISIBILIDAD DE CONTRASEÑAS
    // ==========================================

    var mostrarActual by rememberSaveable {
        mutableStateOf(false)
    }

    var mostrarNueva by rememberSaveable {
        mutableStateOf(false)
    }

    var mostrarConfirmacion by rememberSaveable {
        mutableStateOf(false)
    }

    // ==========================================
    // ESTADO DE LA OPERACIÓN
    // ==========================================

    var cargando by remember {
        mutableStateOf(false)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    // ==========================================
    // VALIDACIONES
    // ==========================================

    val nuevaValida =
        passwordNueva.length in 8..72

    val coinciden =
        passwordNueva == confirmarPassword

    val formularioValido =
        usuarioId > 0L &&
                passwordActual.isNotBlank() &&
                nuevaValida &&
                coinciden &&
                confirmarPassword.isNotBlank() &&
                passwordActual != passwordNueva

    // ==========================================
    // INTERFAZ
    // ==========================================

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Cambiar contraseña")
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        enabled = !cargando
                    ) {
                        Icon(
                            imageVector =
                                Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),

            verticalArrangement =
                Arrangement.spacedBy(16.dp)
        ) {

            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )

            Text(
                text = "Seguridad de tu cuenta",
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = "Para cambiar tu contraseña, " +
                        "confirma primero la contraseña actual.",
                style = MaterialTheme.typography.bodyMedium
            )

            // ==================================
            // 1. CONTRASEÑA ACTUAL
            // ==================================

            OutlinedTextField(
                value = passwordActual,
                onValueChange = {
                    passwordActual = it
                    error = null
                },
                label = {
                    Text("Contraseña actual")
                },
                visualTransformation =
                    if (mostrarActual) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            mostrarActual = !mostrarActual
                        },
                        enabled = !cargando
                    ) {
                        Icon(
                            imageVector =
                                if (mostrarActual) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                            contentDescription =
                                if (mostrarActual) {
                                    "Ocultar contraseña actual"
                                } else {
                                    "Mostrar contraseña actual"
                                }
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password
                ),
                enabled = !cargando,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // ==================================
            // 2. NUEVA CONTRASEÑA
            // ==================================

            OutlinedTextField(
                value = passwordNueva,
                onValueChange = {
                    passwordNueva = it
                    error = null
                },
                label = {
                    Text("Nueva contraseña")
                },
                visualTransformation =
                    if (mostrarNueva) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            mostrarNueva = !mostrarNueva
                        },
                        enabled = !cargando
                    ) {
                        Icon(
                            imageVector =
                                if (mostrarNueva) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                            contentDescription =
                                if (mostrarNueva) {
                                    "Ocultar nueva contraseña"
                                } else {
                                    "Mostrar nueva contraseña"
                                }
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password
                ),
                enabled = !cargando,
                singleLine = true,
                isError =
                    passwordNueva.isNotEmpty() &&
                            !nuevaValida,
                supportingText = {
                    Text("Entre 8 y 72 caracteres.")
                },
                modifier = Modifier.fillMaxWidth()
            )

            // ==================================
            // 3. CONFIRMAR CONTRASEÑA
            // ==================================

            OutlinedTextField(
                value = confirmarPassword,
                onValueChange = {
                    confirmarPassword = it
                    error = null
                },
                label = {
                    Text("Confirmar nueva contraseña")
                },
                visualTransformation =
                    if (mostrarConfirmacion) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            mostrarConfirmacion =
                                !mostrarConfirmacion
                        },
                        enabled = !cargando
                    ) {
                        Icon(
                            imageVector =
                                if (mostrarConfirmacion) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                            contentDescription =
                                if (mostrarConfirmacion) {
                                    "Ocultar confirmación"
                                } else {
                                    "Mostrar confirmación"
                                }
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password
                ),
                enabled = !cargando,
                singleLine = true,
                isError =
                    confirmarPassword.isNotEmpty() &&
                            !coinciden,
                supportingText = {
                    if (
                        confirmarPassword.isNotEmpty() &&
                        !coinciden
                    ) {
                        Text(
                            "Las contraseñas no coinciden."
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // ==================================
            // MENSAJES DE ERROR
            // ==================================

            if (error != null) {
                Text(
                    text = error.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // ==================================
            // MENSAJE DE SEGURIDAD
            // ==================================

            Text(
                text = "Al actualizar tu contraseña, " +
                        "se cerrará la sesión y deberás " +
                        "ingresar con la nueva contraseña.",
                style = MaterialTheme.typography.bodySmall
            )

            // ==================================
            // ACTUALIZAR CONTRASEÑA
            // ==================================

            Button(
                onClick = {

                    if (!formularioValido || cargando) {
                        return@Button
                    }

                    scope.launch {

                        cargando = true
                        error = null

                        try {

                            val respuesta =
                                RetroFistCliente.usuarioApi
                                    .cambiarPassword(
                                        usuarioId,
                                        mapOf(
                                            "passwordActual" to
                                                    passwordActual,
                                            "passwordNueva" to
                                                    passwordNueva
                                        )
                                    )

                            if (respuesta.isSuccessful) {

                                passwordActual = ""
                                passwordNueva = ""
                                confirmarPassword = ""

                                mostrarActual = false
                                mostrarNueva = false
                                mostrarConfirmacion = false

                                onPasswordChanged()

                            } else {

                                error = when (
                                    respuesta.code()
                                ) {
                                    400 ->
                                        "No se pudo cambiar la contraseña. " +
                                                "Verifica la contraseña actual " +
                                                "y los requisitos de la nueva."

                                    401, 403 ->
                                        "Tu sesión no está autorizada. " +
                                                "Inicia sesión nuevamente."

                                    else ->
                                        "No se pudo actualizar la contraseña. " +
                                                "Código: ${respuesta.code()}"
                                }
                            }

                        } catch (e: CancellationException) {

                            throw e

                        } catch (_: Exception) {

                            error =
                                "Error de conexión. Comprueba " +
                                        "tu conexión antes de reintentar."

                        } finally {

                            cargando = false
                        }
                    }
                },
                enabled = formularioValido && !cargando,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {

                if (cargando) {

                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )

                } else {

                    Text("Actualizar contraseña")
                }
            }
        }
    }
}
