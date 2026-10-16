package com.example.frutosapp.api

import com.example.frutosapp.model.Producto
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

interface ProductoApi {
    @GET("api/productos")
    suspend fun listarProductos(): List<Producto>

    @GET("api/productos/{idProductos}")
    suspend fun buscarProducto(
        @Path("idProductos") idProductos: Int
    ): Producto

    @POST("api/productos")
    suspend fun crearProducto(
        @Body producto: Producto
    ): Producto

    @PUT("api/productos/{idProductos}")
    suspend fun actualizarProducto(
        @Path("idProductos") idProductos: Int,
        @Body producto: Producto
    ): Producto

    @DELETE("api/productos/{idProductos}")
    suspend fun eliminarProducto(
        @Path("idProductos") idProductos: Int
    )

    @Multipart
    @POST("api/productos/{idProductos}/imagen")
    suspend fun subirImagenProducto(
        @Path("idProductos") idProductos: Int,
        @Part file: MultipartBody.Part
    ): Producto
}