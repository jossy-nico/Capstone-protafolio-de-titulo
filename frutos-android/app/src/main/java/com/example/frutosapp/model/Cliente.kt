package com.example.frutosapp.model

data class Cliente(
    val usuarioId: Long? = null,
    val usuario: Usuario? = null,
    val telefono: String,
    val direccion: String
) {
}