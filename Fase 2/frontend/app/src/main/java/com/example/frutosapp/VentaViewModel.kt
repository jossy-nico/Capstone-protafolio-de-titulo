package com.example.frutosapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frutosapp.api.RetroFistCliente
import com.example.frutosapp.model.DetalleVentaRequestApi
import com.example.frutosapp.model.DetalleVentaResponseApi
import com.example.frutosapp.model.ItemCarritoConProducto
import com.example.frutosapp.model.VentaRequestApi
import com.example.frutosapp.model.VentaResponseApi
import com.example.frutosapp.model.WebpayInicioResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VentaViewModel : ViewModel(){
    // Venta recién creada para comprobante
    private val _ventaCreada = MutableStateFlow<VentaResponseApi?>(null)
    val ventaCreada: StateFlow<VentaResponseApi?> = _ventaCreada

    // Historial de ventas del cliente
    private val _historialVentas = MutableStateFlow<List<VentaResponseApi>>(emptyList())
    val historialVentas: StateFlow<List<VentaResponseApi>> = _historialVentas

    //Venta seleccionada para comprobante
    private val _ventaSeleccionada = MutableStateFlow<VentaResponseApi?>(null)
    val ventaSeleccionada: StateFlow<VentaResponseApi?> = _ventaSeleccionada.asStateFlow()

    //Detalles de la venta (lineas de productos)
    private val _detallesVenta = MutableStateFlow<List<DetalleVentaResponseApi>>(emptyList())
    val detallesVenta: StateFlow<List<DetalleVentaResponseApi>> = _detallesVenta.asStateFlow()

    // Estados de carga y error
    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando

    private val _errorMensaje = MutableStateFlow<String?>(null)
    val errorMensaje: StateFlow<String?> = _errorMensaje

    // Transacción Webpay preparada para abrir el pago
    private val _webpayInicio =
        MutableStateFlow<WebpayInicioResponse?>(null)

    val webpayInicio: StateFlow<WebpayInicioResponse?> =
        _webpayInicio.asStateFlow()

    //CONFIRMAR COMPRA
    fun confirCompra(
        itemsVenta: List<ItemCarritoConProducto>,
        usuarioId: Long
    ) {
        viewModelScope.launch {

            if (itemsVenta.isEmpty()) {
                _errorMensaje.value =
                    "No hay productos para comprar."
                return@launch
            }

            try {

                _cargando.value = true
                _errorMensaje.value = null
                _webpayInicio.value = null

                val detalles = itemsVenta.map { item ->

                    DetalleVentaRequestApi(
                        productoId =
                            (item.producto.idProductos ?: 0).toLong(),

                        cantidad =
                            item.carritoCompra.cantidad,

                        // El backend NO confía en este precio.
                        // Calcula nuevamente usando producto-api.
                        precioUnitario =
                            item.producto.precioKilo
                    )
                }

                val request = VentaRequestApi(
                    clienteId = usuarioId,
                    detalles = detalles
                )

                // IMPORTANTE:
                // Ya NO creamos la venta directamente.
                // Primero iniciamos Webpay.
                val response =
                    RetroFistCliente.ventaApi
                        .iniciarPagoWebpay(request)

                if (response.isSuccessful) {

                    val webpay = response.body()

                    if (webpay != null) {

                        _webpayInicio.value = webpay

                    } else {

                        _errorMensaje.value =
                            "Webpay no entregó los datos de pago."
                    }

                } else {

                    _errorMensaje.value =
                        when (response.code()) {

                            400, 409 ->
                                "No fue posible iniciar el pago. " +
                                        "Verifica el stock disponible."

                            else ->
                                "Error al iniciar Webpay. Código: " +
                                        response.code()
                        }
                }

            } catch (e: Exception) {

                e.printStackTrace()

                _errorMensaje.value =
                    "Error de conexión: ${e.message}"

            } finally {

                _cargando.value = false
            }
        }
    }

    fun confirmarCompraInvitado(
        itemsVenta: List<ItemCarritoConProducto>,
        datosInvitado: InvitadoState
    ) {
        viewModelScope.launch {

            if (itemsVenta.isEmpty()) {
                _errorMensaje.value =
                    "No hay productos para comprar."
                return@launch
            }

            if (
                datosInvitado.nombre.isBlank() ||
                datosInvitado.apellido.isBlank() ||
                datosInvitado.correo.isBlank() ||
                datosInvitado.telefono.isBlank() ||
                datosInvitado.direccion.isBlank()
            ) {
                _errorMensaje.value =
                    "Los datos del invitado están incompletos."
                return@launch
            }

            try {

                _cargando.value = true
                _errorMensaje.value = null
                _webpayInicio.value = null

                val detalles = itemsVenta.map { item ->

                    DetalleVentaRequestApi(
                        productoId =
                            (item.producto.idProductos ?: 0).toLong(),

                        cantidad =
                            item.carritoCompra.cantidad,

                        precioUnitario =
                            item.producto.precioKilo
                    )
                }

                val request = VentaRequestApi(
                    clienteId = null,

                    nombreInvitado =
                        datosInvitado.nombre.trim(),

                    apellidoInvitado =
                        datosInvitado.apellido.trim(),

                    correoInvitado =
                        datosInvitado.correo.trim(),

                    telefonoInvitado =
                        datosInvitado.telefono.trim(),

                    direccionInvitado =
                        datosInvitado.direccion.trim(),

                    detalles = detalles
                )

                // Ya NO creamos la venta directamente.
                // Primero iniciamos Webpay.
                val response =
                    RetroFistCliente.ventaApi
                        .iniciarPagoWebpay(request)

                if (response.isSuccessful) {

                    val webpay = response.body()

                    if (webpay != null) {

                        _webpayInicio.value = webpay

                    } else {

                        _errorMensaje.value =
                            "Webpay no entregó los datos de pago."
                    }

                } else {

                    _errorMensaje.value =
                        when (response.code()) {

                            400, 409 ->
                                "No fue posible iniciar el pago. " +
                                        "Verifica los datos y el stock disponible."

                            else ->
                                "Error al iniciar Webpay. Código: " +
                                        response.code()
                        }
                }

            } catch (e: Exception) {

                e.printStackTrace()

                _errorMensaje.value =
                    "Error de conexión: ${e.message}"

            } finally {

                _cargando.value = false
            }
        }
    }
    fun procesarPagoWebpay(
        token: String,
        onExito: () -> Unit
    ) {
        viewModelScope.launch {

            try {

                _cargando.value = true
                _errorMensaje.value = null

                // Consultamos el pago Webpay usando el token
                val estadoResponse =
                    RetroFistCliente.ventaApi
                        .obtenerEstadoWebpay(token)

                if (!estadoResponse.isSuccessful) {
                    _errorMensaje.value =
                        "No se pudo consultar el estado del pago. Código: " +
                                estadoResponse.code()
                    return@launch
                }

                val estadoPago = estadoResponse.body()

                if (estadoPago == null) {
                    _errorMensaje.value =
                        "No se recibió información del pago."
                    return@launch
                }

                if (
                    estadoPago.estado != "AUTHORIZED" ||
                    estadoPago.ventaId == null
                ) {
                    _errorMensaje.value =
                        "El pago todavía no tiene una venta asociada."
                    return@launch
                }

                // Recuperamos la venta que Webpay acaba de crear
                val venta =
                    RetroFistCliente.ventaApi
                        .obtenerVentaPorId(estadoPago.ventaId)

                // Dejamos seleccionada la venta para el comprobante
                _ventaSeleccionada.value = venta
                _ventaCreada.value = venta

                // Ya podemos limpiar los datos temporales de Webpay
                _webpayInicio.value = null

                onExito()

            } catch (e: Exception) {

                e.printStackTrace()

                _errorMensaje.value =
                    "Error al recuperar la venta: ${e.message}"

            } finally {

                _cargando.value = false
            }
        }
    }


    //HISTORIAL
    fun cargarHistorial(usuarioId: Long) {
        viewModelScope.launch {
            try {
                _cargando.value = true
                _errorMensaje.value = null
                val response = RetroFistCliente.ventaApi.obtenerVentasCliente(usuarioId)
                if (response.isSuccessful) {
                    _historialVentas.value = response.body().orEmpty()
                    _errorMensaje.value = null
                } else {
                    _errorMensaje.value =
                        "No se pudo cargar el historial. Código: ${response.code()}"
                }
            } catch (e: Exception) {
                _errorMensaje.value = "Error de conexión: ${e.message}"
            } finally {
                _cargando.value = false
            }
        }
    }

    fun seleccionarVenta(venta: VentaResponseApi) {
        _ventaSeleccionada.value = venta
    }

    fun cargarDetallesVenta(idVenta: Long) {
        viewModelScope.launch {
            try {
                _cargando.value = true
                _errorMensaje.value = null

                val response = RetroFistCliente.ventaApi.obtenerDetallesVenta(idVenta)

                if (response.isSuccessful) {
                    _detallesVenta.value = response.body().orEmpty()
                    _errorMensaje.value = null
                } else {
                    _errorMensaje.value =
                        "No se pudieron cargar los detalles. Código: ${response.code()}"
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMensaje.value = "Error al cargar detalles de la venta: ${e.message}"
            } finally {
                _cargando.value = false
            }
        }
    }

    //para que no se dispare dos veces la navegación
    fun limpiarVentaCreada() {
        _ventaCreada.value = null
    }
    fun limpiarWebpayInicio() {
        _webpayInicio.value = null
    }
}