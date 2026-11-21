
package com.example.frutosapp.api

import android.content.Context

import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route

import org.json.JSONObject

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetroFistCliente {

    // ==========================================
    // DIRECCIONES DE MICROSERVICIOS
    // ==========================================

    private val BASE_URL_PRODUCTOS = com.example.frutosapp.BuildConfig.API_BASE_URL
    private val BASE_URL_USUARIOS = com.example.frutosapp.BuildConfig.API_BASE_URL
    private val BASE_URL_CARRITO = com.example.frutosapp.BuildConfig.API_BASE_URL
    private val BASE_URL_VENTA = com.example.frutosapp.BuildConfig.API_BASE_URL
    private val BASE_URL_RESENNAS = com.example.frutosapp.BuildConfig.API_BASE_URL
    // ==========================================
    // JWT EN MEMORIA
    // ==========================================

    @Volatile
    private var tokenJwt: String? = null

    @Volatile
    private var sesionSegura: SesionSegura? = null

    private val bloqueoRenovacion = Any()

    // Evita restablecer una sesión que haya
    // sido cambiada mientras se renovaba.
    @Volatile
    private var versionCredenciales = 0L

    // ==========================================
    // INICIALIZAR AL ABRIR LA APLICACIÓN
    // ==========================================

    fun inicializar(context: Context) {

        sesionSegura = SesionSegura(
            context.applicationContext
        )
    }

    // ==========================================
    // GUARDAR JWT
    // ==========================================

    @Synchronized
    fun guardarTokenJwt(token: String) {

        val tokenLimpio = token
            .trim()
            .removePrefix("Bearer ")
            .trim()

        require(tokenLimpio.isNotBlank()) {
            "El token JWT no puede estar vacío."
        }

        tokenJwt = tokenLimpio
        versionCredenciales++
    }

    // ==========================================
    // LIMPIAR JWT
    // ==========================================

    @Synchronized
    fun limpiarTokenJwt() {

        tokenJwt = null
        versionCredenciales++
    }

    fun existeSesionJwt(): Boolean {

        return !tokenJwt.isNullOrBlank()
    }

    // ==========================================
    // IDENTIFICAR ENDPOINTS PROTEGIDOS
    // ==========================================

    private fun requiereJwt(
        request: Request
    ): Boolean {

        val url = request.url
        val ruta = url.encodedPath
        val puerto = url.port
        val metodo = request.method

        val esUsuarioApi = puerto == 8181
        val esVentaApi = puerto == 8383

        val esWebpayPublico =
            esVentaApi &&
                    ruta.startsWith("/api/webpay/")

        val esSeguimientoInvitadoPublico =
            esVentaApi &&
                    ruta == "/api/ventas/invitados/seguimiento" &&
                    metodo == "POST"

        val esRegistroPublico =
            esUsuarioApi &&
                    ruta == "/api/usuarios" &&
                    metodo == "POST"

        val esLoginPublico =
            esUsuarioApi &&
                    (
                            ruta == "/api/usuarios/login" ||
                                    ruta == "/api/usuarios/login-token" ||
                                    ruta == "/api/usuarios/login-token-persistente" ||
                                    ruta == "/api/usuarios/refresh-token" ||
                                    ruta == "/api/usuarios/logout-token"
                            )

        return (esUsuarioApi || esVentaApi) &&
                !esWebpayPublico &&
                !esSeguimientoInvitadoPublico &&
                !esRegistroPublico &&
                !esLoginPublico
    }

    // ==========================================
    // INTERCEPTOR JWT
    // ==========================================

    private val jwtInterceptor = Interceptor { chain ->

        val requestOriginal = chain.request()

        val jwtActual = tokenJwt

        val requestFinal =
            if (
                requiereJwt(requestOriginal) &&
                !jwtActual.isNullOrBlank()
            ) {

                requestOriginal.newBuilder()
                    .header(
                        "Authorization",
                        "Bearer $jwtActual"
                    )
                    .build()

            } else {

                requestOriginal
            }

        chain.proceed(requestFinal)
    }

    // ==========================================
    // CLIENTE AISLADO PARA RENOVACIÓN
    // ==========================================
    //
    // No utiliza el interceptor JWT ni el
    // Authenticator principal.
    //
    // Esto evita una renovación recursiva.
    // ==========================================

    private val clienteRenovacion: OkHttpClient by lazy {

        OkHttpClient.Builder()
            .build()
    }

    // ==========================================
    // RENOVAR CREDENCIALES
    // ==========================================

    private fun solicitarRenovacion(
        refreshToken: String
    ): Pair<String, String>? {

        val json = JSONObject()
            .put("refreshToken", refreshToken)

        val body = json.toString().toRequestBody(
            "application/json; charset=utf-8".toMediaType()
        )

        val request = Request.Builder()
            .url(
                BASE_URL_USUARIOS +
                        "api/usuarios/refresh-token"
            )
            .post(body)
            .build()

        clienteRenovacion.newCall(request)
            .execute()
            .use { response ->

                if (!response.isSuccessful) {

                    // Solo un rechazo explícito de
                    // credenciales permite descartarlas.
                    if (
                        response.code == 400 ||
                        response.code == 401 ||
                        response.code == 403
                    ) {
                        throw CredencialRevocadaException()
                    }

                    return null
                }

                val contenido =
                    response.body?.string()
                        ?: return null

                val resultado = JSONObject(contenido)

                val nuevoJwt =
                    resultado.optString("token", "")

                val nuevoRefresh =
                    resultado.optString("refreshToken", "")

                if (
                    nuevoJwt.isBlank() ||
                    nuevoRefresh.isBlank()
                ) {
                    return null
                }

                return Pair(
                    nuevoJwt,
                    nuevoRefresh
                )
            }
    }

    private class CredencialRevocadaException :
        RuntimeException()

    // ==========================================
    // AUTHENTICATOR
    // ==========================================
    //
    // Solo reacciona ante HTTP 401 de una
    // petición protegida con JWT.
    // ==========================================

    private val jwtAuthenticator = object : Authenticator {

        override fun authenticate(
            route: Route?,
            response: Response
        ): Request? {

            val requestOriginal = response.request

            if (!requiereJwt(requestOriginal)) {
                return null
            }

            // No entrar en ciclos de reintentos.
            if (response.priorResponse != null) {
                return null
            }

            val authorizationAnterior =
                requestOriginal.header("Authorization")
                    ?: return null

            synchronized(bloqueoRenovacion) {

                val jwtActual = tokenJwt
                    ?: return null

                val authorizationActual =
                    "Bearer $jwtActual"

                // Otra petición pudo renovar primero.
                // Reutilizamos el JWT actualizado.
                if (
                    authorizationAnterior !=
                    authorizationActual
                ) {

                    return requestOriginal.newBuilder()
                        .header(
                            "Authorization",
                            authorizationActual
                        )
                        .build()
                }

                val almacenamiento =
                    sesionSegura ?: return null

                val refreshToken =
                    almacenamiento.obtenerRefreshToken()
                        ?: return null

                val versionInicial = versionCredenciales

                try {

                    val nuevosTokens =
                        solicitarRenovacion(refreshToken)
                            ?: return null

                    // Si se produjo un logout o
                    // cambio de cuenta, no reactivar
                    // la sesión anterior.
                    if (
                        versionCredenciales != versionInicial ||
                        tokenJwt != jwtActual
                    ) {
                        return null
                    }

                    // Primero persistimos el token
                    // rotado y luego actualizamos JWT.
                    almacenamiento.guardarRefreshToken(
                        nuevosTokens.second
                    )

                    guardarTokenJwt(
                        nuevosTokens.first
                    )

                    return requestOriginal.newBuilder()
                        .header(
                            "Authorization",
                            "Bearer ${nuevosTokens.first}"
                        )
                        .build()

                } catch (_: CredencialRevocadaException) {

                    // No limpiar una sesión nueva.
                    if (
                        versionCredenciales == versionInicial &&
                        tokenJwt == jwtActual
                    ) {

                        almacenamiento.limpiar()
                        limpiarTokenJwt()
                    }

                    return null

                } catch (_: Exception) {

                    // Ante un problema de conexión
                    // no borrar automáticamente
                    // la credencial almacenada.
                    return null
                }
            }
        }
    }

    // ==========================================
    // CLIENTE HTTP PRINCIPAL
    // ==========================================

    private val clienteHttp: OkHttpClient by lazy {

        OkHttpClient.Builder()
            .addInterceptor(jwtInterceptor)
            .authenticator(jwtAuthenticator)
            .build()
    }

    // ==========================================
    // CONSTRUCCIÓN RETROFIT
    // ==========================================

    private fun buildRetrofit(
        baseUrl: String
    ): Retrofit {

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(clienteHttp)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
    }

    // ==========================================
    // API PRODUCTOS
    // ==========================================

    val productoApi: ProductoApi by lazy {

        buildRetrofit(BASE_URL_PRODUCTOS)
            .create(ProductoApi::class.java)
    }

    // ==========================================
    // API CATEGORÍAS
    // ==========================================

    val categoriaApi: CategoriaApi by lazy {

        buildRetrofit(BASE_URL_PRODUCTOS)
            .create(CategoriaApi::class.java)
    }

    // ==========================================
    // API USUARIOS
    // ==========================================

    val usuarioApi: UsuarioApi by lazy {

        buildRetrofit(BASE_URL_USUARIOS)
            .create(UsuarioApi::class.java)
    }

    // ==========================================
    // API CLIENTES
    // ==========================================

    val clienteApi: ClienteApi by lazy {

        buildRetrofit(BASE_URL_USUARIOS)
            .create(ClienteApi::class.java)
    }

    // ==========================================
    // API EMPLEADOS
    // ==========================================

    val empleadoApi: EmpleadoApi by lazy {

        buildRetrofit(BASE_URL_USUARIOS)
            .create(EmpleadoApi::class.java)
    }

    // ==========================================
    // API LOGIN
    // ==========================================

    val authApi: AuthApi by lazy {

        buildRetrofit(BASE_URL_USUARIOS)
            .create(AuthApi::class.java)
    }

    // ==========================================
    // API CARRITO
    // ==========================================

    val carritoApi: CarritoApi by lazy {

        buildRetrofit(BASE_URL_CARRITO)
            .create(CarritoApi::class.java)
    }

    // ==========================================
    // API VENTAS
    // ==========================================

    val ventaApi: VentaApi by lazy {

        buildRetrofit(BASE_URL_VENTA)
            .create(VentaApi::class.java)
    }

    // ==========================================
    // API RESEÑAS
    // ==========================================

    val resennaApi: ResennaApi by lazy {

        buildRetrofit(BASE_URL_RESENNAS)
            .create(ResennaApi::class.java)
    }

    // ==========================================
    // API FAVORITOS
    // ==========================================

    val favoritoApi: FavoritoApi by lazy {

        buildRetrofit(BASE_URL_USUARIOS)
            .create(FavoritoApi::class.java)
    }
}
