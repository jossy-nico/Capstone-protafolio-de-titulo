
package com.example.frutosapp

import android.app.Application

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import com.example.frutosapp.api.RetroFistCliente
import com.example.frutosapp.api.SesionSegura

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import org.json.JSONObject

data class EstadoSesion(
    val loginOk: Boolean = false,
    val userId: Long? = null,
    val restaurando: Boolean = false
)

class SesionViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val sesionSegura =
        SesionSegura(application)

    private val _sesionState =
        MutableStateFlow(
            EstadoSesion(restaurando = true)
        )

    val sesionState = _sesionState.asStateFlow()

    // Evita que una restauración antigua
    // sobrescriba un login o logout posterior.
    private var versionSesion = 0L

    init {
        restaurarSesion()
    }

    // ==========================================
    // RESTAURAR SESIÓN AL ABRIR ANDROID
    // ==========================================

    private fun restaurarSesion() {

        val versionActual = ++versionSesion

        viewModelScope.launch {

            _sesionState.value = EstadoSesion(
                restaurando = true
            )

            try {

                // 1. Recuperar refresh token cifrado.
                val refreshToken = withContext(
                    Dispatchers.IO
                ) {
                    sesionSegura.obtenerRefreshToken()
                }

                if (versionActual != versionSesion) {
                    return@launch
                }

                if (refreshToken.isNullOrBlank()) {

                    _sesionState.value = EstadoSesion()

                    return@launch
                }

                // 2. Solicitar JWT y refresh token nuevos.
                val respuesta =
                    RetroFistCliente.authApi.renovarSesion(
                        mapOf(
                            "refreshToken" to refreshToken
                        )
                    )

                if (versionActual != versionSesion) {
                    return@launch
                }

                if (!respuesta.isSuccessful) {

                    // Solo invalidar credenciales
                    // cuando el backend rechace
                    // expresamente la sesión.
                    if (
                        respuesta.code() == 400 ||
                        respuesta.code() == 401 ||
                        respuesta.code() == 403
                    ) {

                        withContext(Dispatchers.IO) {
                            sesionSegura.limpiar()
                        }

                        RetroFistCliente.limpiarTokenJwt()
                    }

                    _sesionState.value = EstadoSesion()

                    return@launch
                }

                val contenido =
                    respuesta.body()?.string()

                if (contenido.isNullOrBlank()) {
                    _sesionState.value = EstadoSesion()
                    return@launch
                }

                val json = JSONObject(contenido)

                val nuevoJwt = json.optString(
                    "token",
                    ""
                )

                val nuevoRefresh = json.optString(
                    "refreshToken",
                    ""
                )

                val usuarioJson =
                    json.optJSONObject("usuario")

                val usuarioId = usuarioJson?.optLong(
                    "idUsuario",
                    -1L
                ) ?: -1L

                val tipoUsuario = usuarioJson?.optString(
                    "tipoUsuario",
                    ""
                ) ?: ""

                if (
                    nuevoJwt.isBlank() ||
                    nuevoRefresh.isBlank() ||
                    usuarioId <= 0L ||
                    !tipoUsuario.equals(
                        "CLIENTE",
                        ignoreCase = true
                    )
                ) {

                    _sesionState.value = EstadoSesion()

                    return@launch
                }

                if (versionActual != versionSesion) {
                    return@launch
                }

                // 3. Guardar el refresh token rotado.
                withContext(Dispatchers.IO) {
                    sesionSegura.guardarRefreshToken(
                        nuevoRefresh
                    )
                }

                if (versionActual != versionSesion) {
                    return@launch
                }

                // 4. Restaurar JWT en memoria.
                RetroFistCliente.guardarTokenJwt(
                    nuevoJwt
                )

                // 5. Restaurar identidad del cliente.
                _sesionState.value = EstadoSesion(
                    loginOk = true,
                    userId = usuarioId,
                    restaurando = false
                )

            } catch (e: CancellationException) {

                throw e

            } catch (e: Exception) {

                if (versionActual == versionSesion) {

                    // Ante error de red, conservar el
                    // refresh token almacenado.
                    // Se podrá reintentar más adelante.
                    RetroFistCliente.limpiarTokenJwt()

                    _sesionState.value = EstadoSesion()
                }
            }
        }
    }

    // ==========================================
    // ACTIVAR SESIÓN DEL CLIENTE
    // ==========================================

    fun onLogin(userId: Long) {

        versionSesion++

        if (
            userId <= 0L ||
            !RetroFistCliente.existeSesionJwt()
        ) {

            _sesionState.value = EstadoSesion()

            return
        }

        _sesionState.value = EstadoSesion(
            loginOk = true,
            userId = userId,
            restaurando = false
        )
    }

    // ==========================================
    // CERRAR SESIÓN
    // ==========================================

    fun onLogout() {

        versionSesion++

        // Recuperar credencial antes de borrarla.
        val refreshToken =
            sesionSegura.obtenerRefreshToken()

        // Cerrar inmediatamente la sesión local.
        RetroFistCliente.limpiarTokenJwt()

        sesionSegura.limpiar()

        _sesionState.value = EstadoSesion()

        // Solicitar revocación al backend.
        // La navegación no debe depender
        // de que esta solicitud termine.
        if (!refreshToken.isNullOrBlank()) {

            viewModelScope.launch {

                try {

                    RetroFistCliente.authApi
                        .cerrarSesionPersistente(
                            mapOf(
                                "refreshToken" to refreshToken
                            )
                        )

                } catch (e: CancellationException) {

                    throw e

                } catch (_: Exception) {

                    // La sesión local ya se cerró.
                    // La revocación remota puede fallar
                    // por problemas de conectividad.
                }
            }
        }
    }
}
