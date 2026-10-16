package com.example.frutosapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.getValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.frutosapp.CarritoViewModel
import com.example.frutosapp.VentaViewModel
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.items
import com.example.frutosapp.InvitadoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDetalleCompra(
    carritoVM: CarritoViewModel,
    ventaViewModel: VentaViewModel,
    invitadoViewModel: InvitadoViewModel,
    usuarioId: Long,
    onVolver: () -> Unit,
    onIrAPago: () -> Unit
) {
    val items by carritoVM.itemsCarrito.collectAsStateWithLifecycle()
    val ventaCreada by ventaViewModel.ventaCreada.collectAsStateWithLifecycle()
    val cargando by ventaViewModel.cargando.collectAsStateWithLifecycle()
    val errorVenta by ventaViewModel.errorMensaje.collectAsStateWithLifecycle()
    val datosInvitado by invitadoViewModel.invitadoState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val total = remember(items) {
        items.sumOf {(it.carritoCompra.cantidad / 1000.0) * it.producto.precioKilo}
    }

    // LIMPIAR LA VENTA AL ENTRAR (muy importante)
    LaunchedEffect(Unit) {
        ventaViewModel.limpiarVentaCreada()
    }

    // Mostrar errores de la API
    LaunchedEffect(errorVenta) {
        errorVenta?.let { msg ->
            scope.launch { snackbarHostState.showSnackbar(msg) }
        }
    }

    // Cuando la venta se crea OK → ir a pantalla de pago
    LaunchedEffect(ventaCreada) {
        ventaCreada?.let {
            onIrAPago()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de la compra") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(
                    items = items,
                    key = { it.carritoCompra.id.productoId }
                ) { item ->
                    ItemCarritoCard(
                        item = item,
                        onAumentar = {},
                        onDisminuir = {},
                        onEliminar = {},
                        mostrarControles = false
                    )
                }
            }

            Divider()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Total a pagar: $${"%.0f".format(total)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (!cargando && items.isNotEmpty()) {
                            if (usuarioId > 0) {
                                ventaViewModel.confirCompra(
                                    itemsVenta = items,
                                    usuarioId = usuarioId
                                )
                            } else {
                                ventaViewModel.confirmarCompraInvitado(
                                    itemsVenta = items,
                                    datosInvitado = datosInvitado
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !cargando
                ) {
                    if (cargando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Creando venta...")
                    } else {
                        Text("Confirmar detalle")
                    }
                }
            }
        }
    }
}