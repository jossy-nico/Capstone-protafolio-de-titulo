package com.example.frutosapp.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.frutosapp.ProductoViewModel
import com.example.frutosapp.UsuarioViewModel
import com.example.frutosapp.ui.PantallaLogin
import com.example.frutosapp.ui.PantallaPrincipal
import com.example.frutosapp.ui.PantallaProducto
import com.example.frutosapp.ui.PantallaUsuario
import com.example.frutosapp.CarritoViewModel
import com.example.frutosapp.InvitadoViewModel
import com.example.frutosapp.LoginViewModel
import com.example.frutosapp.ResennaViewModel
import com.example.frutosapp.SesionViewModel
import com.example.frutosapp.VentaViewModel

import com.example.frutosapp.ui.PantallaCarritoCompra
import com.example.frutosapp.ui.PantallaCategoriasCliente
import com.example.frutosapp.ui.PantallaComprobanteVenta
import com.example.frutosapp.ui.PantallaCuenta
import com.example.frutosapp.ui.PantallaDatosInvitado
import com.example.frutosapp.ui.PantallaDetalleCompra
import com.example.frutosapp.ui.PantallaEditarPerfilCliente
import com.example.frutosapp.ui.PantallaHistorialVentas
import com.example.frutosapp.ui.PantallaPago
import com.example.frutosapp.ui.PantallaQuienesSomos
import com.example.frutosapp.ui.PantallaDetalleProducto
import com.example.frutosapp.ui.PantallaFavoritos
import com.example.frutosapp.ui.PantallaResenna
import com.example.frutosapp.viewmodel.FavoritoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavHost(
    navController: NavHostController,
    sessionVM: SesionViewModel,
    carritoVM: CarritoViewModel = viewModel(),
    productoVM: ProductoViewModel = viewModel(),
    usuarioVM: UsuarioViewModel = viewModel(),
    ventaViewModel: VentaViewModel = viewModel(),
    invitadoViewModel: InvitadoViewModel = viewModel(),
    resennaViewModel: ResennaViewModel = viewModel(),
    favoritoViewModel: FavoritoViewModel = viewModel()

) {
    // Estado de sesión
    val sesionState by sessionVM.sesionState.collectAsStateWithLifecycle()
    val itemsDelCarrito by carritoVM.itemsCarrito.collectAsStateWithLifecycle(initialValue = emptyList())
    val usuarioId = sesionState.userId

    // Asociar carrito al usuario logueado
    LaunchedEffect(usuarioId, sesionState.loginOk) {

        if (sesionState.loginOk && usuarioId != null) {

            // Asociar carrito al usuario logueado
            carritoVM.setUsuarioId(usuarioId)

            // Cargar favoritos del usuario logueado
            favoritoViewModel.cargarFavoritos(usuarioId)

        } else {

            // Usuario invitado
            carritoVM.setUsuarioId(null)

            // Limpiar favoritos al cerrar sesión
            favoritoViewModel.limpiarFavoritos()
        }

        // Limpiar venta creada al cambiar de usuario
        ventaViewModel.limpiarVentaCreada()
    }

    NavHost(
        navController = navController,
        startDestination = "principal"
    ) {
        // PRINCIPAL
        composable("principal") {
            PantallaPrincipal(
                vm = productoVM,
                loginOk = sesionState.loginOk,
                itemsDelCarrito = itemsDelCarrito,
                onLoginClicked = { navController.navigate("login") },
                onRegisterClicked = { navController.navigate("registro") },
                onLogoutClicked = { sessionVM.onLogout() },
                onAddToCartClicked = { producto ->
                    carritoVM.agregarAlCarrito(producto)
                },
                onAumentarCantidad = { item ->
                    carritoVM.aumentarCantidad(item)
                },

                onDisminuirCantidad = { item ->
                    carritoVM.disminuirCantidad(item)
                },
                onNavigateToCarrito = {
                    ventaViewModel.limpiarVentaCreada()
                    navController.navigate("carrito")
                },
                onNavigateToQuienesSomos = { navController.navigate("quienesSomos") },
                onNavigateToHistorial = {
                    if (sesionState.loginOk && usuarioId != null) {
                        navController.navigate("historial")
                    } else {
                        navController.navigate("login")
                    }
                },

                onNavigateToPerfil = {
                    if (sesionState.loginOk && usuarioId != null && usuarioId > 0) {
                        navController.navigate("perfilCliente")
                    } else {
                        navController.navigate("login")
                    }
                },
                onNavigateToDetalleProducto = { producto ->
                    navController.navigate("detalleProducto/${producto.idProductos}")
                },
                onNavigateToInicio = {
                    navController.navigate("principal") {
                        launchSingleTop = true
                    }
                },
                onNavigateToProductos = {
                    navController.navigate("productos") {
                        launchSingleTop = true
                    }
                },
                onNavigateToCategorias = {
                    navController.navigate("categorias") {
                        launchSingleTop = true
                    }
                },
                onNavigateToCuenta = {
                    navController.navigate("cuenta") {
                        launchSingleTop = true
                    }
                },
                usuarioId = usuarioId,
                favoritoViewModel = favoritoViewModel
            )
        }

        // PRODUCTOS
        composable("productos") {
            PantallaProducto(
                vm = productoVM,
                loginOk = sesionState.loginOk,
                itemsDelCarrito = itemsDelCarrito,
                onLoginClicked = { navController.navigate("login") },
                onRegisterClicked = { navController.navigate("registro") },
                onLogoutClicked = { sessionVM.onLogout() },
                onAddToCartClicked = { producto ->
                    carritoVM.agregarAlCarrito(producto)
                },
                onNavigateToCarrito = {
                    ventaViewModel.limpiarVentaCreada()
                    navController.navigate("carrito")
                },

                onNavigateToInicio = {
                    productoVM.onCategoriaSelected(null)
                    productoVM.onBusquedaChange("")

                    navController.navigate("principal") {
                        launchSingleTop = true
                    }
                },

                onNavigateToProductos = {
                    // Ya estamos en Productos
                },

                onNavigateToCategorias = {
                    navController.navigate("categorias") {
                        launchSingleTop = true
                    }
                },

                onNavigateToCuenta = {
                    navController.navigate("cuenta") {
                        launchSingleTop = true
                    }
                }
            )
        }
        // CATEGORÍAS CLIENTE
        composable("categorias") {
            PantallaCategoriasCliente(
                vm = productoVM,

                onCategoriaClick = { categoria ->
                    productoVM.onCategoriaSelected(categoria)

                    navController.navigate("principal") {
                        launchSingleTop = true
                    }
                },

                onNavigateToInicio = {
                    navController.navigate("principal") {
                        launchSingleTop = true
                    }
                },

                onNavigateToProductos = {
                    navController.navigate("productos") {
                        launchSingleTop = true
                    }
                },

                onNavigateToCategorias = {
                    // Ya estamos en Categorías
                },

                onNavigateToCuenta = {
                    navController.navigate("cuenta") {
                        launchSingleTop = true
                    }
                }
            )
        }
        // CUENTA CLIENTE
        composable("cuenta") {

            PantallaCuenta(
                loginOk = sesionState.loginOk,

                onLoginClicked = {
                    navController.navigate("login")
                },

                onRegisterClicked = {
                    navController.navigate("registro")
                },

                onMisComprasClicked = {
                    if (sesionState.loginOk && usuarioId != null) {
                        navController.navigate("historial")
                    } else {
                        navController.navigate("login")
                    }
                },
                onMisFavoritosClicked = {
                    if (
                        sesionState.loginOk &&
                        usuarioId != null &&
                        usuarioId > 0
                    ) {
                        navController.navigate("favoritos")
                    } else {
                        navController.navigate("login")
                    }
                },

                onMisDatosClicked = {
                    if (sesionState.loginOk && usuarioId != null && usuarioId > 0) {
                        navController.navigate("perfilCliente")
                    } else {
                        navController.navigate("login")
                    }
                },

                onQuienesSomosClicked = {
                    navController.navigate("quienesSomos")
                },

                onLogoutClicked = {
                    sessionVM.onLogout()
                },

                onNavigateToInicio = {
                    navController.navigate("principal") {
                        launchSingleTop = true
                    }
                },

                onNavigateToProductos = {
                    navController.navigate("productos") {
                        launchSingleTop = true
                    }
                },

                onNavigateToCategorias = {
                    navController.navigate("categorias") {
                        launchSingleTop = true
                    }
                },

                onNavigateToCuenta = {
                    // Ya estamos en Cuenta
                }
            )
        }

        composable("detalleProducto/{idProducto}") { backStackEntry ->

            val idProducto = backStackEntry.arguments
                ?.getString("idProducto")
                ?.toIntOrNull()

            val productos = productoVM.productos.collectAsStateWithLifecycle().value
            val producto = productos.find { it.idProductos == idProducto }

            if (producto != null) {
                PantallaDetalleProducto(
                    producto = producto,
                    itemsDelCarrito = itemsDelCarrito,
                    loginOk = sesionState.loginOk,
                    onVolver = { navController.popBackStack() },
                    onAddToCartClicked = { carritoVM.agregarAlCarrito(it) },
                    onAumentarCantidad = { carritoVM.aumentarCantidad(it) },
                    onDisminuirCantidad = { carritoVM.disminuirCantidad(it) },
                    onNavigateToCarrito = {
                        ventaViewModel.limpiarVentaCreada()
                        navController.navigate("carrito")
                    },
                    usuarioId = usuarioId,
                    favoritoViewModel = favoritoViewModel
                )
            }
        }
        // FAVORITOS
        composable("favoritos") {

            if (
                sesionState.loginOk &&
                usuarioId != null &&
                usuarioId > 0
            ) {

                PantallaFavoritos(
                    vm = productoVM,
                    favoritoViewModel = favoritoViewModel,
                    usuarioId = usuarioId,
                    itemsDelCarrito = itemsDelCarrito,

                    onVolver = {
                        navController.popBackStack()
                    },

                    onNavigateToDetalleProducto = { producto ->
                        navController.navigate(
                            "detalleProducto/${producto.idProductos}"
                        )
                    },

                    onAddToCartClicked = { producto ->
                        carritoVM.agregarAlCarrito(producto)
                    },

                    onNavigateToCarrito = {
                        ventaViewModel.limpiarVentaCreada()
                        navController.navigate("carrito")
                    },
                    onAumentarCantidad = { item ->
                        carritoVM.aumentarCantidad(item)
                    },

                    onDisminuirCantidad = { item ->
                        carritoVM.disminuirCantidad(item)
                    }
                )

            } else {

                LaunchedEffect(Unit) {
                    navController.navigate("login") {
                        popUpTo("favoritos") {
                            inclusive = true
                        }
                    }
                }
            }
        }

        // CARRITO
        composable("carrito") {
            PantallaCarritoCompra(
                vm = carritoVM,
                usuarioId = usuarioId ?: -1L,
                onVolver = {
                    navController.popBackStack()
                },
                onIrADetalle = {
                    if (usuarioId != null && usuarioId > 0) {
                        navController.navigate("detalleCompra")
                    } else {
                        navController.navigate("datosInvitado")
                    }
                }
            )
        }

        composable("detalleCompra") {
            PantallaDetalleCompra(
                carritoVM = carritoVM,
                ventaViewModel = ventaViewModel,
                invitadoViewModel = invitadoViewModel,
                usuarioId = usuarioId ?: -1L,
                onVolver = {
                    navController.popBackStack()
                },
                onIrAPago = {
                    navController.navigate("pago")
                }
            )
        }

        // PAGO
        composable("pago") {
            PantallaPago(
                ventaViewModel = ventaViewModel,
                carritoVM = carritoVM,
                onVolver = { navController.popBackStack() },
                onPagoExitoso = {
                    navController.navigate("comprobante")
                }
            )
        }

        // HISTORIAL
        composable("historial") {
            PantallaHistorialVentas(
                ventaViewModel = ventaViewModel,
                usuarioId = usuarioId ?: -1L,
                onVerComprobante = { venta ->
                    ventaViewModel.seleccionarVenta(venta)
                    navController.navigate("comprobante")
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // COMPROBANTE
        composable("comprobante") {
            PantallaComprobanteVenta(
                ventaViewModel = ventaViewModel,
                productoViewModel = productoVM,
                onNavigateBack = {
                    navController.navigate("principal") {
                        popUpTo("principal") { inclusive = true }
                    }
                },
                onCalificarProductos = { ventaId, productoId ->
                    navController.navigate(
                        "resenna/$ventaId/$productoId"
                    )
                }
            )
        }

        // LOGIN
        composable("login") {
            PantallaLogin(
                vm = viewModel<LoginViewModel>(),
                onLoginSuccess = { usuario ->
                    // Guardar sesión
                    sessionVM.onLogin(usuario.idUsuario ?: -1L)

                    navController.navigate("principal") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToRegistro = { navController.navigate("registro") },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // REGISTRO
        composable("registro") {
            PantallaUsuario(
                vm = usuarioVM,
                onRegisterSuccess = { navController.popBackStack() },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        //ACTUALIZAR CLIENTE
        composable("perfilCliente") {
            if (sesionState.loginOk && usuarioId != null && usuarioId > 0) {
                PantallaEditarPerfilCliente(
                    vm = usuarioVM,
                    usuarioId = usuarioId,
                    onNavigateBack = { navController.popBackStack() }
                )
            } else {
                LaunchedEffect(Unit) {
                    navController.navigate("login") {
                        popUpTo("principal") { inclusive = false }
                    }
                }
            }
        }

        // QUIÉNES SOMOS
        composable("quienesSomos") {
            PantallaQuienesSomos(
                onNavigateBack = { navController.popBackStack() }
            )
        }


        composable("datosInvitado") {
            PantallaDatosInvitado(
                invitadoViewModel = invitadoViewModel,
                onVolver = {
                    navController.popBackStack()
                },
                onContinuar = {
                    navController.navigate("detalleCompra")
                }
            )
        }

        composable(
            route = "resenna/{ventaId}/{productoId}"
        ) { backStackEntry ->

            val ventaId = backStackEntry.arguments
                ?.getString("ventaId")
                ?.toLongOrNull()

            val productoId = backStackEntry.arguments
                ?.getString("productoId")
                ?.toLongOrNull()

            val clienteId = usuarioId

            if (
                sesionState.loginOk &&
                clienteId != null &&
                clienteId > 0 &&
                ventaId != null &&
                productoId != null
            ) {
                PantallaResenna(
                    resennaViewModel = resennaViewModel,
                    clienteId = clienteId,
                    ventaId = ventaId,
                    productoId = productoId,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            } else {
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            }
        }
    }
}