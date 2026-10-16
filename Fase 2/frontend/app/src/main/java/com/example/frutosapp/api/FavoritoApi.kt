package com.example.frutosapp.api

import com.example.frutosapp.model.Favorito
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface FavoritoApi {

    @POST("api/favoritos/{idUsuario}/{idProducto}")
    suspend fun agregarFavorito(
        @Path("idUsuario") idUsuario: Long,
        @Path("idProducto") idProducto: Int
    ): Response<Favorito>

    @GET("api/favoritos/usuario/{idUsuario}")
    suspend fun getFavoritos(
        @Path("idUsuario") idUsuario: Long
    ): Response<List<Favorito>>

    @GET("api/favoritos/usuario/{idUsuario}/producto/{idProducto}")
    suspend fun esFavorito(
        @Path("idUsuario") idUsuario: Long,
        @Path("idProducto") idProducto: Int
    ): Response<Map<String, Boolean>>

    @DELETE("api/favoritos/{idUsuario}/{idProducto}")
    suspend fun eliminarFavorito(
        @Path("idUsuario") idUsuario: Long,
        @Path("idProducto") idProducto: Int
    ): Response<Unit>
}