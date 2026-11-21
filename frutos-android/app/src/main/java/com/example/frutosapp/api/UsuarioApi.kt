
package com.example.frutosapp.api

import com.example.frutosapp.model.Usuario

import okhttp3.ResponseBody

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import com.example.frutosapp.model.ActualizarMiPerfilRequest

interface UsuarioApi {

    // ==========================================
    // LOGIN JWT - CLIENTE REGISTRADO
    // ==========================================
    //
    // Backend:
    // POST /api/usuarios/login-token
    //
    // La respuesta se recibe como ResponseBody
    // para adaptarla al contrato real del backend,
    // sin crear otro modelo Kotlin.

    @POST("api/usuarios/login-token")
    suspend fun iniciarSesionJwt(
        @Body credenciales: Map<String, String>
    ): Response<ResponseBody>

    // ==========================================
    // LISTADO DE USUARIOS
    // ==========================================
    // Ruta administrativa protegida.

    @GET("api/usuarios")
    suspend fun getUsuarios(): Response<List<Usuario>>

    // ==========================================
    // OBTENER USUARIO
    // ==========================================

    @GET("api/usuarios/{id}")
    suspend fun getUsuario(
        @Path("id") id: Long
    ): Response<Usuario>

    // ==========================================
    // REGISTRAR CLIENTE
    // ==========================================
    // El backend controla el rol CLIENTE.

    @POST("api/usuarios")
    suspend fun crearUsuario(
        @Body usuario: Usuario
    ): Response<Usuario>

    // ==========================================
    // ACTUALIZAR USUARIO
    // ==========================================
    // Ruta protegida por JWT.

    @PUT("api/usuarios/{id}")
    suspend fun actualizarUsuario(
        @Path("id") id: Long,
        @Body usuario: Usuario
    ): Response<Usuario>

    // ==========================================
    // ELIMINAR USUARIO
    // ==========================================
    // El backend mantiene deshabilitada
    // la eliminación física de usuarios.

    @DELETE("api/usuarios/{id}")
    suspend fun eliminarUsuario(
        @Path("id") id: Long
    ): Response<Unit>


    // ==========================================
    // CONSULTAR MI PERFIL
    // ==========================================
    // El backend identifica al cliente
    // mediante el JWT.
    // No se envía un usuarioId.
    // ==========================================

    @GET("api/usuarios/mi-perfil")
    suspend fun getMiPerfil(): Response<Usuario>


    // ==========================================
    // ACTUALIZAR MI PERFIL
    // ==========================================

    @PUT("api/usuarios/mi-perfil")
    suspend fun actualizarMiPerfil(
        @Body request: ActualizarMiPerfilRequest
    ): Response<Usuario>


    // ==========================================
    // CAMBIAR CONTRASEÑA DEL TITULAR
    // ==========================================

    @PUT("api/usuarios/{idUsuario}/cambiar-password")
    suspend fun cambiarPassword(
        @Path("idUsuario") usuarioId: Long,
        @Body request: Map<String, String>
    ): Response<ResponseBody>

}
