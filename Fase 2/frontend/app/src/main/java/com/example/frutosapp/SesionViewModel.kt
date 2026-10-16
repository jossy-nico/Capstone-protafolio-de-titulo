package com.example.frutosapp

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EstadoSesion(
    val loginOk: Boolean = false,
    val userId: Long? = null
)

class SesionViewModel : ViewModel() {

    private val _sesionState = MutableStateFlow(EstadoSesion())
    val sesionState = _sesionState.asStateFlow()

    fun onLogin(userId: Long) {
        _sesionState.value = EstadoSesion(
            loginOk = true,
            userId = userId
        )
    }

    fun onLogout() {
        _sesionState.value = EstadoSesion()
    }
}