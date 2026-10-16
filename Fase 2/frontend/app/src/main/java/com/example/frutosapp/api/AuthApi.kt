package com.example.frutosapp.api

import com.example.frutosapp.model.LoginRequest
import com.example.frutosapp.model.Usuario
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("api/usuarios/login")
    suspend fun login(@Body request: LoginRequest): Response<Usuario>
}
