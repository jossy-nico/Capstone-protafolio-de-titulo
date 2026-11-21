
package com.example.frutosapp.api

import com.example.frutosapp.model.LoginRequest
import com.example.frutosapp.model.Usuario

import okhttp3.ResponseBody

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    // ==========================================
    // LOGIN EXISTENTE
    // ==========================================
    // Se conserva para compatibilidad.

    @POST("api/usuarios/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<Usuario>

    // ==========================================
    // LOGIN CON JWT EXISTENTE
    // ==========================================
    // Se conserva para no afectar el flujo
    // de autenticación actual.

    @POST("api/usuarios/login-token")
    suspend fun loginJwt(
        @Body request: LoginRequest
    ): Response<ResponseBody>

    // ==========================================
    // SESIÓN PERSISTENTE: INICIAR SESIÓN
    // ==========================================
    //
    // Devuelve:
    // - token: JWT de acceso.
    // - tokenType: Bearer.
    // - expiresIn: 900 segundos.
    // - refreshToken: credencial de renovación.
    // - usuario: información del cliente.
    //
    // ResponseBody conserva el patrón actual
    // de lectura JSON de la aplicación.

    @POST("api/usuarios/login-token-persistente")
    suspend fun loginPersistente(
        @Body request: LoginRequest
    ): Response<ResponseBody>

    // ==========================================
    // SESIÓN PERSISTENTE: RENOVAR TOKEN
    // ==========================================
    //
    // Recibe el refresh token anterior.
    // Devuelve un JWT y refresh token nuevos.

    @POST("api/usuarios/refresh-token")
    suspend fun renovarSesion(
        @Body request: Map<String, String>
    ): Response<ResponseBody>

    // ==========================================
    // SESIÓN PERSISTENTE: CERRAR SESIÓN
    // ==========================================
    //
    // Revoca el refresh token presentado.

    @POST("api/usuarios/logout-token")
    suspend fun cerrarSesionPersistente(
        @Body request: Map<String, String>
    ): Response<ResponseBody>
}
