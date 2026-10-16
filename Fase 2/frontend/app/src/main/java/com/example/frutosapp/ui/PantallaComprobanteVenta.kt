package com.example.frutosapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.frutosapp.VentaViewModel
import java.text.NumberFormat
import java.util.Locale
import com.example.frutosapp.ProductoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaComprobanteVenta(
    ventaViewModel: VentaViewModel,
    productoViewModel: ProductoViewModel,
    onNavigateBack: () -> Unit,
    onCalificarProductos: (ventaId: Long, productoId: Long) -> Unit
) {
    val venta = ventaViewModel.ventaSeleccionada.collectAsStateWithLifecycle().value
    val detalles = ventaViewModel.detallesVenta.collectAsStateWithLifecycle().value
    val productos = productoViewModel.productos.collectAsStateWithLifecycle().value
    val cargando = ventaViewModel.cargando.collectAsStateWithLifecycle().value
    val error = ventaViewModel.errorMensaje.collectAsStateWithLifecycle().value

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("es", "CL")) }

    // Cuando se abre la pantalla, cargamos los detalles de la venta seleccionada
    LaunchedEffect(venta?.idVenta) {
        val idVenta = venta?.idVenta
        if (idVenta != null) {
            ventaViewModel.cargarDetallesVenta(idVenta)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Comprobante de compra") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (venta == null) {
                Text(
                    text = "No hay venta seleccionada.",
                    modifier = Modifier.align(Alignment.Center),
                    style = MaterialTheme.typography.bodyLarge
                )
                return@Box
            }

            when {
                cargando -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                error != null -> {
                    Text(
                        text = error,
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        // Resumen general de la venta
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "Número de boleta: #${venta.idVenta}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                if (venta.clienteId != null) {
                                    Text(
                                        text = "Cliente ID: ${venta.clienteId}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                } else {
                                    Text(
                                        text = "Cliente: ${venta.nombreInvitado.orEmpty()} ${venta.apellidoInvitado.orEmpty()}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                    Text(
                                        text = "Correo: ${venta.correoInvitado.orEmpty()}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                    Text(
                                        text = "Teléfono: ${venta.telefonoInvitado.orEmpty()}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )

                                    Text(
                                        text = "Dirección: ${venta.direccionInvitado.orEmpty()}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Total: ${currencyFormatter.format(venta.totalVenta)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Detalle de productos:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(detalles) { det ->

                                val subtotalDetalle =
                                    (det.cantidad / 1000.0) * det.precioUnitario

                                val producto = productos.find {
                                    it.idProductos?.toLong() == det.id.productoId
                                }

                                val nombreProducto = producto?.nomProducto ?: "Producto no encontrado"

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            text = "Código del producto: ${det.id.productoId}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Text(
                                            text = "Producto: $nombreProducto",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Text(
                                            text = "Cantidad: ${formatPeso(det.cantidad)}",
                                            style = MaterialTheme.typography.bodyMedium
                                        )

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Text(
                                            text = "Precio unitario: ${
                                                currencyFormatter.format(det.precioUnitario)
                                            }",
                                            style = MaterialTheme.typography.bodyMedium
                                        )

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Text(
                                            text = "Subtotal: ${
                                                currencyFormatter.format(subtotalDetalle)
                                            }",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )

                                        if (venta.clienteId != null) {
                                            Spacer(modifier = Modifier.height(8.dp))

                                            androidx.compose.material3.Button(
                                                onClick = {
                                                    onCalificarProductos(
                                                        venta.idVenta,
                                                        det.id.productoId
                                                    )
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("Calificar producto")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}