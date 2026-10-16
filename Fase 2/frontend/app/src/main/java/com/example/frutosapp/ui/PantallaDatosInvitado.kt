package com.example.frutosapp.ui

import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.frutosapp.InvitadoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDatosInvitado(
    invitadoViewModel: InvitadoViewModel,
    onVolver: () -> Unit,
    onContinuar: () -> Unit
) {
    val datos by invitadoViewModel.invitadoState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Datos de compra")
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = "Ingresa tus datos para continuar como invitado",
                style = MaterialTheme.typography.titleMedium
            )

            OutlinedTextField(
                value = datos.nombre,
                onValueChange = {
                    invitadoViewModel.actualizarNombre(it)
                },
                label = {
                    Text("Nombre")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = datos.apellido,
                onValueChange = {
                    invitadoViewModel.actualizarApellido(it)
                },
                label = {
                    Text("Apellido")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = datos.correo,
                onValueChange = {
                    invitadoViewModel.actualizarCorreo(it)
                },
                label = {
                    Text("Correo electrónico")
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = datos.telefono,
                onValueChange = {
                    invitadoViewModel.actualizarTelefono(it)
                },
                label = {
                    Text("Teléfono")
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = datos.direccion,
                onValueChange = {
                    invitadoViewModel.actualizarDireccion(it)
                },
                label = {
                    Text("Dirección de entrega")
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    when {
                        !invitadoViewModel.datosValidos() -> {
                            Toast.makeText(
                                context,
                                "Todos los campos son obligatorios",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        !Patterns.EMAIL_ADDRESS.matcher(datos.correo).matches() -> {
                            Toast.makeText(
                                context,
                                "Ingresa un correo válido",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        else -> {
                            onContinuar()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continuar")
            }
        }
    }
}