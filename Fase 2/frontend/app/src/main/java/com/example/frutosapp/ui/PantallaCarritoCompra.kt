package com.example.frutosapp.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.frutosapp.CarritoViewModel
import com.example.frutosapp.model.ItemCarritoConProducto
import kotlinx.coroutines.launch



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaCarritoCompra(
    vm: CarritoViewModel,
    usuarioId: Long,
    onVolver: () -> Unit,
    onIrADetalle: () -> Unit
) {
    val items by vm.itemsCarrito.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Mensajes del carrito
    LaunchedEffect(Unit) {
        vm.mensaje.collect { msg ->
            scope.launch {
                snackbarHostState.showSnackbar(msg)
            }
        }
    }

    val total = remember(items) {
        items.sumOf { (it.carritoCompra.cantidad / 1000.0) * it.producto.precioKilo}
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Carrito de compras") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Tu carrito está vacío")
            }
        } else {
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
                        items,
                        key = { item -> item.carritoCompra.id.productoId }
                    ) { item ->
                        ItemCarritoCard(
                            item = item,
                            onAumentar = { vm.aumentarCantidad(item) },
                            onDisminuir = { vm.disminuirCantidad(item) },
                            onEliminar = { vm.eliminarItem(item) },
                            mostrarControles = true
                        )
                    }
                }

                Divider()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total:", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "$${"%.0f".format(total)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { vm.vaciarCarrito() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Delete, contentDescription = "Vaciar carrito")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Vaciar carrito")
                        }

                        Button(
                            onClick = {
                                val itemsActuales = vm.itemsCarrito.value

                                if (itemsActuales.isEmpty()) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            "El carrito está vacío."
                                        )
                                    }
                                    return@Button
                                }

                                val sinStock = itemsActuales.firstOrNull {
                                    it.carritoCompra.cantidad > it.producto.stockGramos
                                }

                                if (sinStock != null) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            "Stock insuficiente en ${sinStock.producto.nomProducto}. " +
                                                    "Disponible: ${formatPeso(sinStock.producto.stockGramos)}"
                                        )
                                    }
                                    return@Button
                                }

                                onIrADetalle()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Confirmar compra")
                        }
                    }
                }
            }
        }
    }
}
fun formatPeso(gramos: Int): String {
    return if (gramos >= 1000) {
        val kg = gramos / 1000.0
        "${"%.2f".format(kg)} kg"
    } else {
        "$gramos g"
    }
}

@Composable
fun ItemCarritoCard(
    item: ItemCarritoConProducto,
    onAumentar: () -> Unit,
    onDisminuir: () -> Unit,
    onEliminar: () -> Unit,
    mostrarControles: Boolean = true
) {
    val producto = item.producto
    val cantidad = item.carritoCompra.cantidad

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Imagen del producto
            val imageUrl = producto.imageUrl?.takeIf { it.isNotBlank() }

            AsyncImage(
                model = imageUrl,
                contentDescription = producto.nomProducto,
                modifier = Modifier.size(80.dp),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(android.R.drawable.ic_menu_report_image),
                error = painterResource(android.R.drawable.ic_menu_report_image)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = producto.nomProducto,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Precio kilo: $${"%,.0f".format(producto.precioKilo)}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                val subtotal = (cantidad / 1000.0) * producto.precioKilo
                Text(
                    text = "Subtotal: $${"%.0f".format(subtotal)}",
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 👇 SOLO se muestran los controles si mostrarControles == true
                if (mostrarControles) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Button(
                                onClick = onDisminuir,
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text("-")
                            }

                            Text(
                                text = formatPeso(cantidad),
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )

                            Button(
                                onClick = onAumentar,
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text("+")
                            }
                        }

                        IconButton(onClick = onEliminar) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Eliminar"
                            )
                        }
                    }
                }
            }
        }
    }
}
