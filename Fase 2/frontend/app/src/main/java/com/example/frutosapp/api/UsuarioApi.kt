package com.example.frutosapp.api

import com.example.frutosapp.model.Usuario
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface UsuarioApi {

    @GET("api/usuarios")
    suspend fun getUsuarios(): Response<List<Usuario>>

    @GET("api/usuarios/{id}")
    suspend fun getUsuario(@Path("id") id: Long): Response<Usuario>

    @POST("api/usuarios")
    suspend fun crearUsuario(@Body usuario: Usuario): Response<Usuario>

    @PUT("api/usuarios/{id}")
    suspend fun actualizarUsuario(
        @Path("id") id: Long,
        @Body usuario: Usuario
    ): Response<Usuario>

    @DELETE("api/usuarios/{id}")
    suspend fun eliminarUsuario(@Path("id") id: Long): Response<Unit>

}