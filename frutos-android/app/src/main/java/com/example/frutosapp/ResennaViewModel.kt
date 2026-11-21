package com.example.frutosapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frutosapp.api.RetroFistCliente
import com.example.frutosapp.model.ResennaRequest
import com.example.frutosapp.model.ResennaResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ResennaViewModel : ViewModel() {

    private val resennaApi = RetroFistCliente.resennaApi

    private val _resennas =
        MutableStateFlow<List<ResennaResponse>>(emptyList())
    val resennas: StateFlow<List<ResennaResponse>> =
        _resennas.asStateFlow()

    private val _promedio = MutableStateFlow(0.0)
    val promedio: StateFlow<Double> =
        _promedio.asStateFlow()

    private val _resennaCompra =
        MutableStateFlow<ResennaResponse?>(null)
    val resennaCompra: StateFlow<ResennaResponse?> =
        _resennaCompra.asStateFlow()

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> =
        _cargando.asStateFlow()

    private val _mensajeError =
        MutableStateFlow<String?>(null)
    val mensajeError: StateFlow<String?> =
        _mensajeError.asStateFlow()

    private val _mensajeExito =
        MutableStateFlow<String?>(null)
    val mensajeExito: StateFlow<String?> =
        _mensajeExito.asStateFlow()

    fun cargarResennas(productoId: Long) {
        viewModelScope.launch {
            _cargando.value = true
            _mensajeError.value = null

            try {
                _resennas.value =
                    resennaApi.obtenerResennasProducto(productoId)

                _promedio.value =
                    resennaApi.obtenerPromedioProducto(productoId)

            } catch (e: Exception) {
                e.printStackTrace()

                _resennas.value = emptyList()
                _promedio.value = 0.0
                _mensajeError.value =
                    "No se pudieron cargar las reseñas."

            } finally {
                _cargando.value = false
            }
        }
    }

    fun consultarResennaCompra(
        clienteId: Long,
        ventaId: Long,
        productoId: Long
    ) {
        viewModelScope.launch {
            _cargando.value = true
            _mensajeError.value = null
            _resennaCompra.value = null

            try {
                val response =
                    resennaApi.obtenerResennaCompra(
                        clienteId = clienteId,
                        ventaId = ventaId,
                        productoId = productoId
                    )

                if (response.isSuccessful) {
                    _resennaCompra.value = response.body()
                } else {
                    _resennaCompra.value = null
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _resennaCompra.value = null

            } finally {
                _cargando.value = false
            }
        }
    }

    fun crearResenna(
        clienteId: Long,
        ventaId: Long,
        productoId: Long,
        estrellas: Int,
        comentario: String?
    ) {
        viewModelScope.launch {
            _cargando.value = true
            _mensajeError.value = null
            _mensajeExito.value = null

            try {
                val request = ResennaRequest(
                    clienteId = clienteId,
                    ventaId = ventaId,
                    productoId = productoId,
                    estrellas = estrellas,
                    comentario = comentario
                )

                val creada =
                    resennaApi.crearResenna(request)

                _resennaCompra.value = creada
                _mensajeExito.value =
                    "Reseña publicada correctamente."

            } catch (e: Exception) {
                e.printStackTrace()

                _mensajeError.value =
                    "No se pudo publicar la reseña."

            } finally {
                _cargando.value = false
            }
        }
    }

    fun actualizarResenna(
        idResenna: Long,
        clienteId: Long,
        ventaId: Long,
        productoId: Long,
        estrellas: Int,
        comentario: String?
    ) {
        viewModelScope.launch {
            _cargando.value = true
            _mensajeError.value = null
            _mensajeExito.value = null

            try {
                val request = ResennaRequest(
                    clienteId = clienteId,
                    ventaId = ventaId,
                    productoId = productoId,
                    estrellas = estrellas,
                    comentario = comentario
                )

                val actualizada =
                    resennaApi.actualizarResenna(
                        idResenna = idResenna,
                        request = request
                    )

                _resennaCompra.value = actualizada
                _mensajeExito.value =
                    "Reseña actualizada correctamente."

            } catch (e: Exception) {
                e.printStackTrace()

                _mensajeError.value =
                    "No se pudo actualizar la reseña."

            } finally {
                _cargando.value = false
            }
        }
    }

    fun eliminarResenna(idResenna: Long) {
        viewModelScope.launch {
            _cargando.value = true
            _mensajeError.value = null
            _mensajeExito.value = null

            try {
                val response =
                    resennaApi.eliminarResenna(idResenna)

                if (response.isSuccessful) {
                    _resennaCompra.value = null
                    _mensajeExito.value =
                        "Reseña eliminada correctamente."
                } else {
                    _mensajeError.value =
                        "No se pudo eliminar la reseña."
                }

            } catch (e: Exception) {
                e.printStackTrace()

                _mensajeError.value =
                    "Error al eliminar la reseña."

            } finally {
                _cargando.value = false
            }
        }
    }

    fun limpiarMensajes() {
        _mensajeError.value = null
        _mensajeExito.value = null
    }

    fun limpiarResennaCompra() {
        _resennaCompra.value = null
    }
}