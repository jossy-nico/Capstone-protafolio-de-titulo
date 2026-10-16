package com.example.frutosapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.frutosapp.ProductoViewModel
import com.example.frutosapp.model.ItemCarritoConProducto
import com.example.frutosapp.model.Producto
import com.example.frutosapp.viewmodel.FavoritoViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaFavoritos(
    vm: ProductoViewModel,
    favoritoViewModel: FavoritoViewModel,
    usuarioId: Long,
    itemsDelCarrito: List<ItemCarritoConProducto>,
    onVolver: () -> Unit,
    onNavigateToDetalleProducto: (Producto) -> Unit,
    onAddToCartClicked: (Producto) -> Unit,
    onAumentarCantidad: (ItemCarritoConProducto) -> Unit,
    onDisminuirCantidad: (ItemCarritoConProducto) -> Unit,
    onNavigateToCarrito: () -> Unit
) {

    val productos by vm.productosBase.collectAsStateWithLifecycle()
    val favoritos by favoritoViewModel.favoritos.collectAsStateWithLifecycle()

    val productosFavoritos = remember(productos, favoritos) {
        productos.filter { producto ->
            producto.idProductos != null &&
                    favoritos.contains(producto.idProductos)
        }
    }

    val currency = remember {
        NumberFormat.getCurrencyInstance(Locale("es", "CL"))
    }

    val cantidadCarrito = remember(itemsDelCarrito) {
        itemsDelCarrito.sumOf {
            it.carritoCompra.cantidad / 250
        }
    }

    LaunchedEffect(usuarioId) {
        favoritoViewModel.cargarFavoritos(usuarioId)
    }

    Scaffold(

        topBar = {

            TopAppBar(

                title = {
                    Text("Mis favoritos")
                },

                navigationIcon = {

                    IconButton(
                        onClick = onVolver
                    ) {

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },

                actions = {

                    BadgedBox(
                        badge = {

                            if (cantidadCarrito > 0) {

                                Badge {
                                    Text(cantidadCarrito.toString())
                                }
                            }
                        }
                    ) {

                        IconButton(
                            onClick = onNavigateToCarrito
                        ) {

                            Icon(
                                imageVector = Icons.Filled.ShoppingCart,
                                contentDescription = "Carrito"
                            )
                        }
                    }

                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )
                }
            )
        }

    ) { padding ->

        if (productosFavoritos.isEmpty()) {

            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = Color(0xFF66BB6A),
                        modifier = Modifier.size(54.dp)
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

                    Text(
                        text = "Aún no tienes productos favoritos",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text = "Guarda los productos que más te gusten.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

        } else {

            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                items(
                    items = productosFavoritos,
                    key = { producto ->
                        producto.idProductos ?: 0
                    }
                ) { producto ->

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onNavigateToDetalleProducto(producto)
                            },
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 2.dp
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Box(
                                modifier = Modifier
                                    .width(110.dp)
                                    .height(90.dp)
                            ) {

                                AsyncImage(
                                    model = producto.imageUrl,
                                    contentDescription = producto.nomProducto,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(
                                            RoundedCornerShape(10.dp)
                                        ),
                                    contentScale = ContentScale.Crop
                                )

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(27.dp)
                                        .background(
                                            Color.Black,
                                            CircleShape
                                        )
                                        .clickable {

                                            producto.idProductos?.let { idProducto ->

                                                favoritoViewModel.alternarFavorito(
                                                    usuarioId,
                                                    idProducto
                                                )
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {

                                    Icon(
                                        imageVector = Icons.Filled.Favorite,
                                        contentDescription = "Quitar de favoritos",
                                        tint = Color(0xFF66BB6A),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(
                                modifier = Modifier.width(12.dp)
                            )

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {

                                Text(
                                    text = producto.nomProducto,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight =
                                        androidx.compose.ui.text.font.FontWeight.Bold,
                                    maxLines = 2
                                )

                                Spacer(
                                    modifier = Modifier.height(3.dp)
                                )

                                Text(
                                    text = "250g: ${
                                        currency.format(producto.precio250g)
                                    }",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight =
                                        androidx.compose.ui.text.font.FontWeight.Bold
                                )

                                Text(
                                    text = "Kilo: ${
                                        currency.format(producto.precioKilo)
                                    }",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF5F696F)
                                )

                                Text(
                                    text = "Stock: ${producto.stockGramos} g",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF5F696F)
                                )
                            }

                            Spacer(
                                modifier = Modifier.width(8.dp)
                            )

                            val itemEnCarrito = itemsDelCarrito.find { item ->
                                item.producto.idProductos == producto.idProductos
                            }

                            if (itemEnCarrito == null) {

                                Button(
                                    onClick = {
                                        onAddToCartClicked(producto)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(
                                        horizontal = 10.dp,
                                        vertical = 9.dp
                                    )
                                ) {

                                    Icon(
                                        imageVector = Icons.Filled.ShoppingCart,
                                        contentDescription = null,
                                        modifier = Modifier.size(17.dp)
                                    )

                                    Spacer(
                                        modifier = Modifier.width(4.dp)
                                    )

                                    Text(
                                        text = "Agregar",
                                        fontSize = 13.sp
                                    )
                                }

                            } else {

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {

                                        Button(
                                            onClick = {
                                                onDisminuirCantidad(itemEnCarrito)
                                            },
                                            modifier = Modifier.size(34.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("-")
                                        }

                                        Text(
                                            text = "${itemEnCarrito.carritoCompra.cantidad / 250}",
                                            modifier = Modifier.padding(horizontal = 6.dp),
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                        )

                                        Button(
                                            onClick = {
                                                onAumentarCantidad(itemEnCarrito)
                                            },
                                            modifier = Modifier.size(34.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("+")
                                        }
                                    }

                                    Text(
                                        text = "${itemEnCarrito.carritoCompra.cantidad} g",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}