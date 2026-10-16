package com.example.frutosapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.frutosapp.api.RetroFistCliente
import com.example.frutosapp.local.FrutosDatabase
import com.example.frutosapp.model.CarritoCompra
import com.example.frutosapp.model.CarritoCompraId
import com.example.frutosapp.model.CarritoInvitadoEntity
import com.example.frutosapp.model.ItemCarritoConProducto
import com.example.frutosapp.model.Producto
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CarritoViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val carritoInvitadoDao =
        FrutosDatabase.getDatabase(application).carritoInvitadoDao()

    private val _itemsCarrito =
        MutableStateFlow<List<ItemCarritoConProducto>>(emptyList())

    val itemsCarrito: StateFlow<List<ItemCarritoConProducto>> =
        _itemsCarrito.asStateFlow()

    private val _mensaje = MutableSharedFlow<String>()
    val mensaje = _mensaje.asSharedFlow()

    private var usuarioId: Long? = null

    init {
        cargarCarritoInvitado()
    }

    fun setUsuarioId(id: Long?) {
        usuarioId = id

        if (id != null) {
            cargarCarrito()
        } else {
            cargarCarritoInvitado()
        }
    }

    fun cargarCarrito() {
        viewModelScope.launch {
            if (usuarioId == null) {
                cargarCarritoInvitadoInterno()
            } else {
                cargarCarritoRegistradoInterno()
            }
        }
    }

    private fun cargarCarritoInvitado() {
        viewModelScope.launch {
            cargarCarritoInvitadoInterno()
        }
    }

    private suspend fun cargarCarritoRegistradoInterno() {
        val uid = usuarioId ?: return

        try {
            val listaCarrito: List<CarritoCompra> =
                RetroFistCliente.carritoApi.obtenerCarrito(uid)

            val cacheProductos = mutableMapOf<Int, Producto>()

            val itemsConProducto = listaCarrito.map { item ->
                val productoId = item.id.productoId

                val producto = cacheProductos.getOrPut(productoId) {
                    RetroFistCliente.productoApi.buscarProducto(productoId)
                }

                ItemCarritoConProducto(
                    carritoCompra = item,
                    producto = producto
                )
            }

            _itemsCarrito.value = itemsConProducto

        } catch (e: Exception) {
            e.printStackTrace()
            _mensaje.emit("Error al cargar el carrito")
        }
    }

    private suspend fun cargarCarritoInvitadoInterno() {
        try {
            val entidades = carritoInvitadoDao.obtenerTodo()
            val cacheProductos = mutableMapOf<Int, Producto>()

            val itemsConProducto = entidades.mapNotNull { entidad ->
                try {
                    val producto = cacheProductos.getOrPut(entidad.productoId) {
                        RetroFistCliente.productoApi.buscarProducto(
                            entidad.productoId
                        )
                    }

                    ItemCarritoConProducto(
                        carritoCompra = CarritoCompra(
                            id = CarritoCompraId(
                                usuarioId = 0L,
                                productoId = entidad.productoId
                            ),
                            cantidad = entidad.cantidad
                        ),
                        producto = producto
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            _itemsCarrito.value = itemsConProducto

        } catch (e: Exception) {
            e.printStackTrace()
            _mensaje.emit("Error al cargar el carrito de invitado")
        }
    }

    fun agregarAlCarrito(
        producto: Producto,
        cantidad: Int = 250
    ) {
        val productoId = producto.idProductos ?: return

        viewModelScope.launch {
            try {
                val uid = usuarioId

                if (uid != null) {
                    RetroFistCliente.carritoApi.agregarProducto(
                        usuarioId = uid,
                        productoId = productoId,
                        cantidad = cantidad
                    )

                    cargarCarritoRegistradoInterno()

                } else {
                    val itemExistente =
                        carritoInvitadoDao.obtenerPorProducto(productoId)

                    val nuevaCantidad =
                        (itemExistente?.cantidad ?: 0) + cantidad

                    carritoInvitadoDao.guardar(
                        CarritoInvitadoEntity(
                            productoId = productoId,
                            cantidad = nuevaCantidad
                        )
                    )

                    cargarCarritoInvitadoInterno()
                }

                _mensaje.emit("Producto agregado al carrito")

            } catch (e: Exception) {
                e.printStackTrace()
                _mensaje.emit("Error al agregar al carrito")
            }
        }
    }

    fun aumentarCantidad(item: ItemCarritoConProducto) {
        val productoId = item.carritoCompra.id.productoId
        val nuevaCantidad = item.carritoCompra.cantidad + 250

        viewModelScope.launch {
            try {
                val uid = usuarioId

                if (uid != null) {
                    RetroFistCliente.carritoApi.actualizarCantidad(
                        usuarioId = uid,
                        productoId = productoId,
                        cantidad = nuevaCantidad
                    )

                    cargarCarritoRegistradoInterno()

                } else {
                    carritoInvitadoDao.actualizarCantidad(
                        productoId = productoId,
                        cantidad = nuevaCantidad
                    )

                    cargarCarritoInvitadoInterno()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _mensaje.emit("Error al actualizar cantidad")
            }
        }
    }

    fun disminuirCantidad(item: ItemCarritoConProducto) {
        val productoId = item.carritoCompra.id.productoId
        val nuevaCantidad = item.carritoCompra.cantidad - 250

        viewModelScope.launch {
            try {
                val uid = usuarioId

                if (uid != null) {
                    if (nuevaCantidad > 0) {
                        RetroFistCliente.carritoApi.actualizarCantidad(
                            usuarioId = uid,
                            productoId = productoId,
                            cantidad = nuevaCantidad
                        )
                    } else {
                        RetroFistCliente.carritoApi.eliminarProducto(
                            usuarioId = uid,
                            productoId = productoId
                        )
                    }

                    cargarCarritoRegistradoInterno()

                } else {
                    if (nuevaCantidad > 0) {
                        carritoInvitadoDao.actualizarCantidad(
                            productoId = productoId,
                            cantidad = nuevaCantidad
                        )
                    } else {
                        carritoInvitadoDao.eliminar(productoId)
                    }

                    cargarCarritoInvitadoInterno()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _mensaje.emit("Error al actualizar cantidad")
            }
        }
    }

    fun eliminarItem(item: ItemCarritoConProducto) {
        val productoId = item.carritoCompra.id.productoId

        viewModelScope.launch {
            try {
                val uid = usuarioId

                if (uid != null) {
                    RetroFistCliente.carritoApi.eliminarProducto(
                        uid,
                        productoId
                    )

                    cargarCarritoRegistradoInterno()

                } else {
                    carritoInvitadoDao.eliminar(productoId)
                    cargarCarritoInvitadoInterno()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _mensaje.emit("Error al eliminar producto del carrito")
            }
        }
    }

    fun vaciarCarrito() {
        viewModelScope.launch {
            try {
                val uid = usuarioId

                if (uid != null) {
                    RetroFistCliente.carritoApi.vaciarCarrito(uid)
                    cargarCarritoRegistradoInterno()

                } else {
                    carritoInvitadoDao.vaciar()
                    cargarCarritoInvitadoInterno()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _mensaje.emit("Error al vaciar carrito")
            }
        }
    }
}