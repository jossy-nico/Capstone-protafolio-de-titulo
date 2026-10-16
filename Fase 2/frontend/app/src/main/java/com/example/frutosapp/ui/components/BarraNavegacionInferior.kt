package com.example.frutosapp.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun BarraNavegacionInferior(
    rutaActual: String,
    onInicioClick: () -> Unit,
    onProductosClick: () -> Unit,
    onCategoriasClick: () -> Unit,
    onCuentaClick: () -> Unit
) {
    NavigationBar {

        // INICIO
        NavigationBarItem(
            selected = rutaActual == "principal",
            onClick = onInicioClick,
            icon = {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = "Inicio"
                )
            },
            label = {
                Text("Inicio")
            }
        )

        // PRODUCTOS
        NavigationBarItem(
            selected = rutaActual == "productos",
            onClick = onProductosClick,
            icon = {
                Icon(
                    imageVector = Icons.Filled.ShoppingCart,
                    contentDescription = "Productos"
                )
            },
            label = {
                Text("Productos")
            }
        )

        // CATEGORÍAS
        NavigationBarItem(
            selected = rutaActual == "categorias",
            onClick = onCategoriasClick,
            icon = {
                Icon(
                    imageVector = Icons.Filled.List,
                    contentDescription = "Categorías"
                )
            },
            label = {
                Text("Categorías")
            }
        )

        // CUENTA
        NavigationBarItem(
            selected = rutaActual == "cuenta",
            onClick = onCuentaClick,
            icon = {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = "Cuenta"
                )
            },
            label = {
                Text("Cuenta")
            }
        )
    }
}