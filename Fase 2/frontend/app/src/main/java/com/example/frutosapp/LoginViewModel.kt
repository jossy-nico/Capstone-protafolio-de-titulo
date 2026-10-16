package com.example.frutosapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frutosapp.api.RetroFistCliente
import com.example.frutosapp.model.LoginRequest
import com.example.frutosapp.model.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val usuario: Usuario) : LoginState()
    data class Error(val mensaje: String) : LoginState()
}

class LoginViewModel : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState = _loginState.asStateFlow()

    fun iniciarSesion(correo: String, password: String) {
        _loginState.value = LoginState.Loading

        viewModelScope.launch {
            try {
                val response = RetroFistCliente.authApi.login(
                    LoginRequest(
                        correo = correo.trim(),
                        password = password.trim()
                    )
                )

                if (response.isSuccessful) {
                    response.body()?.let { usuario ->
                        _loginState.value = LoginState.Success(usuario)
                    } ?: run {
                        _loginState.value = LoginState.Error("Respuesta vacía del servidor")
                    }
                } else {
                    _loginState.value = LoginState.Error("Credenciales inválidas")
                }

            } catch (e: Exception) {
                _loginState.value = LoginState.Error("Error de conexión: ${e.localizedMessage}")
            }
        }
    }

    fun resetLoginState() {
        _loginState.value = LoginState.Idle
    }
}
