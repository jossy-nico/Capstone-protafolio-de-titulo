package com.example.frutosapp.model

data class WebpayEstadoResponse(
    val estado: String,
    val ventaId: Long?,
    val buyOrder: String,
    val monto: Double
)