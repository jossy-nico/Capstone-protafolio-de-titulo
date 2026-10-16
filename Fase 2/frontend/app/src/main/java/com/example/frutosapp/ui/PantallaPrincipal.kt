package com.example.frutosapp.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.frutosapp.ProductoViewModel
import com.example.frutosapp.R
import com.example.frutosapp.model.ItemCarritoConProducto
import com.example.frutosapp.model.Producto
import com.example.frutosapp.ui.components.BarraNavegacionInferior
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.frutosapp.viewmodel.FavoritoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaPrincipal(
    vm: ProductoViewModel = viewModel(),
    loginOk: Boolean,
    itemsDelCarrito: List<ItemCarritoConProducto>,
    onLoginClicked: () -> Unit,
    onRegisterClicked: () -> Unit,
    onLogoutClicked: () -> Unit,
    onAddToCartClicked: (Producto) -> Unit,
    onNavigateToCarrito: () -> Unit,
    onNavigateToQuienesSomos: () -> Unit,
    onNavigateToHistorial: () -> Unit,
    onAumentarCantidad: (ItemCarritoConProducto) -> Unit,
    onDisminuirCantidad: (ItemCarritoConProducto) -> Unit,
    onNavigateToPerfil: () -> Unit,
    onNavigateToDetalleProducto: (Producto) -> Unit,
    onNavigateToInicio: () -> Unit,
    onNavigateToProductos: () -> Unit,
    onNavigateToCategorias: () -> Unit,
    onNavigateToCuenta: () -> Unit,
    usuarioId: Long?,
    favoritoViewModel: FavoritoViewModel
) {

    // Drawer
    val drawerState = rememberDrawerState(
        initialValue = DrawerValue.Closed
    )

    val scope = rememberCoroutineScope()

    // Estados del ViewModel
    val textoBusqueda by vm.textoBusqueda.collectAsStateWithLifecycle()
    val productos by vm.productos.collectAsStateWithLifecycle()
    val productosBase by vm.productosBase.collectAsStateWithLifecycle()
    val categorias by vm.categorias.collectAsStateWithLifecycle()
    val categoriaSeleccionada by vm.categoriaSelect.collectAsStateWithLifecycle()
    val favoritos by favoritoViewModel.favoritos.collectAsStateWithLifecycle()

    // Banners
    val banners = listOf(
        R.drawable.banner_1,
        R.drawable.banner_2,
        R.drawable.banner_3
    )

    var bannerActual by remember {
        mutableIntStateOf(0)
    }

    // Cambio automático de banner cada 4 segundos
    LaunchedEffect(Unit) {
        while (true) {
            delay(4000)
            bannerActual = (bannerActual + 1) % banners.size
        }
    }

    val ctx = LocalContext.current

    val currency = remember {
        java.text.NumberFormat.getCurrencyInstance(
            java.util.Locale("es", "CL")
        )
    }

    val cantidadCarrito = remember(itemsDelCarrito) {
        itemsDelCarrito.sumOf {
            it.carritoCompra.cantidad / 250
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {

            ModalDrawerSheet {

                Spacer(
                    Modifier.height(12.dp)
                )

                Text(
                    text = "Categorías",
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    ),
                    style = MaterialTheme.typography.titleMedium
                )

                HorizontalDivider(
                    modifier = Modifier.padding(
                        bottom = 8.dp
                    )
                )

                NavigationDrawerItem(
                    label = {
                        Text("Todas las categorías")
                    },
                    selected = categoriaSeleccionada == null,
                    onClick = {
                        vm.onCategoriaSelected(null)

                        scope.launch {
                            drawerState.close()
                        }
                    },
                    modifier = Modifier.padding(
                        NavigationDrawerItemDefaults.ItemPadding
                    )
                )

                categorias.forEach { categoria ->

                    NavigationDrawerItem(
                        label = {
                            Text(categoria.nomCategoria)
                        },
                        selected =
                            categoriaSeleccionada?.idCategoria ==
                                    categoria.idCategoria,
                        onClick = {

                            vm.onCategoriaSelected(categoria)

                            scope.launch {
                                drawerState.close()
                            }
                        },
                        modifier = Modifier.padding(
                            NavigationDrawerItemDefaults.ItemPadding
                        )
                    )
                }

                NavigationDrawerItem(
                    label = {
                        Text("Historial de compras")
                    },
                    selected = false,
                    onClick = {

                        onNavigateToHistorial()

                        scope.launch {
                            drawerState.close()
                        }
                    },
                    icon = {
                        Icon(
                            Icons.Filled.ShoppingCart,
                            contentDescription = "Historial"
                        )
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(
                        vertical = 8.dp
                    )
                )

                NavigationDrawerItem(
                    label = {
                        Text("Quiénes Somos")
                    },
                    selected = false,
                    onClick = {

                        onNavigateToQuienesSomos()

                        scope.launch {
                            drawerState.close()
                        }
                    },
                    icon = {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = "Quiénes Somos"
                        )
                    }
                )
            }
        }
    ) {

        var menuFlotanteAbierto by remember {
            mutableStateOf(false)
        }

        Scaffold(

            // =========================
            // BARRA SUPERIOR
            // =========================

            topBar = {

                TopAppBar(

                    title = {

                        BoxWithConstraints(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val anchoLogo = when {
                                maxWidth < 360.dp -> 110.dp
                                maxWidth < 400.dp -> 125.dp
                                else -> 140.dp
                            }

                            Image(
                                painter = painterResource(
                                    id = R.drawable.logo
                                ),
                                contentDescription = "Natur Cosecha",
                                modifier = Modifier
                                    .width(anchoLogo)
                                    .height(48.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    },

                    navigationIcon = {

                        Box {

                            IconButton(
                                onClick = {
                                    menuFlotanteAbierto = true
                                }
                            ) {

                                Icon(
                                    Icons.Filled.Menu,
                                    contentDescription = "Menú"
                                )
                            }

                            DropdownMenu(
                                expanded = menuFlotanteAbierto,
                                onDismissRequest = {
                                    menuFlotanteAbierto = false
                                }
                            ) {

                                if (loginOk) {

                                    DropdownMenuItem(
                                        text = {
                                            Text("Mi Perfil")
                                        },
                                        onClick = {

                                            menuFlotanteAbierto = false
                                            onNavigateToPerfil()
                                        }
                                    )
                                }

                                DropdownMenuItem(
                                    text = {
                                        Text("Historial de compras")
                                    },
                                    onClick = {

                                        menuFlotanteAbierto = false
                                        onNavigateToHistorial()
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text("Quiénes somos")
                                    },
                                    onClick = {

                                        menuFlotanteAbierto = false
                                        onNavigateToQuienesSomos()
                                    }
                                )
                            }
                        }
                    },

                    actions = {

                        BadgedBox(
                            badge = {

                                if (cantidadCarrito > 0) {

                                    Badge {
                                        Text(
                                            cantidadCarrito.toString()
                                        )
                                    }
                                }
                            }
                        ) {

                            IconButton(
                                onClick = onNavigateToCarrito
                            ) {

                                Icon(
                                    Icons.Filled.ShoppingCart,
                                    contentDescription = "Carrito"
                                )
                            }
                        }

                        Spacer(
                            Modifier.width(8.dp)
                        )

                        if (loginOk) {

                            TextButton(
                                onClick = onLogoutClicked
                            ) {
                                Text("Cerrar sesión")
                            }

                        } else {

                            TextButton(
                                onClick = onLoginClicked
                            ) {
                                Text("Iniciar sesión")
                            }

                            TextButton(
                                onClick = onRegisterClicked
                            ) {
                                Text("Registrar")
                            }
                        }
                    }
                )
            },

            // =========================
            // BARRA INFERIOR
            // =========================

            bottomBar = {

                BarraNavegacionInferior(
                    rutaActual = "principal",
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
                    .fillMaxSize()
            ) {

                // =========================
                // BUSCADOR
                // =========================

                OutlinedTextField(
                    value = textoBusqueda,
                    onValueChange = {
                        vm.onBusquedaChange(it)
                    },
                    placeholder = {
                        Text(
                            "Buscar productos, categorías..."
                        )
                    },
                    leadingIcon = {

                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Buscar"
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(22.dp),
                    colors = OutlinedTextFieldDefaults.colors(

                        focusedContainerColor =
                            Color(0xFFF5F6F5),

                        unfocusedContainerColor =
                            Color(0xFFF5F6F5),

                        focusedBorderColor =
                            MaterialTheme.colorScheme.primary,

                        unfocusedBorderColor =
                            Color(0xFFE1E5E3),

                        focusedLeadingIconColor =
                            MaterialTheme.colorScheme.primary,

                        unfocusedLeadingIconColor =
                            Color(0xFF5F696F),

                        focusedPlaceholderColor =
                            Color(0xFF7A8388),

                        unfocusedPlaceholderColor =
                            Color(0xFF7A8388)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            top = 14.dp,
                            bottom = 8.dp
                        )
                )

                // =========================
                // CATEGORÍAS
                // =========================

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = 4.dp
                    ),
                    horizontalArrangement =
                        Arrangement.spacedBy(14.dp)
                ) {

                    // TODOS
                    item {

                        val seleccionada =
                            categoriaSeleccionada == null

                        Column(
                            horizontalAlignment =
                                Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(74.dp)
                                .clickable {
                                    vm.onCategoriaSelected(null)
                                }
                        ) {

                            Card(
                                modifier =
                                    Modifier.size(68.dp),
                                shape = CircleShape,
                                border =
                                    if (seleccionada) {

                                        BorderStroke(
                                            2.dp,
                                            MaterialTheme
                                                .colorScheme
                                                .primary
                                        )

                                    } else {
                                        null
                                    },
                                colors =
                                    CardDefaults.cardColors(
                                        containerColor =
                                            if (seleccionada) {
                                                Color(0xFFE8F5EC)
                                            } else {
                                                Color(0xFFF5F6F5)
                                            }
                                    )
                            ) {

                                Box(
                                    modifier =
                                        Modifier.fillMaxSize(),
                                    contentAlignment =
                                        Alignment.Center
                                ) {

                                    Text(
                                        text = "●  ●\n●  ●",
                                        color =
                                            MaterialTheme
                                                .colorScheme
                                                .primary,
                                        fontWeight =
                                            FontWeight.Bold,
                                        fontSize = 18.sp,
                                        lineHeight = 20.sp
                                    )
                                }
                            }

                            Spacer(
                                modifier =
                                    Modifier.height(6.dp)
                            )

                            Text(
                                text = "Todos",
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodySmall,
                                fontWeight =
                                    if (seleccionada) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Normal
                                    },
                                color =
                                    if (seleccionada) {

                                        MaterialTheme
                                            .colorScheme
                                            .primary

                                    } else {

                                        MaterialTheme
                                            .colorScheme
                                            .onBackground
                                    }
                            )
                        }
                    }

                    // CATEGORÍAS DEL BACKEND
                    items(
                        items = categorias,
                        key = {
                            it.idCategoria
                        }
                    ) { categoria ->

                        val seleccionada =
                            categoriaSeleccionada
                                ?.idCategoria ==
                                    categoria.idCategoria

                        // La imagen se obtiene desde un producto
                        // perteneciente a la categoría.
                        val imagenCategoria =
                            productosBase
                                .firstOrNull { producto ->

                                    producto
                                        .categoria
                                        .idCategoria ==
                                            categoria.idCategoria &&
                                            !producto
                                                .imageUrl
                                                .isNullOrBlank()
                                }
                                ?.imageUrl

                        Column(
                            horizontalAlignment =
                                Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(78.dp)
                                .clickable {

                                    vm.onCategoriaSelected(
                                        categoria
                                    )
                                }
                        ) {

                            Card(
                                modifier =
                                    Modifier.size(68.dp),
                                shape = CircleShape,
                                border =
                                    if (seleccionada) {

                                        BorderStroke(
                                            2.dp,
                                            MaterialTheme
                                                .colorScheme
                                                .primary
                                        )

                                    } else {
                                        null
                                    },
                                colors =
                                    CardDefaults.cardColors(
                                        containerColor =
                                            Color(0xFFF5F6F5)
                                    )
                            ) {

                                AsyncImage(
                                    model =
                                        imagenCategoria,
                                    contentDescription =
                                        categoria.nomCategoria,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale =
                                        ContentScale.Crop,
                                    placeholder =
                                        painterResource(
                                            android.R.drawable
                                                .ic_menu_report_image
                                        ),
                                    error =
                                        painterResource(
                                            android.R.drawable
                                                .ic_menu_report_image
                                        )
                                )
                            }

                            Spacer(
                                modifier =
                                    Modifier.height(6.dp)
                            )

                            Text(
                                text =
                                    categoria.nomCategoria,
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodySmall,
                                fontWeight =
                                    if (seleccionada) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Normal
                                    },
                                color =
                                    if (seleccionada) {

                                        MaterialTheme
                                            .colorScheme
                                            .primary

                                    } else {

                                        MaterialTheme
                                            .colorScheme
                                            .onBackground
                                    },
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(12.dp)
                )

                // =========================
                // CARRUSEL DE BANNERS
                // =========================

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 16.dp
                        ),
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    AnimatedContent(
                        targetState = bannerActual,
                        transitionSpec = {

                            fadeIn(
                                animationSpec =
                                    tween(600)
                            ) togetherWith fadeOut(
                                animationSpec =
                                    tween(600)
                            )
                        },
                        label = "banner"
                    ) { indice ->

                        Image(
                            painter =
                                painterResource(
                                    id = banners[indice]
                                ),
                            contentDescription =
                                "Banner promocional ${indice + 1}",
                            contentScale =
                                ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(145.dp)
                                .clip(
                                    RoundedCornerShape(
                                        16.dp
                                    )
                                )
                        )
                    }

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    // Indicadores del carrusel
                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(6.dp),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        banners.indices.forEach { indice ->

                            Box(
                                modifier = Modifier
                                    .size(
                                        if (
                                            indice ==
                                            bannerActual
                                        ) {
                                            8.dp
                                        } else {
                                            6.dp
                                        }
                                    )
                                    .clip(CircleShape)
                                    .background(
                                        if (
                                            indice ==
                                            bannerActual
                                        ) {
                                            MaterialTheme
                                                .colorScheme
                                                .primary
                                        } else {
                                            Color(
                                                0xFFD1D5D3
                                            )
                                        }
                                    )
                            )
                        }
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(16.dp)
                )

                // =========================
                // PRODUCTOS
                // =========================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 10.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    Text(
                        text = "Productos destacados",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    TextButton(
                        onClick = onNavigateToProductos
                    ) {
                        Text(
                            text = "Ver todos",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (
                    productos.isEmpty() &&
                    textoBusqueda.isBlank() &&
                    categoriaSeleccionada == null
                ) {

                    Box(
                        modifier =
                            Modifier.weight(1f),
                        contentAlignment =
                            Alignment.Center
                    ) {

                        CircularProgressIndicator()
                    }

                } else {

                    AnimatedVisibility(
                        visible =
                            productos.isNotEmpty(),
                        enter =
                            slideInVertically {
                                it / 10
                            } + fadeIn(),
                        exit =
                            slideOutVertically() +
                                    fadeOut(),
                        modifier =
                            Modifier.weight(1f)
                    ) {

                        LazyColumn(
                            modifier =
                                Modifier.fillMaxSize(),
                            contentPadding =
                                PaddingValues(
                                    start = 16.dp,
                                    end = 16.dp,
                                    top = 8.dp,
                                    bottom = 90.dp
                                ),
                            verticalArrangement =
                                Arrangement.spacedBy(
                                    12.dp
                                )
                        ) {

                            items(
                                productos,
                                key = {
                                    it.idProductos ?: 0
                                }
                            ) { producto ->

                                Card(
                                    elevation = CardDefaults.cardElevation(
                                        defaultElevation = 2.dp
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onNavigateToDetalleProducto(producto)
                                        }
                                ) {

                                    val imageUrl = producto.imageUrl?.takeIf {
                                        it.isNotBlank()
                                    }

                                    val stockFormateado =
                                        if (producto.stockGramos >= 1000) {

                                            val kilos = producto.stockGramos / 1000.0

                                            if (producto.stockGramos % 1000 == 0) {
                                                "${kilos.toInt()} kg"
                                            } else {
                                                "${
                                                    String.format(
                                                        java.util.Locale("es", "CL"),
                                                        "%.3f",
                                                        kilos
                                                    )
                                                } kg"
                                            }

                                        } else {
                                            "${producto.stockGramos} g"
                                        }

                                    val itemEnCarrito = itemsDelCarrito.find {
                                        it.producto.idProductos == producto.idProductos
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {

                                        // =========================
                                        // IMAGEN + CORAZÓN
                                        // =========================

                                        Box(
                                            modifier = Modifier
                                                .width(115.dp)
                                                .height(90.dp)
                                        ) {

                                            AsyncImage(
                                                model = imageUrl,
                                                contentDescription = producto.nomProducto,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(
                                                        RoundedCornerShape(10.dp)
                                                    ),
                                                contentScale = ContentScale.Crop,
                                                placeholder = painterResource(
                                                    android.R.drawable.ic_menu_report_image
                                                ),
                                                error = painterResource(
                                                    android.R.drawable.ic_menu_report_image
                                                )
                                            )

                                            val idProducto = producto.idProductos
                                            val esFavorito =
                                                idProducto != null && favoritos.contains(idProducto)

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
                                                        if (!loginOk || usuarioId == null) {

                                                            Toast.makeText(
                                                                ctx,
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
                                                contentAlignment = Alignment.Center
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
                                                        Color(0xFF66BB6A)
                                                    } else {
                                                        Color.White
                                                    },
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }



                                        Spacer(
                                            modifier = Modifier.width(12.dp)
                                        )

                                        // =========================
                                        // INFORMACIÓN
                                        // =========================

                                        Column(
                                            modifier = Modifier.weight(1f)
                                        ) {

                                            Text(
                                                text = producto.nomProducto,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 2
                                            )

                                            Spacer(
                                                modifier = Modifier.height(2.dp)
                                            )

                                            Text(
                                                text = "250g: ${currency.format(producto.precio250g)}",
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )

                                            Text(
                                                text = "Kilo: ${currency.format(producto.precioKilo)}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF5F696F)
                                            )

                                            Text(
                                                text = "Stock: $stockFormateado",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF5F696F)
                                            )
                                        }

                                        Spacer(
                                            modifier = Modifier.width(8.dp)
                                        )

                                        // =========================
                                        // BOTÓN A LA DERECHA
                                        // =========================

                                        if (itemEnCarrito == null) {

                                            Button(
                                                onClick = {
                                                    onAddToCartClicked(producto)

                                                    Toast.makeText(
                                                        ctx,
                                                        "Producto agregado al carrito",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(
                                                    horizontal = 12.dp,
                                                    vertical = 10.dp
                                                )
                                            ) {

                                                Icon(
                                                    imageVector = Icons.Filled.ShoppingCart,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(17.dp)
                                                )

                                                Spacer(
                                                    modifier = Modifier.width(5.dp)
                                                )

                                                Text(
                                                    text = "Agregar",
                                                    maxLines = 1
                                                )
                                            }

                                        } else {

                                            // Conservamos + y - cuando ya está agregado
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
                                                        modifier = Modifier.padding(
                                                            horizontal = 6.dp
                                                        ),
                                                        fontWeight = FontWeight.Bold
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
        }
    }
}