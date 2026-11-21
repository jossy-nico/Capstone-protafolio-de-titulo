
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

import org.json.JSONObject

class VentaViewModel : ViewModel() {

    // ==========================================
    // VENTA CREADA / SELECCIONADA
    // ==========================================

    private val _ventaCreada =
        MutableStateFlow<VentaResponseApi?>(null)

    val ventaCreada: StateFlow<VentaResponseApi?> =
        _ventaCreada.asStateFlow()

    private val _ventaSeleccionada =
        MutableStateFlow<VentaResponseApi?>(null)

    val ventaSeleccionada: StateFlow<VentaResponseApi?> =
        _ventaSeleccionada.asStateFlow()

    // ==========================================
    // HISTORIAL
    // ==========================================

    private val _historialVentas =
        MutableStateFlow<List<VentaResponseApi>>(emptyList())

    val historialVentas: StateFlow<List<VentaResponseApi>> =
        _historialVentas.asStateFlow()

    // ==========================================
    // DETALLES DE VENTA
    // ==========================================

    private val _detallesVenta =
        MutableStateFlow<List<DetalleVentaResponseApi>>(
            emptyList()
        )

    val detallesVenta:
            StateFlow<List<DetalleVentaResponseApi>> =
        _detallesVenta.asStateFlow()


    // ==========================================
    // HU-32: SEGUIMIENTO DEL DESPACHO
    // ==========================================
    //
    // El backend entrega el estado real del
    // pedido mediante un JSON.
    //
    // Se mantiene separado del historial,
    // Webpay y comprobantes.
    // ==========================================

    private val _seguimientoPedidoJson =
        MutableStateFlow<String?>(null)

    val seguimientoPedidoJson: StateFlow<String?> =
        _seguimientoPedidoJson.asStateFlow()

    private val _cargandoSeguimiento =
        MutableStateFlow(false)

    val cargandoSeguimiento: StateFlow<Boolean> =
        _cargandoSeguimiento.asStateFlow()

    private val _errorSeguimiento =
        MutableStateFlow<String?>(null)

    val errorSeguimiento: StateFlow<String?> =
        _errorSeguimiento.asStateFlow()


    // ==========================================
    // HU-33: SEGUIMIENTO DE COMPRAS INVITADAS
    // ==========================================
    //
    // Independiente del seguimiento con JWT.
    // No modifica la información de la venta.
    // ==========================================

    private val _seguimientoInvitadoJson =
        MutableStateFlow<String?>(null)

    val seguimientoInvitadoJson: StateFlow<String?> =
        _seguimientoInvitadoJson.asStateFlow()

    private val _cargandoSeguimientoInvitado =
        MutableStateFlow(false)

    val cargandoSeguimientoInvitado: StateFlow<Boolean> =
        _cargandoSeguimientoInvitado.asStateFlow()

    private val _errorSeguimientoInvitado =
        MutableStateFlow<String?>(null)

    val errorSeguimientoInvitado: StateFlow<String?> =
        _errorSeguimientoInvitado.asStateFlow()

    // ==========================================
    // ESTADOS GENERALES
    // ==========================================

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> =
        _cargando.asStateFlow()

    private val _errorMensaje =
        MutableStateFlow<String?>(null)

    val errorMensaje: StateFlow<String?> =
        _errorMensaje.asStateFlow()

    // ==========================================
    // WEBPAY - INICIO
    // ==========================================

    private val _webpayInicio =
        MutableStateFlow<WebpayInicioResponse?>(null)

    val webpayInicio: StateFlow<WebpayInicioResponse?> =
        _webpayInicio.asStateFlow()

    // ==========================================
    // WEBPAY - COMPRA CONFIRMADA
    // ==========================================
    //
    // El backend de recuperación entrega un JSON
    // reducido, no la entidad Venta completa.
    //
    // Por eso lo mantenemos separado del modelo
    // VentaResponseApi administrativo.
    // ==========================================

    private val _compraWebpayRecuperada =
        MutableStateFlow<String?>(null)

    val compraWebpayRecuperada: StateFlow<String?> =
        _compraWebpayRecuperada.asStateFlow()

    private val _ventaIdWebpayConfirmada =
        MutableStateFlow<Long?>(null)

    val ventaIdWebpayConfirmada: StateFlow<Long?> =
        _ventaIdWebpayConfirmada.asStateFlow()

    // ==========================================
    // TOKEN TEMPORAL PARA PDF
    // ==========================================
    //
    // Se conserva únicamente en memoria.
    // No guardarlo en logs ni preferencias.
    //
    // Este token permite recuperar el PDF
    // dentro de la ventana temporal del backend.
    // ==========================================

    private val _tokenComprobanteWebpay =
        MutableStateFlow<String?>(null)

    val tokenComprobanteWebpay: StateFlow<String?> =
        _tokenComprobanteWebpay.asStateFlow()

    // ==========================================
    // CONFIRMAR COMPRA CLIENTE
    // ==========================================

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

                limpiarDatosCompraWebpay()

                val detalles = itemsVenta.map { item ->

                    DetalleVentaRequestApi(
                        productoId =
                            (item.producto.idProductos ?: 0)
                                .toLong(),

                        cantidad =
                            item.carritoCompra.cantidad,

                        precioUnitario =
                            item.producto.precioKilo
                    )
                }

                val request = VentaRequestApi(
                    clienteId = usuarioId,
                    detalles = detalles
                )

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

    // ==========================================
    // CONFIRMAR COMPRA INVITADO
    // ==========================================

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

                limpiarDatosCompraWebpay()

                val detalles = itemsVenta.map { item ->

                    DetalleVentaRequestApi(
                        productoId =
                            (item.producto.idProductos ?: 0)
                                .toLong(),

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

    // ==========================================
    // PROCESAR RESULTADO WEBPAY
    // ==========================================
    //
    // ANTES:
    // GET /api/ventas/{id}
    // -> HTTP 401 para invitados.
    //
    // AHORA:
    // POST /api/webpay/compra/recuperar
    // -> Validación mediante token de Webpay.
    //
    // No crea otra venta.
    // No confirma nuevamente la transacción.
    // ==========================================

    fun procesarPagoWebpay(
        token: String,
        onExito: () -> Unit
    ) {
        viewModelScope.launch {

            try {

                _cargando.value = true
                _errorMensaje.value = null

                if (token.isBlank()) {

                    _errorMensaje.value =
                        "No se recibió el token del pago."

                    return@launch
                }

                // 1. Consultar el estado del pago.

                val estadoResponse =
                    RetroFistCliente.ventaApi
                        .obtenerEstadoWebpay(token)

                if (!estadoResponse.isSuccessful) {

                    _errorMensaje.value =
                        "No se pudo consultar el estado del pago. " +
                                "Código: ${estadoResponse.code()}"

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

                // 2. Recuperar la compra usando
                // el nuevo endpoint público controlado.

                val compraResponse =
                    RetroFistCliente.ventaApi
                        .recuperarCompraWebpay(
                            mapOf("token" to token)
                        )

                if (!compraResponse.isSuccessful) {

                    _errorMensaje.value =
                        when (compraResponse.code()) {

                            410 ->
                                "El pago está autorizado, pero " +
                                        "finalizó el plazo de recuperación " +
                                        "inmediata del comprobante."

                            409 ->
                                "La compra todavía está siendo procesada."

                            404 ->
                                "No se encontró la compra asociada al pago."

                            else ->
                                "No se pudo recuperar la compra. " +
                                        "Código: ${compraResponse.code()}"
                        }

                    return@launch
                }

                val contenido =
                    compraResponse.body()?.string()

                if (contenido.isNullOrBlank()) {

                    _errorMensaje.value =
                        "La recuperación de la compra no entregó datos."

                    return@launch
                }

                // 3. Validar la respuesta del backend.

                val json = JSONObject(contenido)

                val ventaIdRecuperada =
                    json.optLong("ventaId", -1L)

                val estadoRecuperado =
                    json.optString("estadoPago", "")

                if (
                    ventaIdRecuperada <= 0L ||
                    estadoRecuperado != "AUTHORIZED"
                ) {

                    _errorMensaje.value =
                        "La compra recuperada no está confirmada."

                    return@launch
                }

                // Evita asociar accidentalmente
                // una respuesta con otra venta.

                if (
                    ventaIdRecuperada !=
                    estadoPago.ventaId.toLong()
                ) {

                    _errorMensaje.value =
                        "La venta recuperada no coincide " +
                                "con la transacción Webpay."

                    return@launch
                }

                // 4. Conservar los datos reales
                // del nuevo contrato de recuperación.

                _compraWebpayRecuperada.value =
                    contenido

                _ventaIdWebpayConfirmada.value =
                    ventaIdRecuperada

                // 5. Conservar el token temporal
                // para descargar el PDF.

                _tokenComprobanteWebpay.value =
                    token

                // 6. No consultar el endpoint ADMIN.
                // No inventar una VentaResponseApi
                // con campos que el backend no entrega.

                _ventaSeleccionada.value = null
                _ventaCreada.value = null
                _detallesVenta.value = emptyList()

                // 7. Limpiar inicio del pago.
                // La compra ya fue recuperada.

                _webpayInicio.value = null

                // 8. Avisar a la navegación
                // que la recuperación terminó.

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

    // ==========================================
    // HISTORIAL EXISTENTE
    // ==========================================
    //
    // Se conserva el comportamiento actual.
    // Este endpoint administrativo todavía
    // deberá adaptarse al JWT del cliente.
    // ==========================================


    // ==========================================
    // HU-32: HISTORIAL CON JWT
    // ==========================================

    @Suppress("UNUSED_PARAMETER")
    fun cargarHistorial(usuarioId: Long) {

        viewModelScope.launch {

            _cargando.value = true
            _errorMensaje.value = null

            try {

                if (!RetroFistCliente.existeSesionJwt()) {

                    _historialVentas.value = emptyList()

                    _errorMensaje.value =
                        "Debes iniciar sesión para consultar tus compras."

                    return@launch
                }

                val response =
                    RetroFistCliente.ventaApi.obtenerMisCompras()

                if (response.isSuccessful) {

                    _historialVentas.value =
                        response.body().orEmpty()

                    _errorMensaje.value = null

                } else {

                    _historialVentas.value = emptyList()

                    _errorMensaje.value =
                        when (response.code()) {

                            401 ->
                                "Tu sesión ha expirado. Inicia sesión nuevamente."

                            403 ->
                                "No tienes autorización para consultar tus compras."

                            else ->
                                "No se pudo cargar el historial. " +
                                        "Código: ${response.code()}"
                        }
                }

            } catch (e: Exception) {

                _historialVentas.value = emptyList()

                _errorMensaje.value =
                    "Error al cargar el historial: ${e.localizedMessage}"

            } finally {

                _cargando.value = false
            }
        }
    }


    // ==========================================
    // SELECCIONAR VENTA EXISTENTE
    // ==========================================

    fun seleccionarVenta(venta: VentaResponseApi) {

        _ventaSeleccionada.value = venta
    }

    // ==========================================
    // CARGAR DETALLES EXISTENTES
    // ==========================================
    //
    // Este endpoint sigue siendo administrativo.
    // No se utiliza durante la recuperación
    // de la compra Webpay invitado.
    // ==========================================


    // ==========================================
    // CLIENTE JWT - DETALLE DE COMPRA PROPIA
    // ==========================================
    //
    // No consulta /api/ventas/{id}/detalles
    // porque esa ruta está reservada para ADMIN.
    //
    // El backend verifica la propiedad de la venta.
    // ==========================================

    fun cargarDetallesVenta(idVenta: Long) {

        viewModelScope.launch {

            _cargando.value = true
            _errorMensaje.value = null

            // Evita mostrar detalles de otra compra
            // mientras se carga la seleccionada.
            _detallesVenta.value = emptyList()

            try {

                if (!RetroFistCliente.existeSesionJwt()) {

                    _errorMensaje.value =
                        "Debes iniciar sesión para consultar " +
                                "los detalles de tu compra."

                    return@launch
                }

                if (idVenta <= 0L) {

                    _errorMensaje.value =
                        "El identificador de la venta no es válido."

                    return@launch
                }

                val response =
                    RetroFistCliente.ventaApi
                        .obtenerDetallesMiCompra(idVenta)

                if (response.isSuccessful) {

                    _detallesVenta.value =
                        response.body().orEmpty()

                    _errorMensaje.value = null

                } else {

                    _errorMensaje.value =
                        when (response.code()) {

                            401 ->
                                "Tu sesión ha expirado. " +
                                        "Inicia sesión nuevamente."

                            403 ->
                                "No tienes autorización " +
                                        "para consultar esta compra."

                            404 ->
                                "La compra no fue encontrada " +
                                        "o no pertenece a tu cuenta."

                            else ->
                                "No se pudieron cargar los detalles. " +
                                        "Código HTTP: ${response.code()}"
                        }
                }

            } catch (e: Exception) {

                _errorMensaje.value =
                    "Error al cargar los detalles: " +
                            (e.localizedMessage ?: "Error de conexión.")

            } finally {

                _cargando.value = false
            }
        }
    }

    // ==========================================
    // HU-32: CONSULTAR SEGUIMIENTO DEL DESPACHO
    // ==========================================
    //
    // Solo consulta pedidos del cliente
    // autenticado mediante JWT.
    //
    // El backend comprueba la propiedad
    // de la venta.
    //
    // No cambia estados ni modifica pedidos.
    // ==========================================

    fun cargarSeguimientoPedido(idVenta: Long) {

        viewModelScope.launch {

            _cargandoSeguimiento.value = true
            _errorSeguimiento.value = null
            _seguimientoPedidoJson.value = null

            try {

                if (!RetroFistCliente.existeSesionJwt()) {

                    _errorSeguimiento.value =
                        "Debes iniciar sesión para consultar " +
                                "el seguimiento de tu pedido."

                    return@launch
                }

                if (idVenta <= 0L) {

                    _errorSeguimiento.value =
                        "El identificador del pedido no es válido."

                    return@launch
                }

                val response =
                    RetroFistCliente.ventaApi
                        .obtenerSeguimientoMiCompra(idVenta)

                if (response.isSuccessful) {

                    val contenido =
                        response.body()?.string()

                    if (contenido.isNullOrBlank()) {

                        _errorSeguimiento.value =
                            "El servidor no entregó información " +
                                    "del seguimiento."

                        return@launch
                    }

                    val json = JSONObject(contenido)

                    val ventaRecibida =
                        json.optLong("ventaId", -1L)

                    val estadoActual =
                        json.optString("estadoActual", "")

                    if (ventaRecibida != idVenta ||
                        estadoActual.isBlank()) {

                        _errorSeguimiento.value =
                            "La información del seguimiento " +
                                    "no corresponde al pedido."

                        return@launch
                    }

                    _seguimientoPedidoJson.value =
                        contenido

                } else {

                    _errorSeguimiento.value =
                        when (response.code()) {

                            401 ->
                                "Tu sesión ha expirado. " +
                                        "Inicia sesión nuevamente."

                            403 ->
                                "No tienes autorización " +
                                        "para consultar este seguimiento."

                            404 ->
                                "El pedido no fue encontrado " +
                                        "o no pertenece a tu cuenta."

                            else ->
                                "No se pudo cargar el seguimiento. " +
                                        "Código HTTP: ${response.code()}"
                        }
                }

            } catch (e: Exception) {

                _errorSeguimiento.value =
                    "Error al consultar el seguimiento: " +
                            (e.localizedMessage
                                ?: "Error de conexión.")

            } finally {

                _cargandoSeguimiento.value = false
            }
        }
    }

    // ==========================================
    // HU-32: LIMPIAR SEGUIMIENTO
    // ==========================================

    fun limpiarSeguimientoPedido() {

        _seguimientoPedidoJson.value = null
        _errorSeguimiento.value = null
        _cargandoSeguimiento.value = false
    }

    // ==========================================
    // HU-33: CONSULTAR PEDIDO COMO INVITADO
    // ==========================================
    //
    // Requiere:
    // - Número de orden Webpay.
    // - Código privado de seguimiento.
    //
    // No necesita JWT.
    // No modifica pedidos ni estados.
    // ==========================================

    fun cargarSeguimientoInvitado(
        numeroOrden: String,
        codigoSeguimiento: String
    ) {

        viewModelScope.launch {

            _cargandoSeguimientoInvitado.value = true
            _errorSeguimientoInvitado.value = null
            _seguimientoInvitadoJson.value = null

            try {

                val ordenLimpia = numeroOrden.trim()
                val codigoLimpio = codigoSeguimiento.trim()

                // Validar número de orden.
                if (ordenLimpia.isBlank() ||
                    ordenLimpia.length > 50) {

                    _errorSeguimientoInvitado.value =
                        "Ingresa un número de orden válido."

                    return@launch
                }

                // Validar código privado.
                if (!codigoLimpio.matches(
                        Regex("NC-[A-Za-z0-9_-]{32}")
                    )) {

                    _errorSeguimientoInvitado.value =
                        "Ingresa un código de seguimiento válido."

                    return@launch
                }

                // Consultar backend.
                val response =
                    RetroFistCliente.ventaApi
                        .obtenerSeguimientoInvitado(
                            mapOf(
                                "numeroOrden" to ordenLimpia,
                                "codigoSeguimiento" to codigoLimpio
                            )
                        )

                if (response.isSuccessful) {

                    val contenido = response.body()?.string()

                    if (contenido.isNullOrBlank()) {

                        _errorSeguimientoInvitado.value =
                            "El servidor no entregó información " +
                                    "del seguimiento."

                        return@launch
                    }

                    val json = JSONObject(contenido)

                    val ventaId = json.optLong("ventaId", -1L)
                    val estadoActual =
                        json.optString("estadoActual", "")

                    // Validar estructura de respuesta.
                    if (ventaId <= 0L ||
                        estadoActual.isBlank()) {

                        _errorSeguimientoInvitado.value =
                            "La respuesta del servidor " +
                                    "no contiene un seguimiento válido."

                        return@launch
                    }

                    _seguimientoInvitadoJson.value = contenido

                } else {

                    _errorSeguimientoInvitado.value =
                        when (response.code()) {

                            400 ->
                                "Debes ingresar el número de orden " +
                                        "y el código privado."

                            404 ->
                                "No encontramos un pedido con " +
                                        "los datos ingresados. " +
                                        "Verifica ambos valores."

                            429 ->
                                "Demasiados intentos. " +
                                        "Intenta nuevamente más tarde."

                            else ->
                                "No fue posible consultar " +
                                        "el seguimiento del pedido. " +
                                        "Código HTTP: ${response.code()}"
                        }
                }

            } catch (e: Exception) {

                // No mostrar ni registrar códigos privados.
                _errorSeguimientoInvitado.value =
                    "No fue posible conectar con el servidor. " +
                            "Verifica tu conexión e intenta nuevamente."

            } finally {

                _cargandoSeguimientoInvitado.value = false
            }
        }
    }

    // ==========================================
    // HU-33: LIMPIAR CONSULTA DE INVITADOS
    // ==========================================

    fun limpiarSeguimientoInvitado() {

        _seguimientoInvitadoJson.value = null
        _errorSeguimientoInvitado.value = null
        _cargandoSeguimientoInvitado.value = false
    }




    // ==========================================
    // LIMPIEZA DE ESTADOS
    // ==========================================

    fun limpiarVentaCreada() {
        _ventaCreada.value = null
    }

    fun limpiarWebpayInicio() {
        _webpayInicio.value = null
    }

    // No llamar durante la descarga del PDF.
    // Se utiliza solamente al iniciar
    // una compra diferente.

    private fun limpiarDatosCompraWebpay() {

        _webpayInicio.value = null

        _compraWebpayRecuperada.value = null
        _ventaIdWebpayConfirmada.value = null
        _tokenComprobanteWebpay.value = null

        _ventaSeleccionada.value = null
        _ventaCreada.value = null

        _detallesVenta.value = emptyList()
    }
}
