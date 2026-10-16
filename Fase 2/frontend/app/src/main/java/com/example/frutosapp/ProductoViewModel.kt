package com.example.frutosapp


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frutosapp.api.RetroFistCliente
import com.example.frutosapp.model.Categoria
import com.example.frutosapp.model.Producto
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProductoViewModel : ViewModel() {

    private val productoApi = RetroFistCliente.productoApi
    private val categoriaApi = RetroFistCliente.categoriaApi

    // Datos base
    private val _productosBase = MutableStateFlow<List<Producto>>(emptyList())
    val productosBase: StateFlow<List<Producto>> = _productosBase.asStateFlow()
    private val _categoriasBase = MutableStateFlow<List<Categoria>>(emptyList())

    val categorias: StateFlow<List<Categoria>> = _categoriasBase.asStateFlow()

    // Búsqueda
    private val _textoBusqueda = MutableStateFlow("")
    val textoBusqueda: StateFlow<String> = _textoBusqueda.asStateFlow()

    // Filtro por categoría
    private val _categoriaSelect = MutableStateFlow<Categoria?>(null)
    val categoriaSelect: StateFlow<Categoria?> = _categoriaSelect.asStateFlow()

    // Mensajes (para Toast / Snackbar)
    private val _mensaje = MutableSharedFlow<String>()
    val mensaje = _mensaje.asSharedFlow()

    // Loading
    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    // Lista de productos filtrados para la aplicación cliente
    val productos: StateFlow<List<Producto>> = combine(
        _productosBase,
        _textoBusqueda,
        _categoriaSelect
    ) { productos, texto, categoria ->

        val textoFiltro = texto.trim().lowercase()

        // filtro por texto
        val filtradosPorTexto = if (textoFiltro.isBlank()) {
            productos
        } else {
            productos.filter { it.nomProducto.lowercase().contains(textoFiltro) }
        }

        // filtro por categoría
        if (categoria == null) {
            filtradosPorTexto
        } else {
            filtradosPorTexto.filter { it.categoria.idCategoria == categoria.idCategoria }
        }

    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        cargarDesdeApi()
    }

    // Cargar productos y categorías desde la API
    fun cargarDesdeApi() {
        viewModelScope.launch {
            _cargando.value = true
            try {
                val categoriasApi = categoriaApi.listarCategorias()
                _categoriasBase.value = categoriasApi

                val productosApi = productoApi.listarProductos()
                _productosBase.value = productosApi

            } catch (e: Exception) {
                e.printStackTrace()
                _mensaje.emit("Error al cargar productos/categorías desde la API")
                _productosBase.value = emptyList()
            } finally {
                _cargando.value = false
            }
        }
    }

    fun onBusquedaChange(nuevoTexto: String) {
        _textoBusqueda.value = nuevoTexto
    }

    fun onCategoriaSelected(categoria: Categoria?) {
        _categoriaSelect.value = categoria
    }


}

