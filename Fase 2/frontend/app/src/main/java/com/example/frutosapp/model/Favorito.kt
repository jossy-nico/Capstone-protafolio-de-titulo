package com.example.frutosapp.model

data class Favorito(
    val idFavorito: Long? = null,
    val idUsuario: Long,
    val idProducto: Int,
    val fechaAgregado: String? = null
)