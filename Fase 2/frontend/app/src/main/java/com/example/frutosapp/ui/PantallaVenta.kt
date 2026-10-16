package com.example.frutosapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.frutosapp.CarritoViewModel
import com.example.frutosapp.VentaViewModel
import java.text.NumberFormat
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaVenta(
    carritoViewModel: CarritoViewModel,
    ventaViewModel: VentaViewModel,
    usuarioId: Long,
    onVentaExitosa: () -> Unit,
    onNavigateBack: () -> Unit
) {
    // Items del carrito
    val items by carritoViewModel.itemsCarrito.collectAsStateWithLifecycle()

    // Estados del ViewModel de ventas
    val ventaCreada by ventaViewModel.ventaCreada.collectAsStateWithLifecycle()
    val cargando by ventaViewModel.cargando.collectAsStateWithLifecycle()
    val errorMensaje by ventaViewModel.errorMensaje.collectAsStateWithLifecycle()

    val total = items.sumOf { (it.carritoCompra.cantidad / 1000.0) * it.producto.precioKilo }
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("es", "CL"))

    // Cuando la venta se crea con éxito:
    // vaciamos el carrito y disparamos la navegación
    LaunchedEffect(ventaCreada) {
        if (ventaCreada != null) {
            carritoViewModel.vaciarCarrito()
            onVentaExitosa()
        }
    }

    LaunchedEffect(errorMensaje) {
        if (errorMensaje?.contains("Stock insuficiente", ignoreCase = true) == true) {
            carritoViewModel.cargarCarrito()
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Confirmar Compra") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver al carrito"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No hay productos para comprar.")
                }
            } else {
                // Lista de productos del resumen
                LazyColumn(
                    modifier = Modifier
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "Resumen de tu pedido:",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    items(
                        items= items,
                        key = { it.carritoCompra.id.productoId }
                    ) { itemConProducto ->
                        Card(
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val imageUrl = itemConProducto.producto.imageUrl?.takeIf { it.isNotBlank() }

                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = itemConProducto.producto.nomProducto,
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )


                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = itemConProducto.producto.nomProducto,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Cantidad: ${formatPeso(itemConProducto.carritoCompra.cantidad)}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                Text(
                                    text = currencyFormatter.format(
                                        (itemConProducto.carritoCompra.cantidad / 1000.0 )* itemConProducto.producto.precioKilo
                                    ),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Total, mensajes y botón de pago
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total a Pagar:",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = currencyFormatter.format(total),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Mostrar error si hay
                    if (errorMensaje != null) {
                        Text(
                            text = errorMensaje ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Loader pequeño si está cargando
                    if (cargando) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            Text("Procesando compra...")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            // Llamamos al backend para crear la venta
                            ventaViewModel.confirCompra(items, usuarioId)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = items.isNotEmpty() && !cargando
                    ) {
                        Text(
                            text = "CONFIRMAR Y PAGAR",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}