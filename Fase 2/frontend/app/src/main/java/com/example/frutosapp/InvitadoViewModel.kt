package com.example.frutosapp

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class InvitadoState(
    val nombre: String = "",
    val apellido: String = "",
    val correo: String = "",
    val telefono: String = "",
    val direccion: String = ""
)

class InvitadoViewModel : ViewModel() {

    private val _invitadoState = MutableStateFlow(InvitadoState())
    val invitadoState: StateFlow<InvitadoState> = _invitadoState.asStateFlow()

    fun actualizarNombre(nombre: String) {
        _invitadoState.value = _invitadoState.value.copy(nombre = nombre)
    }

    fun actualizarApellido(apellido: String) {
        _invitadoState.value = _invitadoState.value.copy(apellido = apellido)
    }

    fun actualizarCorreo(correo: String) {
        _invitadoState.value = _invitadoState.value.copy(correo = correo)
    }

    fun actualizarTelefono(telefono: String) {
        _invitadoState.value = _invitadoState.value.copy(telefono = telefono)
    }

    fun actualizarDireccion(direccion: String) {
        _invitadoState.value = _invitadoState.value.copy(direccion = direccion)
    }

    fun guardarDatos(
        nombre: String,
        apellido: String,
        correo: String,
        telefono: String,
        direccion: String
    ) {
        _invitadoState.value = InvitadoState(
            nombre = nombre.trim(),
            apellido = apellido.trim(),
            correo = correo.trim(),
            telefono = telefono.trim(),
            direccion = direccion.trim()
        )
    }

    fun datosValidos(): Boolean {
        val datos = _invitadoState.value

        return datos.nombre.isNotBlank() &&
                datos.apellido.isNotBlank() &&
                datos.correo.isNotBlank() &&
                datos.telefono.isNotBlank() &&
                datos.direccion.isNotBlank()
    }

    fun limpiarDatos() {
        _invitadoState.value = InvitadoState()
    }
}