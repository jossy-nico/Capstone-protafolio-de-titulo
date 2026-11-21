package com.example.frutosapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.frutosapp.api.RetroFistCliente
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FavoritoViewModel : ViewModel() {

    private val favoritoApi = RetroFistCliente.favoritoApi

    private val _favoritos = MutableStateFlow<Set<Int>>(emptySet())
    val favoritos: StateFlow<Set<Int>> = _favoritos.asStateFlow()

    fun cargarFavoritos(idUsuario: Long) {
        viewModelScope.launch {
            try {
                val response = favoritoApi.getFavoritos(idUsuario)

                if (response.isSuccessful) {
                    _favoritos.value = response.body()
                        ?.map { it.idProducto }
                        ?.toSet()
                        ?: emptySet()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun esFavorito(idProducto: Int): Boolean {
        return _favoritos.value.contains(idProducto)
    }

    fun alternarFavorito(
        idUsuario: Long,
        idProducto: Int
    ) {
        viewModelScope.launch {
            try {

                if (_favoritos.value.contains(idProducto)) {

                    val response = favoritoApi.eliminarFavorito(
                        idUsuario,
                        idProducto
                    )

                    if (response.isSuccessful) {
                        _favoritos.value =
                            _favoritos.value - idProducto
                    }

                } else {

                    val response = favoritoApi.agregarFavorito(
                        idUsuario,
                        idProducto
                    )

                    if (response.isSuccessful) {
                        _favoritos.value =
                            _favoritos.value + idProducto
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun limpiarFavoritos() {
        _favoritos.value = emptySet()
    }
}