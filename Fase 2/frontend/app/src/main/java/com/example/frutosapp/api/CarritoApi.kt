package com.example.frutosapp.api

import com.example.frutosapp.model.CarritoCompra
import okhttp3.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface CarritoApi {

    //obtener todos los items del carrito de un usuario
    @GET("api/carrito/{usuarioId}")
    suspend fun obtenerCarrito(@Path("usuarioId") usuarioId: Long): List<CarritoCompra>

    //agregar producto al carrito
    @POST("api/carrito/agregar")
    suspend fun agregarProducto(
        @Query("usuarioId") usuarioId: Long,
        @Query("productoId") productoId: Int,
        @Query("cantidad") cantidad: Int
    ): CarritoCompra

    //Actualizar cantidad de un producto
    @PUT("api/carrito/actualizar")
    suspend fun actualizarCantidad(
        @Query("usuarioId") usuarioId: Long,
        @Query("productoId") productoId: Int,
        @Query("cantidad") cantidad: Int
    ): CarritoCompra

    //Eliminar un producto del carrito
    @DELETE("api/carrito/eliminar")
    suspend fun eliminarProducto(
        @Query("usuarioId") usuarioId: Long,
        @Query("productoId") productoId: Int
    )

    //vaciar carrito completo de un usuario
    @DELETE("api/carrito/vaciar/{usuarioId}")
    suspend fun vaciarCarrito(
        @Path("usuarioId") usuarioId: Long
    )
}