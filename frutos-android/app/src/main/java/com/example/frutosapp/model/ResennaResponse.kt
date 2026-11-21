package com.example.frutosapp.model

data class ResennaResponse(
    val idResenna: Long,
    val clienteId: Long,
    val nombreCliente: String,
    val ventaId: Long,
    val productoId: Long,
    val estrellas: Int,
    val comentario: String?,
    val imagenUrl: String?,
    val fechaCreacion: String,
    val fechaActualizacion: String?
)

data class ResennaRequest(
    val clienteId: Long,
    val ventaId: Long,
    val productoId: Long,
    val estrellas: Int,
    val comentario: String?
)