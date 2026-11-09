package com.example.frutosapp.model

data class WebpayInicioResponse(
    val buyOrder: String,
    val sessionId: String,
    val monto: Double,
    val token: String,
    val url: String
)