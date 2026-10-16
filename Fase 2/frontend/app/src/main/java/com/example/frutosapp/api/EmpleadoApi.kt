package com.example.frutosapp.api

import com.example.frutosapp.model.Empleado
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface EmpleadoApi {
    // GET /api/empleados/{usuarioId}
    @GET("api/empleados/{usuarioId}")
    suspend fun obtenerEmpleado(
        @Path("usuarioId") usuarioId: Long
    ): Response<Empleado>

    // POST /api/empleados
    @POST("api/empleados")
    suspend fun crearEmpleado(
        @Body empleado: Empleado
    ): Response<Empleado>

    // PUT /api/empleados/{usuarioId}
    @PUT("api/empleados/{usuarioId}")
    suspend fun actualizarEmpleado(
        @Path("usuarioId") usuarioId: Long,
        @Body empleado: Empleado
    ): Response<Empleado>

    // DELETE /api/empleados/{usuarioId}
    @DELETE("api/empleados/{usuarioId}")
    suspend fun eliminarEmpleado(
        @Path("usuarioId") usuarioId: Long
    ): Response<Void>
}