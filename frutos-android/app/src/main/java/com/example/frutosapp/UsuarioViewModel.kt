
package com.example.frutosapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.example.frutosapp.api.RetroFistCliente
import com.example.frutosapp.model.Cliente
import com.example.frutosapp.model.Usuario

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.frutosapp.model.LoginRequest
import org.json.JSONObject
import com.example.frutosapp.model.ActualizarMiPerfilRequest
import com.example.frutosapp.model.ActualizarPerfilCompletoRequest

class UsuarioViewModel : ViewModel() {

    // ==========================================
    // APIS
    // ==========================================

    private val usuarioApi =
        RetroFistCliente.usuarioApi

    private val clienteApi =
        RetroFistCliente.clienteApi

    // ==========================================
    // USUARIO SELECCIONADO
    // ==========================================

    private val _usuarioSeleccionado =
        MutableStateFlow<Usuario?>(null)

    val usuarioSeleccionado: StateFlow<Usuario?> =
        _usuarioSeleccionado.asStateFlow()

    // ==========================================
    // CLIENTE SELECCIONADO
    // ==========================================

    private val _clienteSeleccionado =
        MutableStateFlow<Cliente?>(null)

    val clienteSeleccionado: StateFlow<Cliente?> =
        _clienteSeleccionado.asStateFlow()

    // ==========================================
    // USUARIO CLIENTE ACTUALIZADO
    // ==========================================

    private val _usuarioClienteSeleccionado =
        MutableStateFlow<Usuario?>(null)

    val usuarioClienteSeleccionado: StateFlow<Usuario?> =
        _usuarioClienteSeleccionado.asStateFlow()

    // ==========================================
    // ERRORES Y MENSAJES
    // ==========================================

    private val _errorMensaje =
        MutableStateFlow<String?>(null)

    val errorMensaje: StateFlow<String?> =
        _errorMensaje.asStateFlow()

    private val _mensaje =
        MutableSharedFlow<String>()

    val mensaje =
        _mensaje.asSharedFlow()

    private val _cargando =
        MutableStateFlow(false)

    val cargando: StateFlow<Boolean> =
        _cargando.asStateFlow()

    // ==========================================
    // VALIDAR SESIÓN JWT
    // ==========================================

    private fun tieneSesionJwt(): Boolean {
        return RetroFistCliente.existeSesionJwt()
    }

    private fun mensajeHttp(
        codigo: Int,
        operacion: String
    ): String {

        return when (codigo) {

            400 ->
                "Los datos enviados no son válidos."

            401 ->
                "Tu sesión no es válida o ha expirado. " +
                        "Inicia sesión nuevamente."

            403 ->
                "No tienes autorización para $operacion."

            404 ->
                "No se encontró la información solicitada."

            409 ->
                "Existe un conflicto con los datos registrados."

            else ->
                "No fue posible $operacion. " +
                        "Código HTTP: $codigo"
        }
    }

    // ==========================================
    // REGISTRAR CLIENTE
    // ==========================================
    //
    // El registro es público.
    // No necesita JWT.
    //
    // Conservamos la creación del usuario
    // y luego la creación del perfil cliente.
    // ==========================================


    // ==========================================
    // REGISTRO DE CLIENTE CON JWT
    // ==========================================
    //
    // 1. Validar datos.
    // 2. Crear usuario.
    // 3. Obtener JWT.
    // 4. Crear perfil del titular.
    // 5. Confirmar registro completo.
    //
    // No crear dos veces el usuario.
    // ==========================================

    fun registrarCliente(
        nombre: String,
        apellido: String,
        correo: String,
        password: String,
        direccion: String,
        telefono: String
    ) {

        viewModelScope.launch {

            if (_cargando.value) {
                return@launch
            }

            _cargando.value = true
            _errorMensaje.value = null

            var usuarioCreadoId: Long? = null

            try {

                // ==================================
                // 1. VALIDAR DATOS DEL FORMULARIO
                // ==================================

                val nombreLimpio = nombre.trim()
                val apellidoLimpio = apellido.trim()
                val correoLimpio = correo.trim().lowercase()
                val direccionLimpia = direccion.trim()
                val telefonoLimpio = telefono.trim()

                if (
                    nombreLimpio.isBlank() ||
                    apellidoLimpio.isBlank() ||
                    correoLimpio.isBlank() ||
                    password.isBlank() ||
                    direccionLimpia.isBlank() ||
                    telefonoLimpio.isBlank()
                ) {

                    val mensajeError =
                        "Todos los campos son obligatorios."

                    _errorMensaje.value = mensajeError
                    _mensaje.emit(mensajeError)
                    return@launch
                }

                if (
                    !android.util.Patterns.EMAIL_ADDRESS
                        .matcher(correoLimpio)
                        .matches()
                ) {

                    val mensajeError =
                        "Ingresa un correo electrónico válido."

                    _errorMensaje.value = mensajeError
                    _mensaje.emit(mensajeError)
                    return@launch
                }

                if (
                    password.length < 8 ||
                    password.length > 72
                ) {

                    val mensajeError =
                        "La contraseña debe tener entre " +
                                "8 y 72 caracteres."

                    _errorMensaje.value = mensajeError
                    _mensaje.emit(mensajeError)
                    return@launch
                }

                if (
                    !telefonoLimpio.matches(
                        Regex("^9\\d{8}$")
                    )
                ) {

                    val mensajeError =
                        "El teléfono debe tener 9 dígitos " +
                                "y comenzar con 9. " +
                                "Ejemplo: 912345678"

                    _errorMensaje.value = mensajeError
                    _mensaje.emit(mensajeError)
                    return@launch
                }

                // ==================================
                // 2. CREAR USUARIO
                // ==================================

                val nuevoUsuario = Usuario(
                    idUsuario = null,
                    nombre = nombreLimpio,
                    apellido = apellidoLimpio,
                    correo = correoLimpio,
                    password = password,
                    tipoUsuario = "CLIENTE"
                )

                val respuestaUsuario =
                    usuarioApi.crearUsuario(nuevoUsuario)

                if (!respuestaUsuario.isSuccessful) {

                    val mensajeError =
                        if (
                            respuestaUsuario.code() == 409 ||
                            respuestaUsuario.code() == 400
                        ) {

                            "No fue posible crear la cuenta " +
                                    "(HTTP ${respuestaUsuario.code()}). " +
                                    "Si el correo ya está registrado, " +
                                    "inicia sesión en lugar de " +
                                    "registrarte nuevamente."

                        } else {

                            mensajeHttp(
                                respuestaUsuario.code(),
                                "registrar el usuario"
                            )
                        }

                    _errorMensaje.value = mensajeError
                    _mensaje.emit(mensajeError)
                    return@launch
                }

                val usuarioCreado =
                    respuestaUsuario.body()

                val usuarioId =
                    usuarioCreado?.idUsuario

                if (usuarioId == null || usuarioId <= 0L) {

                    val mensajeError =
                        "El servidor respondió al registro, " +
                                "pero no entregó un ID de usuario. " +
                                "No repitas el registro sin " +
                                "verificar primero la cuenta."

                    _errorMensaje.value = mensajeError
                    _mensaje.emit(mensajeError)
                    return@launch
                }

                usuarioCreadoId = usuarioId

                // ==================================
                // 3. OBTENER JWT DEL NUEVO USUARIO
                // ==================================

                val respuestaJwt =
                    RetroFistCliente.authApi.loginJwt(
                        LoginRequest(
                            correo = correoLimpio,
                            password = password
                        )
                    )

                if (!respuestaJwt.isSuccessful) {

                    val mensajeError =
                        "La cuenta fue creada con ID " +
                                "$usuarioId, pero no fue " +
                                "posible iniciar sesión para " +
                                "completar el perfil. " +
                                "HTTP ${respuestaJwt.code()}. " +
                                "No vuelvas a registrarla."

                    _errorMensaje.value = mensajeError
                    _mensaje.emit(mensajeError)
                    return@launch
                }

                val contenidoJwt =
                    respuestaJwt.body()?.string()

                if (contenidoJwt.isNullOrBlank()) {

                    val mensajeError =
                        "La cuenta fue creada, pero " +
                                "el servidor no entregó la " +
                                "respuesta de autenticación."

                    _errorMensaje.value = mensajeError
                    _mensaje.emit(mensajeError)
                    return@launch
                }

                val json = JSONObject(contenidoJwt)

                val token = listOf(
                    "token",
                    "accessToken",
                    "access_token",
                    "jwt"
                ).firstNotNullOfOrNull { campo ->

                    json.optString(campo, "")
                        .takeIf {
                            it.isNotBlank() &&
                                    it != "null"
                        }
                }

                if (token.isNullOrBlank()) {

                    val mensajeError =
                        "La cuenta fue creada, pero " +
                                "no se encontró el JWT en " +
                                "la respuesta del servidor."

                    _errorMensaje.value = mensajeError
                    _mensaje.emit(mensajeError)
                    return@launch
                }

                // Retrofit enviará Authorization:
                // Bearer JWT al endpoint /api/clientes.

                RetroFistCliente.guardarTokenJwt(token)

                // ==================================
                // 4. CREAR PERFIL DEL CLIENTE
                // ==================================
                //
                // ClienteController comprobará:
                //
                // - JWT válido.
                // - Rol CLIENTE.
                // - Cuenta ACTIVA.
                // - ID del usuario igual al JWT.
                // ==================================


                val clienteReq = Cliente(
                    usuarioId = null,

                    usuario = Usuario(
                        idUsuario = usuarioId,
                        nombre = nombreLimpio,
                        apellido = apellidoLimpio,
                        correo = correoLimpio,
                        password = "",
                        tipoUsuario = "CLIENTE"
                    ),

                    telefono = telefonoLimpio,
                    direccion = direccionLimpia
                )


                val respuestaCliente =
                    clienteApi.crearCliente(clienteReq)

                if (!respuestaCliente.isSuccessful) {

                    val mensajeError =
                        "La cuenta fue creada con ID " +
                                "$usuarioId, pero falló la " +
                                "creación del perfil. " +
                                "HTTP ${respuestaCliente.code()}. " +
                                "No vuelvas a registrar " +
                                "el mismo correo."

                    _errorMensaje.value = mensajeError
                    _mensaje.emit(mensajeError)
                    return@launch
                }

                // ==================================
                // 5. REGISTRO COMPLETADO
                // ==================================

                _usuarioSeleccionado.value =
                    usuarioCreado

                _usuarioClienteSeleccionado.value =
                    usuarioCreado

                _clienteSeleccionado.value = Cliente(
                    usuarioId = usuarioId,
                    usuario = usuarioCreado,
                    telefono = telefonoLimpio,
                    direccion = direccionLimpia
                )

                _errorMensaje.value = null

                _mensaje.emit(
                    "Cliente registrado correctamente"
                )

            } catch (e: Exception) {

                val mensajeError =
                    if (usuarioCreadoId != null) {

                        "El usuario ID $usuarioCreadoId " +
                                "ya fue creado, pero ocurrió " +
                                "un error al completar el perfil. " +
                                "No repitas el registro."

                    } else {

                        "No se pudo completar el registro. " +
                                "Verifica si la cuenta ya existe " +
                                "antes de intentarlo nuevamente."
                    }

                _errorMensaje.value = mensajeError
                _mensaje.emit(mensajeError)

            } finally {

                // El registro no inicia una sesión
                // permanente automáticamente.
                //
                // El cliente deberá utilizar
                // el login normal de la aplicación.

                RetroFistCliente.limpiarTokenJwt()

                _cargando.value = false
            }
        }
    }


    // ==========================================
    // CARGAR DATOS DEL CLIENTE
    // ==========================================
    //
    // RetroFistCliente incorpora el JWT.
    //
    // La autorización final depende del
    // SecurityConfig de usuario-api.
    // ==========================================

    fun cargarDatosCliente(usuarioId: Long) {

        viewModelScope.launch {

            _cargando.value = true
            _errorMensaje.value = null

            try {

                if (!tieneSesionJwt()) {

                    val mensajeError =
                        "Debes iniciar sesión para " +
                                "consultar tu perfil."

                    _errorMensaje.value = mensajeError
                    _mensaje.emit(mensajeError)

                    return@launch
                }

                if (usuarioId <= 0L) {

                    val mensajeError =
                        "El identificador del usuario " +
                                "no es válido."

                    _errorMensaje.value = mensajeError
                    _mensaje.emit(mensajeError)

                    return@launch
                }

                // ==================================
                // CONSULTAR USUARIO
                // ==================================

                val respUser = usuarioApi.getMiPerfil()

                if (!respUser.isSuccessful) {

                    val mensajeError = mensajeHttp(
                        respUser.code(),
                        "consultar los datos del usuario"
                    )

                    _errorMensaje.value = mensajeError
                    _mensaje.emit(mensajeError)

                    return@launch
                }

                // ==================================
                // CONSULTAR PERFIL CLIENTE
                // ==================================

                val respCliente =
                    clienteApi.obtenerCliente(usuarioId)

                if (!respCliente.isSuccessful) {

                    val mensajeError = mensajeHttp(
                        respCliente.code(),
                        "consultar los datos del cliente"
                    )

                    _errorMensaje.value = mensajeError
                    _mensaje.emit(mensajeError)

                    return@launch
                }

                // ==================================
                // ACTUALIZAR ESTADOS
                // ==================================

                _usuarioSeleccionado.value =
                    respUser.body()

                _clienteSeleccionado.value =
                    respCliente.body()

                _usuarioClienteSeleccionado.value =
                    respUser.body()

            } catch (e: Exception) {

                val mensajeError =
                    "Error al cargar el perfil: " +
                            (e.localizedMessage
                                ?: "Error de conexión.")

                _errorMensaje.value = mensajeError
                _mensaje.emit(mensajeError)

            } finally {

                _cargando.value = false
            }
        }
    }


    // ==========================================
    // ACTUALIZAR PERFIL DEL CLIENTE
    // ==========================================
    // Mantiene la firma actual para no
    // modificar la navegación ni otras vistas.
    //
    // El correo y la contraseña no se envían.
    // El backend identifica al cliente por JWT.
    // ==========================================

    @Suppress("UNUSED_PARAMETER")
    fun actualizarPerfilCliente(
        usuarioId: Long,
        nombre: String,
        apellido: String,
        correo: String,
        password: String,
        direccion: String,
        telefono: String
    ) {

        if (_cargando.value) {
            return
        }

        viewModelScope.launch {

            _cargando.value = true
            _errorMensaje.value = null

            try {

                // ==================================
                // 1. VALIDAR SESIÓN
                // ==================================

                if (!tieneSesionJwt()) {

                    val mensajeError =
                        "Debes iniciar sesión para actualizar tu perfil."

                    _errorMensaje.value = mensajeError
                    _mensaje.emit(mensajeError)

                    return@launch
                }

                // ==================================
                // 2. VALIDAR IDENTIDAD Y DATOS
                // ==================================

                if (usuarioId <= 0L) {

                    val mensajeError =
                        "El identificador del cliente no es válido."

                    _errorMensaje.value = mensajeError
                    _mensaje.emit(mensajeError)

                    return@launch
                }

                val nombreLimpio = nombre.trim()
                val apellidoLimpio = apellido.trim()
                val telefonoLimpio = telefono.trim()
                val direccionLimpia = direccion.trim()

                if (
                    nombreLimpio.length < 2 ||
                    apellidoLimpio.length < 2
                ) {

                    val mensajeError =
                        "Nombre y apellido deben tener " +
                                "al menos 2 caracteres."

                    _errorMensaje.value = mensajeError
                    _mensaje.emit(mensajeError)

                    return@launch
                }

                if (!telefonoLimpio.matches(Regex("9\\d{8}"))) {

                    val mensajeError =
                        "El teléfono debe tener 9 dígitos y comenzar con 9."

                    _errorMensaje.value = mensajeError
                    _mensaje.emit(mensajeError)

                    return@launch
                }

                if (direccionLimpia.isBlank()) {

                    val mensajeError =
                        "La dirección de envío es obligatoria."

                    _errorMensaje.value = mensajeError
                    _mensaje.emit(mensajeError)

                    return@launch
                }

                // ==================================
                // 3. ACTUALIZACIÓN ÚNICA
                // ==================================

                val request = ActualizarPerfilCompletoRequest(
                    nombre = nombreLimpio,
                    apellido = apellidoLimpio,
                    telefono = telefonoLimpio,
                    direccion = direccionLimpia
                )

                val respuesta =
                    clienteApi.actualizarMiPerfilCompleto(
                        request
                    )

                if (!respuesta.isSuccessful) {

                    val mensajeError = mensajeHttp(
                        respuesta.code(),
                        "actualizar el perfil"
                    )

                    _errorMensaje.value = mensajeError
                    _mensaje.emit(mensajeError)

                    return@launch
                }

                // ==================================
                // 4. REFRESCAR INFORMACIÓN REAL
                // ==================================

                val respuestaUsuario =
                    usuarioApi.getMiPerfil()

                val respuestaCliente =
                    clienteApi.obtenerCliente(usuarioId)

                if (
                    respuestaUsuario.isSuccessful &&
                    respuestaCliente.isSuccessful &&
                    respuestaUsuario.body() != null &&
                    respuestaCliente.body() != null
                ) {

                    val usuarioActualizado =
                        respuestaUsuario.body()!!

                    val clienteActualizado =
                        respuestaCliente.body()!!

                    _usuarioSeleccionado.value =
                        usuarioActualizado

                    _usuarioClienteSeleccionado.value =
                        usuarioActualizado

                    _clienteSeleccionado.value =
                        clienteActualizado

                    _mensaje.emit(
                        "Perfil actualizado correctamente."
                    )

                } else {

                    // El PUT ya fue aceptado.
                    // No afirmar que falló el guardado
                    // solo porque falló la recarga.

                    _mensaje.emit(
                        "Los cambios se guardaron, " +
                                "pero no fue posible recargar " +
                                "el perfil. Vuelve a abrir la pantalla."
                    )
                }

            } catch (
                e: kotlinx.coroutines.CancellationException
            ) {

                throw e

            } catch (e: Exception) {

                val mensajeError =
                    "No se pudo completar la operación. " +
                            "Verifica tu conexión y consulta " +
                            "los datos antes de reintentar."

                _errorMensaje.value = mensajeError
                _mensaje.emit(mensajeError)

            } finally {

                _cargando.value = false
            }
        }
    }
}