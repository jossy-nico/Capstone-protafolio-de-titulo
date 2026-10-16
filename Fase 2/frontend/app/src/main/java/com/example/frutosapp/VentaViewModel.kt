package com.example.frutosapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frutosapp.api.RetroFistCliente
import com.example.frutosapp.model.DetalleVentaRequestApi
import com.example.frutosapp.model.DetalleVentaResponseApi
import com.example.frutosapp.model.ItemCarritoConProducto
import com.example.frutosapp.model.VentaRequestApi
import com.example.frutosapp.model.VentaResponseApi
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

    //CONFIRMAR COMPRA
    fun confirCompra(itemsVenta: List<ItemCarritoConProducto>, usuarioId: Long) {
        viewModelScope.launch {
            if (itemsVenta.isEmpty()) {
                _errorMensaje.value = "No hay productos para comprar."
                return@launch
            }

            try {
                _cargando.value = true

                // Armar detalles para el request
                val detalles = itemsVenta.map { item ->
                    DetalleVentaRequestApi(
                        productoId = (item.producto.idProductos ?: 0).toLong(),
                        cantidad = item.carritoCompra.cantidad,
                        precioUnitario = item.producto.precioKilo
                    )
                }

                val request = VentaRequestApi(
                    clienteId = usuarioId,
                    detalles = detalles
                )

                val response = RetroFistCliente.ventaApi.crearVenta(request)

                if (response.isSuccessful) {
                    val venta = response.body()
                    _ventaCreada.value = venta
                    if (venta != null) {
                        seleccionarVenta(venta)
                    }
                } else {
                    _errorMensaje.value = when (response.code()){
                        400, 409 -> "Stock insuficiente. Otro cliente compró antes o ya no queda stock."
                        else -> "Error al crear la enta. Código: ${response.code()}"
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMensaje.value = "Error de conexión: ${e.message}"
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
                _errorMensaje.value = "No hay productos para comprar."
                return@launch
            }

            if (
                datosInvitado.nombre.isBlank() ||
                datosInvitado.apellido.isBlank() ||
                datosInvitado.correo.isBlank() ||
                datosInvitado.telefono.isBlank() ||
                datosInvitado.direccion.isBlank()
            ) {
                _errorMensaje.value = "Los datos del invitado están incompletos."
                return@launch
            }

            try {
                _cargando.value = true
                _errorMensaje.value = null

                val detalles = itemsVenta.map { item ->
                    DetalleVentaRequestApi(
                        productoId = (item.producto.idProductos ?: 0).toLong(),
                        cantidad = item.carritoCompra.cantidad,
                        precioUnitario = item.producto.precioKilo
                    )
                }

                val request = VentaRequestApi(
                    clienteId = null,
                    nombreInvitado = datosInvitado.nombre.trim(),
                    apellidoInvitado = datosInvitado.apellido.trim(),
                    correoInvitado = datosInvitado.correo.trim(),
                    telefonoInvitado = datosInvitado.telefono.trim(),
                    direccionInvitado = datosInvitado.direccion.trim(),
                    detalles = detalles
                )

                val response = RetroFistCliente.ventaApi.crearVenta(request)

                if (response.isSuccessful) {
                    val venta = response.body()

                    _ventaCreada.value = venta

                    if (venta != null) {
                        seleccionarVenta(venta)
                    }
                } else {
                    _errorMensaje.value = when (response.code()) {
                        400, 409 ->
                            "Stock insuficiente o datos de invitado inválidos."

                        else ->
                            "Error al crear la venta. Código: ${response.code()}"
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _errorMensaje.value = "Error de conexión: ${e.message}"
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
}