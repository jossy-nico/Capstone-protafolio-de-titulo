package com.example.frutosapp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.frutosapp.ProductoViewModel
import com.example.frutosapp.model.ItemCarritoConProducto
import com.example.frutosapp.model.Producto
import com.example.frutosapp.ui.components.BarraNavegacionInferior

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaProducto(
    vm: ProductoViewModel = viewModel(),
    loginOk: Boolean,
    itemsDelCarrito: List<ItemCarritoConProducto>,
    onLoginClicked: () -> Unit,
    onRegisterClicked: () -> Unit,
    onLogoutClicked: () -> Unit,
    onAddToCartClicked: (Producto) -> Unit,
    onNavigateToCarrito: () -> Unit,

    onNavigateToInicio: () -> Unit,
    onNavigateToProductos: () -> Unit,
    onNavigateToCategorias: () -> Unit,
    onNavigateToCuenta: () -> Unit
) {
    val productos by vm.productos.collectAsStateWithLifecycle()
    val categorias by vm.categorias.collectAsStateWithLifecycle()
    val textoBusqueda by vm.textoBusqueda.collectAsStateWithLifecycle()
    val categoriaSet by vm.categoriaSelect.collectAsStateWithLifecycle()

    var expandido by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Frutos Secos") },
                actions = {

                    val cantidadItems = itemsDelCarrito.sumOf {
                        it.carritoCompra.cantidad / 250
                    }

                    BadgedBox(
                        badge = {
                            if (cantidadItems > 0) {
                                Badge {
                                    Text(cantidadItems.toString())
                                }
                            }
                        }
                    ) {
                        IconButton(onClick = onNavigateToCarrito) {
                            Icon(
                                Icons.Filled.ShoppingCart,
                                contentDescription = "Carrito"
                            )
                        }
                    }

                    if (loginOk) {
                        IconButton(onClick = onLogoutClicked) {
                            Icon(
                                Icons.Filled.ExitToApp,
                                contentDescription = "Cerrar sesión"
                            )
                        }
                    } else {
                        TextButton(onClick = onLoginClicked) {
                            Text("Iniciar sesión")
                        }

                        TextButton(onClick = onRegisterClicked) {
                            Text("Registrar")
                        }
                    }
                }
            )
        },
        bottomBar = {
            BarraNavegacionInferior(
                rutaActual = "productos",
                onInicioClick = onNavigateToInicio,
                onProductosClick = onNavigateToProductos,
                onCategoriasClick = onNavigateToCategorias,
                onCuentaClick = onNavigateToCuenta
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Búsqueda
            OutlinedTextField(
                value = textoBusqueda,
                onValueChange = { vm.onBusquedaChange(it) },
                label = { Text("Buscar producto...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Buscar") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Filtro por categoría
            ExposedDropdownMenuBox(
                expanded = expandido,
                onExpandedChange = { expandido = !expandido }
            ) {
                OutlinedTextField(
                    value = categoriaSet?.nomCategoria ?: "Todas las categorías",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expandido,
                    onDismissRequest = { expandido = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Todas las categorías") },
                        onClick = {
                            vm.onCategoriaSelected(null)
                            expandido = false
                        }
                    )
                    categorias.forEach { categoria ->
                        DropdownMenuItem(
                            text = { Text(categoria.nomCategoria) },
                            onClick = {
                                vm.onCategoriaSelected(categoria)
                                expandido = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Estados de carga / vacío
            if (productos.isEmpty() && textoBusqueda.isBlank() && categoriaSet == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (productos.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No se encontraron productos para tu búsqueda.")
                }
            }

            AnimatedVisibility(visible = productos.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(productos, key = { it.idProductos ?: 0 }) { producto ->
                        Card(elevation = CardDefaults.cardElevation(2.dp)) {
                            Column(modifier = Modifier.padding(12.dp)) {

                                val imageUrl = producto.imageUrl?.takeIf { it.isNotBlank() }

                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = producto.nomProducto,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                        .align(Alignment.CenterHorizontally),
                                    contentScale = ContentScale.Crop,
                                    placeholder = painterResource(android.R.drawable.ic_menu_report_image),
                                    error = painterResource(android.R.drawable.ic_menu_report_image)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = producto.nomProducto,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val formatoMoneda = remember {
                                    java.text.NumberFormat.getCurrencyInstance(
                                        java.util.Locale("es", "CL")
                                    )
                                }

                                val stockFormateado = if (producto.stockGramos >= 1000) {

                                    val kilos = producto.stockGramos / 1000.0

                                    val textoKilos = if (producto.stockGramos % 1000 == 0) {
                                        String.format(
                                            java.util.Locale("es", "CL"),
                                            "%.0f",
                                            kilos
                                        )
                                    } else {
                                        String.format(
                                            java.util.Locale("es", "CL"),
                                            "%.3f",
                                            kilos
                                        )
                                    }

                                    "$textoKilos kg"

                                } else {
                                    "${producto.stockGramos} g"
                                }

                                Text(
                                    text = "250 g: ${formatoMoneda.format(producto.precio250g)}",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Text(
                                    text = "Kilo: ${formatoMoneda.format(producto.precioKilo)}",
                                    style = MaterialTheme.typography.bodySmall
                                )

                                Text(
                                    text = "Stock: $stockFormateado",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = { onAddToCartClicked(producto) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Filled.ShoppingCart, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Agregar")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
