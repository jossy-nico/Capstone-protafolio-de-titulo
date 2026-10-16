package com.example.frutosapp.api

import com.example.frutosapp.model.ResennaRequest
import com.example.frutosapp.model.ResennaResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ResennaApi {

    @POST("api/resennas")
    suspend fun crearResenna(
        @Body request: ResennaRequest
    ): ResennaResponse

    @GET("api/resennas/producto/{productoId}")
    suspend fun obtenerResennasProducto(
        @Path("productoId") productoId: Long
    ): List<ResennaResponse>

    @GET("api/resennas/producto/{productoId}/promedio")
    suspend fun obtenerPromedioProducto(
        @Path("productoId") productoId: Long
    ): Double

    @GET(
        "api/resennas/cliente/{clienteId}/venta/{ventaId}/producto/{productoId}"
    )
    suspend fun obtenerResennaCompra(
        @Path("clienteId") clienteId: Long,
        @Path("ventaId") ventaId: Long,
        @Path("productoId") productoId: Long
    ): Response<ResennaResponse>

    @PUT("api/resennas/{idResenna}")
    suspend fun actualizarResenna(
        @Path("idResenna") idResenna: Long,
        @Body request: ResennaRequest
    ): ResennaResponse

    @DELETE("api/resennas/{idResenna}")
    suspend fun eliminarResenna(
        @Path("idResenna") idResenna: Long
    ): Response<Unit>
}