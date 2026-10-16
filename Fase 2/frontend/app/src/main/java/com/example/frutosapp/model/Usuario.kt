package com.example.frutosapp.model

data class Usuario(
    val idUsuario: Long? = null,
    val nombre: String,
    val apellido: String,
    val correo: String,
    val password: String,
    val tipoUsuario: String
)
