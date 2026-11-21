
package com.example.frutosapp.api

import com.example.frutosapp.model.DetalleVentaResponseApi
import com.example.frutosapp.model.VentaRequestApi
import com.example.frutosapp.model.VentaResponseApi
import com.example.frutosapp.model.WebpayInicioResponse
import com.example.frutosapp.model.WebpayEstadoResponse

import okhttp3.ResponseBody

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface VentaApi {

    // ==========================================
    // CREAR VENTA
    // ==========================================
    // Endpoint administrativo existente.
    // Se conserva para mantener compatibilidad.

    @POST("api/ventas/crear")
    suspend fun crearVenta(
        @Body request: VentaRequestApi
    ): Response<VentaResponseApi>

    // ==========================================
    // HISTORIAL DE COMPRAS
    // ==========================================
    // Endpoint existente.
    // Actualmente requiere autorización ADMIN.

    @GET("api/ventas/cliente/{clienteId}")
    suspend fun obtenerVentasCliente(
        @Path("clienteId") clienteId: Long
    ): Response<List<VentaResponseApi>>

    // ==========================================
    // OBTENER VENTA POR ID
    // ==========================================
    // Endpoint administrativo existente.

    @GET("api/ventas/{id}")
    suspend fun obtenerVentaPorId(
        @Path("id") idVenta: Long
    ): VentaResponseApi

    // ==========================================
    // DETALLES DE UNA VENTA
    // ==========================================
    // Endpoint administrativo existente.

    @GET("api/ventas/{idVenta}/detalles")
    suspend fun obtenerDetallesVenta(
        @Path("idVenta") idVenta: Long
    ): Response<List<DetalleVentaResponseApi>>

    // ==========================================
    // COMPROBANTE PDF ADMINISTRATIVO
    // ==========================================
    // Se conserva para no afectar integraciones
    // existentes.
    //
    // Requiere autorización ADMIN.

    @GET("api/ventas/{ventaId}/comprobante/pdf")
    suspend fun descargarComprobante(
        @Path("ventaId") ventaId: Long
    ): Response<ResponseBody>

    // ==========================================
    // INICIAR PAGO WEBPAY
    // ==========================================

    @POST("api/webpay/iniciar")
    suspend fun iniciarPagoWebpay(
        @Body request: VentaRequestApi
    ): Response<WebpayInicioResponse>

    // ==========================================
    // CONSULTAR ESTADO WEBPAY
    // ==========================================

    @GET("api/webpay/estado/{token}")
    suspend fun obtenerEstadoWebpay(
        @Path("token") token: String
    ): Response<WebpayEstadoResponse>

    // ==========================================
    // RECUPERAR COMPRA AUTORIZADA
    // ==========================================
    // Utiliza el token original de Webpay.
    // No requiere JWT ADMIN.
    //
    // ResponseBody permite recibir el JSON sin
    // crear otro modelo Kotlin por ahora.

    @POST("api/webpay/compra/recuperar")
    suspend fun recuperarCompraWebpay(
        @Body request: Map<String, String>
    ): Response<ResponseBody>

    // ==========================================
    // DESCARGAR PDF - COMPRA INVITADO
    // ==========================================
    // Utiliza el mismo endpoint que ya funciona
    // en Postman.
    //
    // El backend verifica:
    // - Token de la transacción.
    // - Pago AUTHORIZED.
    // - Venta asociada.
    // - Ventana de acceso inmediato.

    @POST("api/webpay/compra/comprobante/pdf")
    suspend fun descargarComprobanteInvitado(
        @Body request: Map<String, String>
    ): Response<ResponseBody>


    // ==========================================
    // HU-32: HISTORIAL DEL CLIENTE AUTENTICADO
    // ==========================================
    // El backend obtiene el usuario desde el JWT.
    // No se envía clienteId en la URL.

    @GET("api/ventas/mis-compras")
    suspend fun obtenerMisCompras(): Response<List<VentaResponseApi>>


    // ==========================================
    // CLIENTE JWT - DETALLES DE MI COMPRA
    // ==========================================
    // El backend verifica que la venta pertenezca
    // al usuario autenticado mediante el JWT.

    @GET("api/ventas/mis-compras/{idVenta}/detalles")
    suspend fun obtenerDetallesMiCompra(
        @Path("idVenta") idVenta: Long
    ): Response<List<DetalleVentaResponseApi>>


    // ==========================================
    // HU-32: PDF DE MI COMPRA CON JWT
    // ==========================================
    //
    // Exclusivo para clientes registrados.
    //
    // El backend verifica:
    // - JWT válido.
    // - Rol CLIENTE.
    // - Que la venta pertenezca al usuario.
    //
    // No reemplaza la descarga de invitados
    // ni la descarga administrativa.
    // ==========================================

    @GET("api/ventas/mis-compras/{ventaId}/comprobante/pdf")
    suspend fun descargarComprobanteMiCompra(
        @Path("ventaId") ventaId: Long
    ): Response<ResponseBody>


    // ==========================================
    // HU-32: SEGUIMIENTO DEL DESPACHO CON JWT
    // ==========================================
    //
    // Permite consultar el estado de una compra
    // perteneciente al cliente autenticado.
    //
    // El backend verifica:
    // - JWT válido.
    // - Rol CLIENTE.
    // - Propiedad de la venta.
    //
    // No modifica estados ni datos del despacho.
    // ==========================================

    @GET("api/ventas/mis-compras/{idVenta}/seguimiento")
    suspend fun obtenerSeguimientoMiCompra(
        @Path("idVenta") idVenta: Long
    ): Response<ResponseBody>


    // ==========================================
    // HU-33: SEGUIMIENTO DE COMPRADORES INVITADOS
    // ==========================================
    //
    // No requiere JWT.
    //
    // El comprador debe proporcionar:
    // - Número de orden de Webpay.
    // - Código privado de seguimiento.
    //
    // El backend comprueba que ambos datos
    // correspondan a la misma compra autorizada.
    //
    // La respuesta contiene solamente datos
    // logísticos y no permite modificar pedidos.
    // ==========================================

    @POST("api/ventas/invitados/seguimiento")
    suspend fun obtenerSeguimientoInvitado(
        @Body request: Map<String, String>
    ): Response<ResponseBody>



}
