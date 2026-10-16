package com.example.frutosapp.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.frutosapp.model.ItemCarritoConProducto
import com.example.frutosapp.model.Producto
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import  android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.example.frutosapp.ResennaViewModel
import com.example.frutosapp.viewmodel.FavoritoViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PantallaDetalleProducto(
    producto: Producto,
    itemsDelCarrito: List<ItemCarritoConProducto>,
    loginOk: Boolean,
    onVolver: () -> Unit,
    onAddToCartClicked: (Producto) -> Unit,
    onAumentarCantidad: (ItemCarritoConProducto) -> Unit,
    onDisminuirCantidad: (ItemCarritoConProducto) -> Unit,
    onNavigateToCarrito: () -> Unit,
    usuarioId: Long?,
    favoritoViewModel: FavoritoViewModel,
    resennaViewModel: ResennaViewModel = viewModel()
) {
    val context = LocalContext.current
    val favoritos by favoritoViewModel.favoritos.collectAsStateWithLifecycle()

    val idProducto = producto.idProductos

    val esFavorito =
        idProducto != null && favoritos.contains(idProducto)

    val promedio by resennaViewModel.promedio.collectAsState()
    LaunchedEffect(producto.idProductos) {
        producto.idProductos?.let {
            resennaViewModel.cargarResennas(it.toLong())
        }
    }

    val resennas by resennaViewModel.resennas.collectAsState()
    val cargandoResennas by resennaViewModel.cargando.collectAsState()

    val itemEnCarrito = itemsDelCarrito.find {
        it.producto.idProductos == producto.idProductos
    }
    val cantidadCarrito = itemsDelCarrito.sumOf {
        it.carritoCompra.cantidad
    }

    val formatoMoneda = remember {
        java.text.NumberFormat.getCurrencyInstance(
            java.util.Locale("es", "CL")
        )
    }

    val stockDisponible = if (producto.stockGramos >= 1000) {

        val kilos = producto.stockGramos / 1000.0

        val kilosFormateados = if (producto.stockGramos % 1000 == 0) {
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

        "$kilosFormateados kg"

    } else {
        "${producto.stockGramos} g"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle producto") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    BadgedBox(
                        badge = {
                            if (cantidadCarrito > 0) {
                                Badge {
                                    Text((cantidadCarrito / 250).toString())
                                }
                            }
                        }
                    ) {
                        IconButton(onClick = onNavigateToCarrito) {
                            Icon(
                                Icons.Filled.ShoppingCart,
                                contentDescription = "Ver carrito"
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
            ) {

                AsyncImage(
                    model = producto.imageUrl,
                    contentDescription = producto.nomProducto,
                    modifier = Modifier
                        .fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Corazón Wishlist / Favoritos
                Box(
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.TopEnd)
                        .padding(10.dp)
                        .size(38.dp)
                        .background(
                            color = androidx.compose.ui.graphics.Color.Black,
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                        .clickable {

                            if (!loginOk || usuarioId == null) {

                                Toast.makeText(
                                    context,
                                    "Inicia sesión o crea una cuenta para guardar productos en favoritos.",
                                    Toast.LENGTH_SHORT
                                ).show()

                            } else if (idProducto != null) {

                                favoritoViewModel.alternarFavorito(
                                    usuarioId,
                                    idProducto
                                )
                            }
                        },
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {

                    Icon(
                        imageVector = if (esFavorito) {
                            Icons.Filled.Favorite
                        } else {
                            Icons.Outlined.FavoriteBorder
                        },
                        contentDescription = if (esFavorito) {
                            "Quitar de favoritos"
                        } else {
                            "Agregar a favoritos"
                        },
                        tint = if (esFavorito) {
                            androidx.compose.ui.graphics.Color(0xFF66BB6A)
                        } else {
                            androidx.compose.ui.graphics.Color.White
                        },
                        modifier = Modifier.size(27.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = producto.nomProducto,
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = if (resennas.isNotEmpty()) {
                    val promedioFormateado = String.format(
                        java.util.Locale("es", "CL"),
                        "%.1f",
                        promedio
                    )

                    "⭐ $promedioFormateado · ${resennas.size} ${
                        if (resennas.size == 1) {
                            "calificación"
                        } else {
                            "calificaciones"
                        }
                    }"
                } else {
                    "⭐ Sin calificaciones"
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "250g: ${formatoMoneda.format(producto.precio250g)}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Precio kilo: ${formatoMoneda.format(producto.precioKilo)}"
            )

            Text(
                text = "Stock disponible: $stockDisponible"
            )

            Spacer(Modifier.height(16.dp))

            if (itemEnCarrito == null) {
                androidx.compose.material3.Button(
                    onClick = {
                        onAddToCartClicked(producto)

                        Toast.makeText(
                            context,
                            "Producto agregado al carrito",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Agregar")
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    androidx.compose.material3.Button(
                        onClick = {
                            onDisminuirCantidad(itemEnCarrito)
                        }
                    ) {
                        Text("-")
                    }

                    Text(
                        text = "${itemEnCarrito.carritoCompra.cantidad} g",
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )

                    androidx.compose.material3.Button(
                        onClick = {
                            onAumentarCantidad(itemEnCarrito)
                        }
                    ) {
                        Text("+")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            SeccionDesplegable(
                titulo = "Descripción",
                contenido = producto.descripcion
            )

            SeccionDesplegable(
                titulo = "Ingredientes",
                contenido = producto.ingredientes
                    ?: "Sin información de ingredientes"
            )

            SeccionDesplegable(
                titulo = "Alérgenos",
                contenido = producto.alergenos
                    ?.takeIf { it.isNotBlank() }
                    ?: "Sin información de alérgenos"
            )

            SeccionDesplegable(
                titulo = "Tabla nutricional",
                contenido = producto.tablaNutricional ?: "Sin información nutricional"
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Opiniones de clientes",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            when {
                cargandoResennas -> {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier
                            .padding(16.dp)
                            .align(androidx.compose.ui.Alignment.CenterHorizontally)
                    )
                }

                resennas.isEmpty() -> {
                    Text(
                        text = "Este producto todavía no tiene comentarios.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                else -> {
                    resennas.forEach { resenna ->
                        TarjetaResenna(resenna)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

    }
}
@Composable
fun SeccionDesplegable(
    titulo: String,
    contenido: String
) {
    var expandido by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expandido = !expandido }
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium
            )

            Icon(
                imageVector = if (expandido) {
                    Icons.Filled.KeyboardArrowUp
                } else {
                    Icons.Filled.KeyboardArrowDown
                },
                contentDescription = null
            )
        }

        if (expandido) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = contenido,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun TarjetaResenna(
    resenna: com.example.frutosapp.model.ResennaResponse
) {
    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = androidx.compose.material3.CardDefaults
            .cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = resenna.nombreCliente,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))
            Row {
                for (valor in 1..5) {
                    Text(
                        text = if (valor <= resenna.estrellas) {
                            "★"
                        } else {
                            "☆"
                        },
                        color = if (valor <= resenna.estrellas) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            if (!resenna.comentario.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = resenna.comentario,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = formatearFechaResenna(
                    resenna.fechaActualizacion
                        ?: resenna.fechaCreacion
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
fun formatearFechaResenna(fecha: String?): String {
    if (fecha.isNullOrBlank()) {
        return ""
    }

    return try {
        val fechaSinHora = fecha.substringBefore("T")
        val partes = fechaSinHora.split("-")

        if (partes.size == 3) {
            "${partes[2]}/${partes[1]}/${partes[0]}"
        } else {
            fecha
        }
    } catch (e: Exception) {
        fecha
    }
}