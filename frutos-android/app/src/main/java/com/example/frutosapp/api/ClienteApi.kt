
package com.example.frutosapp.api

import com.example.frutosapp.model.Cliente
import com.example.frutosapp.model.ActualizarPerfilCompletoRequest

import okhttp3.ResponseBody

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ClienteApi {

    // ==========================================
    // MÉTODOS EXISTENTES
    // ==========================================

    @GET("api/clientes")
    suspend fun listarClientes(): Response<List<Cliente>>

    @GET("api/clientes/{usuarioId}")
    suspend fun obtenerCliente(
        @Path("usuarioId") usuarioId: Long
    ): Response<Cliente>

    @POST("api/clientes")
    suspend fun crearCliente(
        @Body cliente: Cliente
    ): Response<ResponseBody>

    @PUT("api/clientes/{usuarioId}")
    suspend fun actualizarCliente(
        @Path("usuarioId") usuarioId: Long,
        @Body cliente: Cliente
    ): Response<Cliente>

    @DELETE("api/clientes/{usuarioId}")
    suspend fun eliminarCliente(
        @Path("usuarioId") usuarioId: Long
    ): Response<Unit>

    // ==========================================
    // ACTUALIZAR MI PERFIL
    // ==========================================
    // El backend obtiene el ID desde el JWT.
    // ==========================================

    @PUT("api/clientes/mi-perfil")
    suspend fun actualizarMiPerfilCompleto(
        @Body request: ActualizarPerfilCompletoRequest
    ): Response<ResponseBody>
}
