package com.example.frutosapp.api

import com.example.frutosapp.model.DetalleVentaResponseApi
import com.example.frutosapp.model.VentaRequestApi
import com.example.frutosapp.model.VentaResponseApi
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface VentaApi {
    @POST("api/ventas/crear")
    suspend fun crearVenta(
        @Body request: VentaRequestApi
    ): Response<VentaResponseApi>

    //Historial por cliente
    @GET("api/ventas/cliente/{clienteId}")
    suspend fun obtenerVentasCliente(
        @Path("clienteId") clienteId: Long
    ): Response<List<VentaResponseApi>>

    //venta por id
    @GET("api/ventas/{id}")
    suspend fun obtenerVentaPorId(
        @Path("id") idVenta: Long
    ): VentaResponseApi

    //Detalles de una venta
    @GET("api/ventas/{idVenta}/detalles")
    suspend fun obtenerDetallesVenta(
        @Path("idVenta") idVenta: Long
    ): Response<List<DetalleVentaResponseApi>>
}