package com.example.frutosapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.frutosapp.ResennaViewModel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaResenna(
    resennaViewModel: ResennaViewModel,
    clienteId: Long,
    ventaId: Long,
    productoId: Long,
    onNavigateBack: () -> Unit
) {
    val resennaExistente by
    resennaViewModel.resennaCompra.collectAsStateWithLifecycle()

    val cargando by
    resennaViewModel.cargando.collectAsStateWithLifecycle()

    val mensajeError by
    resennaViewModel.mensajeError.collectAsStateWithLifecycle()

    val mensajeExito by
    resennaViewModel.mensajeExito.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    var estrellas by remember {
        mutableIntStateOf(0)
    }

    var comentario by remember {
        mutableStateOf("")
    }

    var datosCargados by remember {
        mutableStateOf(false)
    }

    var mostrarConfirmacionEliminar by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(clienteId, ventaId, productoId) {
        datosCargados = false
        resennaViewModel.limpiarMensajes()
        resennaViewModel.limpiarResennaCompra()

        resennaViewModel.consultarResennaCompra(
            clienteId = clienteId,
            ventaId = ventaId,
            productoId = productoId
        )
    }

    LaunchedEffect(resennaExistente) {
        val resenna = resennaExistente

        if (resenna != null && !datosCargados) {
            estrellas = resenna.estrellas
            comentario = resenna.comentario.orEmpty()
            datosCargados = true
        }
    }

    LaunchedEffect(mensajeError) {
        mensajeError?.let {
            snackbarHostState.showSnackbar(it)
            resennaViewModel.limpiarMensajes()
        }
    }

    LaunchedEffect(mensajeExito) {
        mensajeExito?.let { mensaje ->
            snackbarHostState.showSnackbar(mensaje)
            resennaViewModel.limpiarMensajes()
            onNavigateBack()
        }
    }

    LaunchedEffect(mensajeExito) {
        mensajeExito?.let { mensaje ->

            if (mensaje.contains("eliminada", ignoreCase = true)) {
                estrellas = 0
                comentario = ""
                datosCargados = false
            }

            snackbarHostState.showSnackbar(mensaje)

            resennaViewModel.limpiarMensajes()

            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (resennaExistente == null) {
                            "Calificar producto"
                        } else {
                            "Editar reseña"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector =
                                Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Selecciona una calificación",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                for (valor in 1..5) {
                    Icon(
                        imageVector = if (valor <= estrellas) {
                            Icons.Filled.Star
                        } else {
                            Icons.Outlined.Star
                        },
                        contentDescription = "$valor estrellas",
                        tint = if (valor <= estrellas) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        modifier = Modifier
                            .padding(4.dp)
                            .clickable {
                                estrellas = valor
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = comentario,
                onValueChange = {
                    if (it.length <= 1000) {
                        comentario = it
                    }
                },
                label = {
                    Text("Comentario")
                },
                supportingText = {
                    Text("${comentario.length}/1000")
                },
                minLines = 4,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    val existente = resennaExistente

                    if (existente == null) {
                        resennaViewModel.crearResenna(
                            clienteId = clienteId,
                            ventaId = ventaId,
                            productoId = productoId,
                            estrellas = estrellas,
                            comentario = comentario
                                .trim()
                                .takeIf { it.isNotEmpty() }
                        )
                    } else {
                        resennaViewModel.actualizarResenna(
                            idResenna = existente.idResenna,
                            clienteId = clienteId,
                            ventaId = ventaId,
                            productoId = productoId,
                            estrellas = estrellas,
                            comentario = comentario
                                .trim()
                                .takeIf { it.isNotEmpty() }
                        )
                    }
                },
                enabled = !cargando && estrellas in 1..5,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (cargando) {
                    CircularProgressIndicator()
                } else {
                    Text(
                        if (resennaExistente == null) {
                            "Publicar reseña"
                        } else {
                            "Guardar cambios"
                        }
                    )
                }
            }
            if (resennaExistente != null) {

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        mostrarConfirmacionEliminar = true
                    },
                    enabled = !cargando,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Eliminar reseña")
                }
            }
        }
        if (mostrarConfirmacionEliminar) {

            AlertDialog(
                onDismissRequest = {
                    mostrarConfirmacionEliminar = false
                },
                title = {
                    Text("Eliminar reseña")
                },
                text = {
                    Text(
                        "¿Estás seguro de que deseas eliminar esta reseña? " +
                                "Esta acción no se puede deshacer."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val idResenna =
                                resennaExistente?.idResenna

                            if (idResenna != null) {
                                mostrarConfirmacionEliminar = false

                                resennaViewModel.eliminarResenna(
                                    idResenna
                                )
                            }
                        }
                    ) {
                        Text("Eliminar")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            mostrarConfirmacionEliminar = false
                        }
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}