
package com.example.frutosapp.ui

import android.widget.Toast

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place

import androidx.compose.material3.*

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import com.example.frutosapp.UsuarioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaEditarPerfilCliente(
    vm: UsuarioViewModel,
    usuarioId: Long,
    onNavigateBack: () -> Unit,
    onCambiarPassword: () -> Unit
) {

    val context = LocalContext.current

    val cargando by vm.cargando.collectAsStateWithLifecycle()
    val usuario by vm.usuarioSeleccionado.collectAsStateWithLifecycle()
    val cliente by vm.clienteSeleccionado.collectAsStateWithLifecycle()

    // ==========================================
    // ESTADO DEL FORMULARIO
    // ==========================================

    var nombre by rememberSaveable(usuarioId) {
        mutableStateOf("")
    }

    var apellido by rememberSaveable(usuarioId) {
        mutableStateOf("")
    }

    var correo by rememberSaveable(usuarioId) {
        mutableStateOf("")
    }

    var telefono by rememberSaveable(usuarioId) {
        mutableStateOf("")
    }

    var direccion by rememberSaveable(usuarioId) {
        mutableStateOf("")
    }

    var inicializado by remember(usuarioId) {
        mutableStateOf(false)
    }

    var mostrarErrores by remember {
        mutableStateOf(false)
    }

    var nombreOriginal by remember(usuarioId) {
        mutableStateOf("")
    }

    var apellidoOriginal by remember(usuarioId) {
        mutableStateOf("")
    }

    var telefonoOriginal by remember(usuarioId) {
        mutableStateOf("")
    }

    var direccionOriginal by remember(usuarioId) {
        mutableStateOf("")
    }


    // ==========================================
    // CARGAR DATOS
    // ==========================================

    LaunchedEffect(usuarioId) {

        if (usuarioId > 0L) {
            vm.cargarDatosCliente(usuarioId)
        }
    }


    // ==========================================
    // CARGAR DATOS Y GUARDAR VALORES ORIGINALES
    // ==========================================

    LaunchedEffect(usuario, cliente, usuarioId) {

        val usuarioActual = usuario

        if (
            !inicializado &&
            usuarioActual != null &&
            usuarioActual.idUsuario == usuarioId &&
            cliente != null
        ) {

            // Cargar información del cliente
            nombre = usuarioActual.nombre
            apellido = usuarioActual.apellido
            correo = usuarioActual.correo

            direccion = cliente?.direccion.orEmpty()
            telefono = cliente?.telefono.orEmpty()

            // Guardar valores originales para
            // detectar modificaciones posteriores
            nombreOriginal = nombre.trim()
            apellidoOriginal = apellido.trim()
            telefonoOriginal = telefono.trim()
            direccionOriginal = direccion.trim()

            inicializado = true
        }
    }



    // ==========================================
    // ESCUCHAR RESULTADO DE ACTUALIZACIÓN
    // ==========================================

    LaunchedEffect(vm) {

        vm.mensaje.collect { msg ->

            // Actualizar valores originales
            // solo si el backend confirmó el guardado.
            if (
                msg == "Perfil actualizado correctamente." ||
                msg.startsWith("Los cambios se guardaron,")
            ) {

                nombreOriginal = nombre.trim()
                apellidoOriginal = apellido.trim()
                telefonoOriginal = telefono.trim()
                direccionOriginal = direccion.trim()
            }

            // Mostrar mensaje al cliente
            Toast.makeText(
                context,
                msg,
                Toast.LENGTH_LONG
            ).show()
        }
    }



    // ==========================================
    // VALIDACIONES DEL FORMULARIO
    // ==========================================

    val nombreValido =
        nombre.trim().length >= 2

    val apellidoValido =
        apellido.trim().length >= 2

    val telefonoValido =
        telefono.trim().matches(
            Regex("9\\d{8}")
        )

    val direccionValida =
        direccion.trim().isNotBlank()

    val formularioValido =
        nombreValido &&
                apellidoValido &&
                telefonoValido &&
                direccionValida

    val hayCambios =
        nombre.trim() != nombreOriginal ||
                apellido.trim() != apellidoOriginal ||
                telefono.trim() != telefonoOriginal ||
                direccion.trim() != direccionOriginal



    val puedeGuardar =
        inicializado &&
                !cargando &&
                formularioValido &&
                hayCambios &&
                usuarioId > 0L



    // ==========================================
    // INTERFAZ
    // ==========================================

    Scaffold(

        topBar = {

            TopAppBar(

                title = {
                    Text("Editar mi perfil")
                },

                navigationIcon = {

                    IconButton(
                        onClick = onNavigateBack
                    ) {

                        Icon(
                            imageVector =
                                Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }

    ) { padding ->

        Column(

            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),

            verticalArrangement =
                Arrangement.spacedBy(16.dp)

        ) {

            // ==================================
            // ENCABEZADO
            // ==================================

            Text(
                text = "Mi información",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Mantén actualizados tus datos " +
                        "personales y de contacto.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // ==================================
            // CARGANDO INFORMACIÓN
            // ==================================

            if (cargando && !inicializado) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {

                    CircularProgressIndicator()
                }
            }

            if (!cargando && !inicializado) {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {

                    Text(
                        text = "No se han cargado los datos " +
                                "del perfil. Verifica tu sesión " +
                                "o inténtalo nuevamente.",
                        modifier = Modifier.padding(16.dp)
                    )

                    TextButton(
                        onClick = {
                            vm.cargarDatosCliente(usuarioId)
                        },
                        enabled = !cargando && usuarioId > 0L
                    ) {
                        Text("Reintentar")
                    }
                }
            }

            // ==================================
            // DATOS PERSONALES
            // ==================================

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically,
                        horizontalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {

                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint =
                                MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "Datos personales",
                            style =
                                MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    OutlinedTextField(
                        value = nombre,
                        onValueChange = {
                            nombre = it
                        },
                        label = {
                            Text("Nombre *")
                        },
                        singleLine = true,
                        enabled = inicializado && !cargando,
                        isError =
                            mostrarErrores && !nombreValido,
                        supportingText = {

                            if (
                                mostrarErrores &&
                                !nombreValido
                            ) {
                                Text(
                                    "Ingresa al menos 2 caracteres."
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = apellido,
                        onValueChange = {
                            apellido = it
                        },
                        label = {
                            Text("Apellido *")
                        },
                        singleLine = true,
                        enabled = inicializado && !cargando,
                        isError =
                            mostrarErrores && !apellidoValido,
                        supportingText = {

                            if (
                                mostrarErrores &&
                                !apellidoValido
                            ) {
                                Text(
                                    "Ingresa al menos 2 caracteres."
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ==================================
            // CONTACTO Y ENVÍO
            // ==================================

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically,
                        horizontalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {

                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = null,
                            tint =
                                MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "Contacto y envío",
                            style =
                                MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    OutlinedTextField(
                        value = correo,
                        onValueChange = {},
                        label = {
                            Text("Correo electrónico")
                        },
                        readOnly = true,
                        enabled = inicializado,
                        singleLine = true,
                        supportingText = {
                            Text(
                                "El cambio de correo requiere " +
                                        "un proceso de verificación."
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = telefono,
                        onValueChange = {
                            telefono = it
                        },
                        label = {
                            Text("Teléfono")
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone
                        ),
                        singleLine = true,
                        enabled = inicializado && !cargando,
                        isError =
                            mostrarErrores && !telefonoValido,
                        supportingText = {

                            if (
                                mostrarErrores &&
                                !telefonoValido
                            ) {
                                Text(
                                    "Ingresa un teléfono válido."
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )


                    OutlinedTextField(
                        value = direccion,
                        onValueChange = {
                            direccion = it
                        },
                        label = {
                            Text("Dirección de envío *")
                        },
                        minLines = 1,
                        maxLines = 2,
                        enabled = inicializado && !cargando,
                        isError = mostrarErrores && !direccionValida,
                        supportingText = {
                            if (mostrarErrores && !direccionValida) {
                                Text("La dirección de envío es obligatoria.")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                }
            }

            // ==================================
            // SEGURIDAD DE LA CUENTA
            // ==================================

            Card(
                modifier = Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically,
                        horizontalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {

                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint =
                                MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = "Seguridad de la cuenta",
                            style =
                                MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = "El cambio de contraseña " +
                                "se realizará por separado " +
                                "para proteger tu cuenta.",
                        style =
                            MaterialTheme.typography.bodyMedium,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    TextButton(
                        onClick = onCambiarPassword
                    ) {
                        Text("Cambiar contraseña")
                    }

                }
            }

            // ==================================
            // GUARDAR CAMBIOS
            // ==================================

            Button(
                onClick = {

                    mostrarErrores = true

                    if (puedeGuardar) {

                        vm.actualizarPerfilCliente(
                            usuarioId = usuarioId,
                            nombre = nombre.trim(),
                            apellido = apellido.trim(),
                            correo = correo,
                            password = "",
                            direccion = direccion.trim(),
                            telefono = telefono.trim()
                        )
                    }
                },
                enabled = puedeGuardar,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {

                if (cargando) {

                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )

                } else {

                    Text("Guardar cambios")
                }
            }

            Text(
                text = "* Campos obligatorios",
                style = MaterialTheme.typography.bodySmall,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(
                modifier = Modifier.height(16.dp)
            )
        }
    }
}
