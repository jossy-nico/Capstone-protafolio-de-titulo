package com.example.frutosapp.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetroFistCliente {
    // Emulador Android accediendo a backend en localhost:8080

    private const val BASE_URL_PRODUCTOS = "http://10.0.2.2:8080/"
    private const val BASE_URL_USUARIOS = "http://10.0.2.2:8181/"
    private const val BASE_URL_CARRITO = "http://10.0.2.2:8282/"
    private const val BASE_URL_VENTA = "http://10.0.2.2:8383/"

    private const val BASE_URL_RESENNAS = "http://10.0.2.2:8484/"

    //solo se crea una vez, es un objeto único
    private fun buildRetrofit(baseUrl: String): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    // API de productos
    val productoApi: ProductoApi by lazy {
        buildRetrofit(BASE_URL_PRODUCTOS).create(ProductoApi::class.java)
    }

    // API de categorias
    val categoriaApi: CategoriaApi by lazy {
        buildRetrofit(BASE_URL_PRODUCTOS).create(CategoriaApi::class.java)
    }

    //api usuarios
    val usuarioApi: UsuarioApi by lazy{
        buildRetrofit(BASE_URL_USUARIOS).create(UsuarioApi::class.java)
    }
    //api cliente
    val  clienteApi: ClienteApi by lazy {
        buildRetrofit(BASE_URL_USUARIOS).create((ClienteApi::class.java))
    }

    //api empleado
    val empleadoApi: EmpleadoApi by lazy {
        buildRetrofit(BASE_URL_USUARIOS).create(EmpleadoApi::class.java)
    }

    //api de login
    val authApi: AuthApi by lazy {
        buildRetrofit(BASE_URL_USUARIOS).create(AuthApi::class.java)
    }
    //Api de carrito
    val carritoApi: CarritoApi by lazy {
        buildRetrofit(BASE_URL_CARRITO).create(CarritoApi::class.java)
    }

    //Api ventas
    val ventaApi: VentaApi by lazy {
        buildRetrofit(BASE_URL_VENTA).create(VentaApi::class.java)
    }

    //reseñas
    val resennaApi: ResennaApi by lazy {
        buildRetrofit(BASE_URL_RESENNAS).create(ResennaApi::class.java)
    }
    // API de favoritos
    val favoritoApi: FavoritoApi by lazy {
        buildRetrofit(BASE_URL_USUARIOS).create(FavoritoApi::class.java)
    }
}