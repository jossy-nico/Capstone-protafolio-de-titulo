package com.example.frutosapp.model


// ----- DETALLE -----

data class DetalleVentaIdApi(
    val ventaId: Long,
    val productoId: Long
)

data class DetalleVentaRequestApi(
    val productoId: Long,
    val cantidad: Int,
    val precioUnitario: Double
)

data class DetalleVentaResponseApi(
    val id: DetalleVentaIdApi,
    val cantidad: Int,
    val precioUnitario: Double
)

// ----- VENTA -----

data class VentaRequestApi(
    val clienteId: Long? = null,
    val nombreInvitado: String? = null,
    val apellidoInvitado: String? = null,
    val correoInvitado: String? = null,
    val telefonoInvitado: String? = null,
    val direccionInvitado: String? = null,
    val detalles: List<DetalleVentaRequestApi>
)

data class VentaResponseApi(
    val idVenta: Long,
    val clienteId: Long?,
    val nombreInvitado: String? = null,
    val apellidoInvitado: String? = null,
    val correoInvitado: String? = null,
    val telefonoInvitado: String? = null,
    val direccionInvitado: String? = null,
    val fechaVenta: String?,
    val totalVenta: Double
)