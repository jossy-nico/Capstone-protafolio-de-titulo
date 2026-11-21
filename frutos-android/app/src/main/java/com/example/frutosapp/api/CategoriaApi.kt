package com.example.frutosapp.api

import com.example.frutosapp.model.Categoria
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface CategoriaApi {
    @GET("api/categorias")
    suspend fun listarCategorias(): List<Categoria>

    @GET("api/categorias/{idCategoria}")
    suspend fun buscarCategoria(
        @Path("idCategoria") idCategoria: Int
    ): Categoria

    @POST("api/categorias")
    suspend fun crearCategoria(
        @Body categoria: Categoria
    ): Categoria

    @PUT("api/categorias/{idCategoria}")
    suspend fun actualizarCategoria(
        @Path("idCategoria") idCategoria: Int,
        @Body categoria: Categoria
    ): Categoria

    @DELETE("api/categorias/{idCategoria}")
    suspend fun eliminarCategoria(
        @Path("idCategoria") idCategoria: Int
    )
}