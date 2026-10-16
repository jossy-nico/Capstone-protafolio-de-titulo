
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


class UsuarioViewModel : ViewModel() {

    // APIs
    private val usuarioApi = RetroFistCliente.usuarioApi
    private val clienteApi = RetroFistCliente.clienteApi


    // Usuario seleccionado para edición
    private val _usuarioSeleccionado = MutableStateFlow<Usuario?>(null)
    val usuarioSeleccionado: StateFlow<Usuario?> = _usuarioSeleccionado.asStateFlow()

    private val _clienteSeleccionado = MutableStateFlow<Cliente?>(null)
    val clienteSeleccionado: StateFlow<Cliente?> = _clienteSeleccionado.asStateFlow()

    private val _usuarioClienteSeleccionado = MutableStateFlow<Usuario?>(null)
    val usuarioClienteSeleccionado: StateFlow<Usuario?> = _usuarioClienteSeleccionado.asStateFlow()

    // Estado simple de error / loading
    private val _errorMensaje = MutableStateFlow<String?>(null)
    val errorMensaje: StateFlow<String?> = _errorMensaje.asStateFlow()

    private val _mensaje = MutableSharedFlow<String>()
    val mensaje = _mensaje.asSharedFlow()

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()





    // Registrar un nuevo CLIENTE (como ya lo tenías)
    fun registrarCliente(
        nombre: String,
        apellido: String,
        correo: String,
        password: String,
        direccion: String,
        telefono: String
    ) {
        val nuevoUser = Usuario(
            idUsuario = null,
            nombre = nombre.trim(),
            apellido = apellido.trim(),
            correo = correo.trim().lowercase(),
            password = password,
            tipoUsuario = "CLIENTE"
        )

        viewModelScope.launch {
            _cargando.value = true
            _errorMensaje.value = null
            try {
                // 1) Crear usuario
                val respUser = usuarioApi.crearUsuario(nuevoUser)

                if (!respUser.isSuccessful) {
                    _errorMensaje.value =
                        "Error al registrar usuario (${respUser.code()})"
                    _mensaje.emit("Error al registrar usuario (${respUser.code()})")
                    return@launch
                }

                val usuarioCreado = respUser.body()
                if (usuarioCreado?.idUsuario == null) {
                    _errorMensaje.value = "Usuario creado sin idUsuario"
                    _mensaje.emit("Usuario creado sin idUsuario")
                    return@launch
                }

                // 2) Crear cliente usando el idUsuario
                val clienteReq = Cliente(
                    usuarioId = null,
                    usuario = Usuario(
                        idUsuario = usuarioCreado.idUsuario,
                        nombre = "",
                        apellido = "",
                        correo = "",
                        password = "",
                        tipoUsuario = ""
                    ),
                    telefono = telefono.trim(),
                    direccion = direccion.trim()
                )

                val respCliente = clienteApi.crearCliente(clienteReq)

                if (!respCliente.isSuccessful) {
                    _errorMensaje.value =
                        "Error al registrar cliente (${respCliente.code()})"
                    _mensaje.emit("Error al registrar cliente (${respCliente.code()})")
                    return@launch
                }

                _mensaje.emit("Cliente registrado correctamente")

            } catch (e: Exception) {
                _errorMensaje.value = "Error de red: ${e.message}"
                _mensaje.emit("Error de red: ${e.message}")
            } finally {
                _cargando.value = false
            }
        }
    }

    //Actualizar cliente
    fun cargarDatosCliente(usuarioId: Long){
        viewModelScope.launch {
            _cargando.value = true
            _errorMensaje.value = null
            try {
                val respUser = usuarioApi.getUsuario(usuarioId)
                val respCliente = clienteApi.obtenerCliente(usuarioId)

                if (!respUser.isSuccessful){
                    _errorMensaje.value = "No se puede cargar los datos del usuario (${respUser.code()})"
                    _mensaje.emit("No se pudo cargar los datos del usuario (${respUser.code()})")
                    return@launch
                }

                if (!respCliente.isSuccessful){
                    _errorMensaje.value =" No se pueden cargar los datos del cliente (${respCliente.code()})"
                    _mensaje.emit(" No se pueden cargar los datos del cliente (${respCliente.code()})")
                    return@launch
                }
                _usuarioSeleccionado.value = respUser.body()
                _clienteSeleccionado.value = respCliente.body()
            } catch (e: Exception){
                _errorMensaje.value = "Error de red: ${e.message}"
                _mensaje.emit("Error de red: ${e.message}")
            } finally {
                _cargando.value = false
            }
        }
    }

    fun actualizarPerfilCliente(
        usuarioId: Long,
        nombre: String,
        apellido: String,
        correo: String,
        password: String,
        direccion: String,
        telefono: String
    ) {
        viewModelScope.launch {
            _cargando.value = true
            _errorMensaje.value = null
            try {
                // Validaciones mínimas
                if (nombre.isBlank() || apellido.isBlank() || correo.isBlank() || password.isBlank()) {
                    _mensaje.emit("Nombre, apellido, correo y contraseña son obligatorios")
                    return@launch
                }
                if (!correo.contains("@")) {
                    _mensaje.emit("Correo inválido")
                    return@launch
                }

                // 1) Actualizar USUARIO (forzar CLIENTE)
                val usuarioReq = Usuario(
                    idUsuario = usuarioId,
                    nombre = nombre.trim(),
                    apellido = apellido.trim(),
                    correo = correo.trim().lowercase(),
                    password = password,
                    tipoUsuario = "CLIENTE"
                )

                val respUser = usuarioApi.actualizarUsuario(usuarioId, usuarioReq)
                if (!respUser.isSuccessful) {
                    _errorMensaje.value = "Error al actualizar usuario (${respUser.code()})"
                    _mensaje.emit("Error al actualizar usuario (${respUser.code()})")
                    return@launch
                }

                // 2) Actualizar CLIENTE
                val clienteReq = Cliente(
                    usuarioId = usuarioId,
                    usuario = null, // en PUT normalmente no se necesita el objeto usuario
                    telefono = telefono.trim(),
                    direccion = direccion.trim()
                )

                val respCliente = clienteApi.actualizarCliente(usuarioId, clienteReq)
                if (!respCliente.isSuccessful) {
                    _errorMensaje.value = "Error al actualizar cliente (${respCliente.code()})"
                    _mensaje.emit("Error al actualizar cliente (${respCliente.code()})")
                    return@launch
                }

                // refrescar estados locales
                _usuarioClienteSeleccionado.value = respUser.body() ?: usuarioReq
                _clienteSeleccionado.value = respCliente.body() ?: clienteReq

                _mensaje.emit("Datos actualizados correctamente ✅")

            } catch (e: Exception) {
                _errorMensaje.value = "Error de red: ${e.message}"
                _mensaje.emit("Error de red: ${e.message}")
            } finally {
                _cargando.value = false
            }
        }
    }
}