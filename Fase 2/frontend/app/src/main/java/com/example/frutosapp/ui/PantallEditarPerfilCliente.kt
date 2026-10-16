package com.example.frutosapp.ui

import android.widget.Toast
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.frutosapp.UsuarioViewModel
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaEditarPerfilCliente(
    vm: UsuarioViewModel,
    usuarioId: Long,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    val cargando by vm.cargando.collectAsStateWithLifecycle()
    val usuario by vm.usuarioSeleccionado.collectAsStateWithLifecycle()
    val cliente by vm.clienteSeleccionado.collectAsStateWithLifecycle()

    // campos editables
    var nombre by remember { mutableStateOf("") }
    var apellido by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var direccion by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }

    // 1) Cargar al entrar
    LaunchedEffect(usuarioId) {
        if (usuarioId > 0) vm.cargarDatosCliente(usuarioId)
    }

    // 2) Poblar inputs cuando lleguen datos (solo si vienen no vacíos)
    var inicializado by remember { mutableStateOf(false) }
    LaunchedEffect(usuario, cliente) {
        if (!inicializado && (usuario != null || cliente != null)) {
            usuario?.let {
                nombre = it.nombre
                apellido = it.apellido
                correo = it.correo
                password = it.password
            }
            cliente?.let {
                direccion = it.direccion
                telefono = it.telefono
            }
            inicializado = true
        }
    }

    // 3) Escuchar mensajes del VM y mostrar Toast
    LaunchedEffect(Unit) {
        vm.mensaje.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar mis datos") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "volver"
                        )
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Text("Datos Personales", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = apellido,
                onValueChange = { apellido = it },
                label = { Text("Apellido") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(6.dp))

            Text("Datos de la Cuenta", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = correo,
                onValueChange = { correo = it },
                label = { Text("Correo") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            // Password editable
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(6.dp))

            Text("Datos de Envío", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = direccion,
                onValueChange = { direccion = it },
                label = { Text("Dirección") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = telefono,
                onValueChange = { telefono = it },
                label = { Text("Teléfono") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    vm.actualizarPerfilCliente(
                        usuarioId = usuarioId,
                        nombre = nombre,
                        apellido = apellido,
                        correo = correo,
                        password = password,
                        direccion = direccion,
                        telefono = telefono
                    )
                },
                enabled = !cargando,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (cargando) "Guardando..." else "Guardar cambios")
            }
        }
    }
}