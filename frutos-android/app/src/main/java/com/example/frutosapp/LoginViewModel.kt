
package com.example.frutosapp

import android.app.Application

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.example.frutosapp.api.RetroFistCliente
import com.example.frutosapp.api.SesionSegura

import com.example.frutosapp.model.LoginRequest
import com.example.frutosapp.model.Usuario

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import org.json.JSONObject

sealed class LoginState {

    object Idle : LoginState()

    object Loading : LoginState()

    data class Success(
        val usuario: Usuario
    ) : LoginState()

    data class Error(
        val mensaje: String
    ) : LoginState()
}

class LoginViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val sesionSegura =
        SesionSegura(application)

    private val _loginState =
        MutableStateFlow<LoginState>(
            LoginState.Idle
        )

    val loginState = _loginState.asStateFlow()

    // ==========================================
    // INICIAR SESIÓN PERSISTENTE
    // ==========================================

    fun iniciarSesion(
        correo: String,
        password: String
    ) {

        if (_loginState.value is LoginState.Loading) {
            return
        }

        viewModelScope.launch {

            _loginState.value = LoginState.Loading

            try {

                if (
                    correo.isBlank() ||
                    password.isBlank()
                ) {

                    _loginState.value =
                        LoginState.Error(
                            "Ingresa tu correo y contraseña."
                        )

                    return@launch
                }

                val credenciales = LoginRequest(
                    correo = correo.trim().lowercase(),
                    password = password
                )

                // ==================================
                // 1. INICIAR SESIÓN PERSISTENTE
                // ==================================

                val respuesta =
                    RetroFistCliente.authApi
                        .loginPersistente(
                            credenciales
                        )

                if (!respuesta.isSuccessful) {

                    _loginState.value =
                        LoginState.Error(
                            when (respuesta.code()) {

                                400, 401 ->
                                    "Correo o contraseña incorrectos, " +
                                            "o la cuenta no está habilitada."

                                403 ->
                                    "La cuenta no tiene autorización " +
                                            "para iniciar sesión."

                                else ->
                                    "No fue posible iniciar sesión. " +
                                            "Código: ${respuesta.code()}"
                            }
                        )

                    return@launch
                }

                val contenido =
                    respuesta.body()?.string()

                if (contenido.isNullOrBlank()) {

                    _loginState.value =
                        LoginState.Error(
                            "El servidor no entregó " +
                                    "datos de autenticación."
                        )

                    return@launch
                }

                val json = JSONObject(contenido)

                // ==================================
                // 2. VALIDAR AMBOS TOKENS
                // ==================================

                val token = json.optString(
                    "token",
                    ""
                )

                val refreshToken = json.optString(
                    "refreshToken",
                    ""
                )

                if (
                    token.isBlank() ||
                    refreshToken.isBlank()
                ) {

                    _loginState.value =
                        LoginState.Error(
                            "El servidor no entregó " +
                                    "las credenciales de sesión."
                        )

                    return@launch
                }

                // ==================================
                // 3. RECUPERAR PERFIL DEL CLIENTE
                // ==================================
                //
                // Se mantiene temporalmente este
                // método por compatibilidad con
                // las pantallas existentes.
                // ==================================

                val respuestaUsuario =
                    RetroFistCliente.authApi.login(
                        credenciales
                    )

                if (!respuestaUsuario.isSuccessful) {

                    _loginState.value =
                        LoginState.Error(
                            "Se autenticó al usuario, " +
                                    "pero no fue posible " +
                                    "recuperar su perfil."
                        )

                    return@launch
                }

                val usuario =
                    respuestaUsuario.body()

                if (
                    usuario == null ||
                    usuario.idUsuario == null
                ) {

                    _loginState.value =
                        LoginState.Error(
                            "El servidor no entregó " +
                                    "un usuario válido."
                        )

                    return@launch
                }

                if (
                    !usuario.tipoUsuario.equals(
                        "CLIENTE",
                        ignoreCase = true
                    )
                ) {

                    _loginState.value =
                        LoginState.Error(
                            "Esta aplicación requiere " +
                                    "una cuenta de cliente."
                        )

                    return@launch
                }

                // ==================================
                // 4. VALIDAR IDENTIDAD
                // ==================================
                //
                // Evitar asociar una credencial
                // emitida para una cuenta distinta
                // al perfil recuperado.
                // ==================================

                val usuarioJson =
                    json.optJSONObject("usuario")

                val idToken =
                    usuarioJson?.optLong(
                        "idUsuario",
                        -1L
                    ) ?: -1L

                if (
                    idToken <= 0L ||
                    idToken != usuario.idUsuario.toLong()
                ) {

                    _loginState.value =
                        LoginState.Error(
                            "No fue posible validar " +
                                    "la identidad del cliente."
                        )

                    return@launch
                }

                // ==================================
                // 5. GUARDAR CREDENCIALES
                // ==================================
                //
                // Refresh token:
                // Cifrado en Android Keystore.
                //
                // JWT:
                // Conservado en memoria para las
                // llamadas actuales de Retrofit.
                // ==================================

                RetroFistCliente.limpiarTokenJwt()

                sesionSegura.limpiar()

                sesionSegura.guardarRefreshToken(
                    refreshToken
                )

                RetroFistCliente.guardarTokenJwt(
                    token
                )

                // ==================================
                // 6. ACTIVAR NAVEGACIÓN
                // ==================================

                _loginState.value =
                    LoginState.Success(
                        usuario
                    )

            } catch (e: Exception) {

                RetroFistCliente.limpiarTokenJwt()

                _loginState.value =
                    LoginState.Error(
                        "Error al iniciar sesión: " +
                                (e.localizedMessage
                                    ?: "Error de conexión.")
                    )
            }
        }
    }

    // ==========================================
    // RESTABLECER ESTADO DE LA PANTALLA
    // ==========================================

    fun resetLoginState() {
        _loginState.value = LoginState.Idle
    }
}
